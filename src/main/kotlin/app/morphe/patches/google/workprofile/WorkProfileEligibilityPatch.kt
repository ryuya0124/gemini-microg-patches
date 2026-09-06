package app.morphe.patches.google.workprofile

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/** Enable the existing allow-work-profile branch; preserve other eligibility checks. */
val workProfileEligibilityPatch = bytecodePatch(
    name = "Allow Work Profile Gemini Eligibility",
    description = "Secure FolderでのWORK_PROFILE_NOT_SUPPORTED (19)を、既存のプロファイル許可分岐で回避します。",
    default = true
) {
    compatibleWith("com.google.android.googlequicksearchbox")
    execute {
        // Obfuscated field verified for this version. Fail closed on structural changes.
        val candidates = getAllClassesWithString("Robin eligibility : is disallowed on Work profile")
        var patched = 0
        for (clazz in candidates) {
            check(clazz.type == "Lappk;") { "Unsupported eligibility class: ${clazz.type}" }
            for (method in mutableClassDefBy(clazz).methods) {
                if (method.implementation == null) continue
                val ins = method.instructions.toList()
                val strings = ins.mapNotNull { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string }
                if ("Robin eligibility : is disallowed on Work profile" !in strings) continue
                check("Robin eligibility : Work profile is allowed." in strings)
                check(method.name == "k" && method.returnType == "Ljava/lang/Object;")
                for ((index, instruction) in ins.withIndex()) {
                    val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: continue
                    if (instruction.opcode == Opcode.IGET_BOOLEAN && field.definingClass == clazz.type &&
                        field.name == "x" && field.type == "Z") {
                        val register = (instruction as OneRegisterInstruction).registerA
                        // Same width as iget-boolean, preserving all code offsets.
                        method.replaceInstruction(index, "const/16 v$register, 0x1")
                        patched++
                    }
                }
            }
        }
        check(patched == 1) { "Expected one work-profile allowance read; found $patched" }
        println("[WorkProfileEligibility] Enabled existing work-profile allowance (error 19)")
    }
}
