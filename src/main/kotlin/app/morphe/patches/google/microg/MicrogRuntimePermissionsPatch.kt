package app.morphe.patches.google.microg

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

/** Request microG account access when installed outside the ADB helper. */
val microgRuntimePermissionsPatch = bytecodePatch(
    name = "MicroG Account Permissions",
    description = "Requests account access and microG extended access on Gemini startup.",
    default = true
) {
    compatibleWith("com.google.android.googlequicksearchbox")
    dependsOn(googleAppManifestPatch)
    execute {
        val activity = mutableClassDefBy(classDefByOrNull(
            "Lcom/google/android/apps/search/assistant/surfaces/voice/robin/main/MainActivity;"
        ) ?: error("Gemini MainActivity not found"))
        val method = activity.methods.single {
            it.name == "onCreate" && it.parameterTypes == listOf("Landroid/os/Bundle;") && it.returnType == "V"
        }
        val registers = method.implementation?.registerCount ?: error("Missing onCreate body")
        // Three local registers, and p0 must fit an invoke-virtual register operand.
        check(registers in 11..17) { "Unsupported onCreate register layout: $registers" }
        val exits = method.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }
        check(exits.size == 1) { "Expected one onCreate exit, found ${exits.size}" }
        val insertion = method.instructions.indexOfLast { instruction ->
            instruction.opcode == Opcode.IPUT_BOOLEAN &&
                (instruction as? ReferenceInstruction)?.reference.toString() == "${activity.type}->o:Z" &&
                (instruction as TwoRegisterInstruction).registerB == registers - 2
        } + 1
        check(insertion > 0 && insertion < exits.single()) { "Missing onCreate completion flag" }
        method.addInstructionsWithLabels(insertion, """
            const-string v6, "app.revanced.gms.EXTENDED_ACCESS"
            invoke-virtual {p0, v6}, Landroid/content/Context;->checkSelfPermission(Ljava/lang/String;)I
            move-result v6
            if-nez v6, :request_microg_accounts
            const-string v6, "android.permission.GET_ACCOUNTS"
            invoke-virtual {p0, v6}, Landroid/content/Context;->checkSelfPermission(Ljava/lang/String;)I
            move-result v6
            if-eqz v6, :microg_permissions_done
            :request_microg_accounts
            const/4 v6, 0x2
            new-array v6, v6, [Ljava/lang/String;
            const/4 v7, 0x0
            const-string v8, "android.permission.GET_ACCOUNTS"
            aput-object v8, v6, v7
            const/4 v7, 0x1
            const-string v8, "app.revanced.gms.EXTENDED_ACCESS"
            aput-object v8, v6, v7
            const/16 v7, 0x6d47
            invoke-virtual {p0, v6, v7}, Landroid/app/Activity;->requestPermissions([Ljava/lang/String;I)V
            :microg_permissions_done
            nop
        """)
        println("[MicroG Account Permissions] Added startup permission request")
    }
}
