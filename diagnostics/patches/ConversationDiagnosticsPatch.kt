package app.morphe.patches.google.diagnostics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.google.common.VersionHookRegistry
import app.morphe.patches.google.common.automaticCloneSetupPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.Method
import java.util.Properties

private object Payload
// Manager's external JVM host does not expose the desktop JSON implementation.
// package.py derives this resource from the owning JSON profile during the build.
private val profile by lazy {
    Properties().apply { checkNotNull(Payload::class.java.getResourceAsStream("/diagnostics/profile.properties")).use { load(it) } }
}
private fun value(name: String) = checkNotNull(profile.getProperty(name)) { "Missing diagnostic profile key $name" }
private fun section(name: String) = profile.stringPropertyNames().filter { it.startsWith("$name.") }
    .associate { it.removePrefix("$name.") to value(it) }
private const val HELPER = "Lapp/geminipatch/diagnostics/Capture;"
private fun id(m: Method) = "${m.definingClass}->${m.name}(${m.parameterTypes.joinToString("")})${m.returnType}"

private val diagnosticsScreen = resourcePatch {
    dependsOn(automaticCloneSetupPatch)
    execute {
        VersionHookRegistry.requireProfile(packageMetadata)
        check(packageMetadata.packageName == value("packageName") && packageMetadata.versionName == value("versionName") && packageMetadata.versionCode == value("versionCode")) {
            "Diagnostics support only the exact original Google version recorded in the diagnostic profile"
        }
        document("AndroidManifest.xml").use { doc ->
            val app = doc.getElementsByTagName("application").item(0)
            check((app as org.w3c.dom.Element).getAttribute("android:name") == value("application")) { "Application initialization profile changed" }
            val activity = doc.createElement("activity")
            activity.setAttribute("android:name", "app.geminipatch.diagnostics.CaptureActivity")
            activity.setAttribute("android:exported", "true")
            activity.setAttribute("android:label", "Gemini diagnostics")
            activity.setAttribute("android:icon", "@android:drawable/ic_menu_info_details")
            activity.setAttribute("android:theme", "@android:style/Theme.Material.Light.NoActionBar")
            val filter = doc.createElement("intent-filter")
            val action = doc.createElement("action"); action.setAttribute("android:name", "android.intent.action.MAIN")
            val category = doc.createElement("category"); category.setAttribute("android:name", "android.intent.category.LAUNCHER")
            filter.appendChild(action); filter.appendChild(category); activity.appendChild(filter); app.appendChild(activity)
        }
    }
    finalize {
        document("AndroidManifest.xml").use { doc ->
            val queries = doc.getElementsByTagName("queries").item(0) ?: doc.createElement("queries").also { doc.documentElement.appendChild(it) }
            val existing = doc.getElementsByTagName("package").let { nodes -> (0 until nodes.length).map { (nodes.item(it) as org.w3c.dom.Element).getAttribute("android:name") }.toSet() }
            for (name in listOf("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.morphe", "com.google.android.apps.bard", "com.google.android.apps.bard.morphe")) {
                if (name !in existing) queries.appendChild(doc.createElement("package").also { it.setAttribute("android:name", name) })
            }
        }
    }
}

