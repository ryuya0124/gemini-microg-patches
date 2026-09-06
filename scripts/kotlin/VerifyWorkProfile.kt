import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import java.util.zip.ZipFile
fun read(path: String): List<Instruction> = ZipFile(path).use { zip ->
    zip.entries().asSequence().filter { it.name.matches(Regex("classes[0-9]*\\.dex")) }.flatMap { entry ->
        zip.getInputStream(entry).buffered().use { DexBackedDexFile.fromInputStream(Opcodes.getDefault(), it) }.classes.asSequence()
    }.single { it.type == "Lappk;" }.methods.single { it.name == "k" }.implementation!!.instructions.toList()
}
fun main(args: Array<String>) {
    val original = read(args[0]); val patched = read(args[1])
    check(original.size == patched.size)
    val targets = original.indices.filter { i ->
        val ref = (original[i] as? ReferenceInstruction)?.reference as? FieldReference
        original[i].opcode == Opcode.IGET_BOOLEAN && ref?.definingClass == "Lappk;" && ref.name == "x" && ref.type == "Z"
    }
    check(targets.size == 1)
    for (i in original.indices) {
        check(original[i].codeUnits == patched[i].codeUnits)
        if (i == targets.single()) {
            check(patched[i].opcode == Opcode.CONST_16)
            check((patched[i] as NarrowLiteralInstruction).narrowLiteral == 1)
            check((original[i] as OneRegisterInstruction).registerA == (patched[i] as OneRegisterInstruction).registerA)
        } else check(original[i].opcode == patched[i].opcode)
        if (original[i] is OffsetInstruction) check((original[i] as OffsetInstruction).codeOffset == (patched[i] as OffsetInstruction).codeOffset)
    }
    println("Eligibility verified: exactly one allowance read replaced with true; instruction sizes and branch offsets preserved")
}
