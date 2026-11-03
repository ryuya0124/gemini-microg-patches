import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.*
import java.util.zip.ZipFile

// Also supports a previously patched installed APK as the original side.
fun main(args: Array<String>) {
    require(args.size == 3) { "ORIGINAL_APK PATCHED_APK CLASS_DESCRIPTOR" }
    fun read(path: String) = ZipFile(path).use { z ->
        z.entries().asSequence().filter { it.name.matches(Regex("classes[0-9]*\\.dex")) }.flatMap { e ->
            z.getInputStream(e).buffered().use { DexBackedDexFile.fromInputStream(Opcodes.getDefault(), it) }.classes.asSequence()
        }.single { it.type == args[2] }.methods.single { it.name == "invoke" }.implementation!!.instructions.toList()
    }
    val original = read(args[0]); val patched = read(args[1])
    check(original.size == patched.size)
    var changes = 0
    for (i in original.indices) {
        val a = original[i]; val b = patched[i]
        check(a.codeUnits == b.codeUnits)
        if (a.opcode != b.opcode) {
            check(a.opcode == Opcode.IF_NEZ && b.opcode == Opcode.GOTO_16)
            check(i > 0 && original[i-1].opcode == Opcode.INSTANCE_OF)
            check((original[i-1] as ReferenceInstruction).reference.toString() == "Lcom/google/android/gms/gmscompliance/ui/UncertifiedDeviceActivity;")
            changes++
        }
        if (a is OffsetInstruction) check(a.codeOffset == (b as OffsetInstruction).codeOffset)
        if (a is ReferenceInstruction) check(a.reference.toString() == (b as ReferenceInstruction).reference.toString())
        if (a is OneRegisterInstruction && b is OneRegisterInstruction) check(a.registerA == b.registerA)
        if (a is TwoRegisterInstruction && b is TwoRegisterInstruction) check(a.registerB == b.registerB)
        if (a is WideLiteralInstruction) check(a.wideLiteral == (b as WideLiteralInstruction).wideLiteral)
    }
    check(changes == 1)
    println("Compliance redirect verified: exactly one branch changed; sizes, offsets and references preserved")
}