// This source is compiled only by build-diagnostics.sh, never the stable bundle build.
val conversationDiagnosticsPatch = bytecodePatch(
    name = "Conversation diagnostics",
    description = "Adds an opt-in, local-only diagnostics screen for conversation update warnings. No chat text or accounts are saved.",
    default = true
) {
    compatibleWith("com.google.android.googlequicksearchbox"("17.72.17.ve.arm64"))
    dependsOn(diagnosticsScreen)
    extendWith(java.util.function.Supplier { checkNotNull(Payload::class.java.getResourceAsStream("/extensions/conversation-diagnostics.bin")) })
    execute {
        val methods = section("methods")
        val versions = (0 until value("versionFields.count").toInt()).associate { value("versionFields.$it.field") to value("versionFields.$it.observer") }
        val caps = (0 until value("capabilityFields.count").toInt()).map { value("capabilityFields.$it") }.toSet()
        val roots = section("incomingTypes").keys.map { "L$it;" }.toSet()
        val counts = mutableMapOf<String, Int>()
        val rootCounts = mutableMapOf<String, Int>()
        val activity = value("activity")
        classDefForEach { cls ->
            val targets = cls.methods.filter { m -> m.implementation != null && (
                id(m) in methods.values || cls.type == activity && m.name == "onCreate" && m.parameterTypes == listOf("Landroid/os/Bundle;") ||
                    m.implementation!!.instructions.any { ins ->
                        val reference = (ins as? ReferenceInstruction)?.reference.toString()
                        ins.opcode == Opcode.CHECK_CAST && reference in roots || ins.opcode == Opcode.IPUT_OBJECT && cls.type != "Lggjj;" && (reference in versions || reference in caps)
                    }) }
            if (targets.isEmpty()) return@classDefForEach
            val mutable = mutableClassDefBy(cls)
            for (target in targets) {
                val m = mutable.methods.single { id(it) == id(target) }
                val registers = m.implementation!!.registerCount
                data class Edit(val index: Int, val code: String, val returnRegister: String? = null)
                val edits = mutableListOf<Edit>()
                fun observe(index: Int, register: String, method: String, type: String = "Ljava/lang/Object;", replaceReturn: Boolean = false) {
                    edits.add(Edit(index, "invoke-static/range {$register .. $register}, $HELPER->$method($type)V", if (replaceReturn) register else null))
                    counts[method] = (counts[method] ?: 0) + 1
                }
                if (cls.type == activity && m.name == "onCreate") observe(0, "p0", "attach", "Landroid/app/Activity;")
                if (id(m) == methods["init"]) observe(0, "p0", "init", "Landroid/content/Context;")
                if (id(m) == methods["stream"]) observe(0, "p1", "stream")
                if (id(m) == methods["readOnly"]) observe(0, "p1", "readOnly")
                for ((i, ins) in m.instructions.toList().withIndex()) {
                    val reference = (ins as? ReferenceInstruction)?.reference.toString()
                    if (ins.opcode == Opcode.CHECK_CAST && reference in roots) {
                        observe(i + 1, "v${(ins as OneRegisterInstruction).registerA}", "incoming")
                        rootCounts[reference] = (rootCounts[reference] ?: 0) + 1
                    }
                    if (ins.opcode == Opcode.RETURN_OBJECT && id(m) == methods["status"]) observe(i, "v${(ins as OneRegisterInstruction).registerA}", "status", replaceReturn = true)
                    if (ins.opcode == Opcode.RETURN_OBJECT && id(m) == methods["classified"]) observe(i, "v${(ins as OneRegisterInstruction).registerA}", "classified", "Ljava/lang/String;", replaceReturn = true)
                    if (ins.opcode != Opcode.IPUT_OBJECT || cls.type == "Lggjj;") continue
                    val register = "v${(ins as TwoRegisterInstruction).registerA}"
                    if (reference in versions) observe(i + 1, register, versions.getValue(reference), "Ljava/lang/String;")
                    if (reference in caps) observe(i + 1, register, "capabilities")
                }
                for (edit in edits.sortedByDescending { it.index }) {
                    if (edit.returnRegister == null) m.addInstructions(edit.index, edit.code)
                    else {
                        // Retain the old RETURN location for existing branch labels. Inserting
                        // before RETURN alone lets branches jump past the observer.
                        m.replaceInstruction(edit.index, edit.code)
                        m.addInstructions(edit.index + 1, "return-object ${edit.returnRegister}")
                    }
                }
                check(m.implementation!!.registerCount == registers)
            }
        }
        for ((name, expected) in section("expectedSites")) check(counts[name] == expected.toInt()) { "Diagnostic hook count changed: $name ${counts[name]} vs $expected" }
        check(rootCounts.keys == roots) { "Diagnostic response schema disappeared: ${roots - rootCounts.keys}" }
        println("[ConversationDiagnostics] Hook sites=$counts; schema casts=$rootCounts; original register counts retained")
    }
}
