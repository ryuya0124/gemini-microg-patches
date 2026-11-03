package app.morphe.patches.google.microg

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.google.common.HookId
import app.morphe.patches.google.common.VersionHookRegistry
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction20t

/** Opt out of the bundled GMS warning redirect, not device/server certification. */
val deviceComplianceRedirectPatch = bytecodePatch(
    name = "MicroG Device Compliance Redirect",
    description = "microG利用時にGoogleアプリ内の未認定警告へ転送するローカル分岐を抑止します。端末認定は変更しません。",
    default = true
) {
    compatibleWith("com.google.android.googlequicksearchbox")
    execute {
        val spec = VersionHookRegistry.target(HookId.DEVICE_COMPLIANCE_REDIRECT, packageMetadata)
        val clazz = getAllClassesWithString(spec.anchorStrings.single()).single()
        check(clazz.type == spec.className)
        val method = mutableClassDefBy(clazz).methods.single { spec.matches(it) }
        val ins = method.instructions.toList()
        val candidates = ins.indices.filter { i ->
            i > 0 && ins[i].opcode == Opcode.IF_NEZ && ins[i-1].opcode == Opcode.INSTANCE_OF &&
                (ins[i-1] as? ReferenceInstruction)?.reference.toString() ==
                "Lcom/google/android/gms/gmscompliance/ui/UncertifiedDeviceActivity;" &&
                (ins[i] as OneRegisterInstruction).registerA == (ins[i-1] as OneRegisterInstruction).registerA
        }
        check(candidates.size == spec.expectedMatches)
        val index = candidates.single()
        val offset = (ins[index] as OffsetInstruction).codeOffset
        val addresses = mutableListOf<Int>(); var address = 0
        ins.forEach { addresses.add(address); address += it.codeUnits }
        val targetIndex = addresses.indexOf(addresses[index] + offset)
        check(targetIndex > index)
        check(ins[targetIndex].opcode == Opcode.SGET_OBJECT && ins[targetIndex+1].opcode == Opcode.RETURN_OBJECT)
        val skipped = ins.subList(index+1, targetIndex)
        check(skipped.any { (it as? ReferenceInstruction)?.reference.toString() == "Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V" })
        check(skipped.any { (it as? ReferenceInstruction)?.reference.toString() == "Landroid/app/Activity;->finish()V" })
        // Both instructions occupy two code units: no relocation of branches/try ranges.
        method.replaceInstruction(index, BuilderInstruction20t(Opcode.GOTO_16, method.implementation!!.newLabelForIndex(targetIndex)))
        println("[DeviceCompliance] Skipped local warning launch; certification state unchanged")
    }
}
