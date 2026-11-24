package app.morphe.patches.google.conversation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.google.common.HookId
import app.morphe.patches.google.common.VersionHookRegistry

/** Keep conversations usable when Gemini answers with the recognized "Conversation requires an update" warning. */
val conversationUpdateCompatibilityPatch = bytecodePatch(
    name = "Conversation update warning compatibility",
    description = "Ignores the \"Conversation requires an update\" warning so existing chats keep working.",
    default = true
) {
    compatibleWith(*VersionHookRegistry.compatibility("com.google.android.googlequicksearchbox"))
    extendWith(java.util.function.Supplier {
        checkNotNull(VersionHookRegistry::class.java.classLoader.getResourceAsStream("extensions/conversation-update.bin")) {
            "Missing conversation update runtime extension"
        }
    })
    execute {
        val spec = VersionHookRegistry.requireProfile(packageMetadata).hooks[HookId.CONVERSATION_UPDATE_WARNING]
        if (spec == null) {
            VersionHookRegistry.logHook(HookId.CONVERSATION_UPDATE_WARNING, "profile", "not needed for ${packageMetadata.versionName}")
            return@execute
        }
        val runtime = "Lapp/geminipatch/extension/ConversationUpdateCompatibility;"

        val history = classDefBy(requireNotNull(spec.className))
        check(history.superclass == spec.value("superclass")) { "History receiver changed: ${history.superclass}" }
        val historyRegisters = history.methods.single { spec.matches(it) }.implementation!!.registerCount
        val historyMethod = mutableClassDefBy(history).methods.single { spec.matches(it) }
        historyMethod.addInstructions(0, """
            invoke-static/range {p1 .. p1}, $runtime->filter(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object p1
        """.trimIndent())
        check(historyMethod.implementation!!.registerCount == historyRegisters)

        val streamClass = classDefBy(spec.value("streamClass"))
        fun isStream(m: com.android.tools.smali.dexlib2.iface.Method) = m.name == spec.value("streamMethod") &&
            "(${m.parameterTypes.joinToString("")})${m.returnType}" == spec.value("streamDescriptor")
        check(streamClass.methods.count(::isStream) == spec.expectedMatches) { "Reply receiver changed" }
        val streamRegisters = streamClass.methods.single(::isStream).implementation!!.registerCount
        val streamMethod = mutableClassDefBy(streamClass).methods.single(::isStream)
        streamMethod.addInstructions(0, """
            invoke-static/range {p1 .. p1}, $runtime->filterStream(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object p1
            check-cast p1, ${spec.value("streamResponse")}
        """.trimIndent())
        check(streamMethod.implementation!!.registerCount == streamRegisters)
        // Turns carry their own warning, which history and replies hand to this mapping.
        val turn = spec.value("turnMethod")
        val turnClass = classDefBy(turn.substringBefore("->"))
        fun id(m: com.android.tools.smali.dexlib2.iface.Method) = "${m.definingClass}->${m.name}(${m.parameterTypes.joinToString("")})${m.returnType}"
        val turnRegisters = turnClass.methods.single { id(it) == turn }.implementation!!.registerCount
        val turnMethod = mutableClassDefBy(turnClass).methods.single { id(it) == turn }
        turnMethod.addInstructions(0, """
            invoke-static/range {p1 .. p1}, $runtime->filterTurn(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object p1
            check-cast p1, Lgfnl;
        """.trimIndent())
        check(turnMethod.implementation!!.registerCount == turnRegisters)

        // The newer chat history sends a message update after each reply; filter it where it is cast.
        var updates = 0
        for (site in spec.value("updateCasts").split('|')) {
            val owner = classDefBy(site.substringBefore("->"))
            val registers = owner.methods.single { id(it) == site }.implementation!!.registerCount
            val method = mutableClassDefBy(owner).methods.single { id(it) == site }
            val casts = method.instructions.withIndex().filter { (_, ins) ->
                ins.opcode == com.android.tools.smali.dexlib2.Opcode.CHECK_CAST &&
                    (ins as com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction).reference.toString() == "Lgicm;"
            }
            check(casts.size == 1) { "Unexpected message update casts in $site: ${casts.size}" }
            val (index, cast) = casts.single()
            val register = (cast as com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction).registerA
            method.addInstructions(index + 1, """
                invoke-static/range {v$register .. v$register}, $runtime->filterUpdate(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$register
                check-cast v$register, Lgicm;
            """.trimIndent())
            check(method.implementation!!.registerCount == registers)
            updates++
        }
        check(updates == 2)
        VersionHookRegistry.logHook(HookId.CONVERSATION_UPDATE_WARNING, "profile", "history, reply, turn and $updates history-update sites hooked")
    }
}
