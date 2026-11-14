import java.util.zip.ZipFile
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.*
fun main(args:Array<String>){
 fun classes(path:String)=ZipFile(path).use{z->z.entries().asSequence().filter{it.name.matches(Regex("classes[0-9]*\\.dex"))}.flatMap{e->z.getInputStream(e).buffered().use{DexBackedDexFile.fromInputStream(Opcodes.getDefault(),it).classes.toList()}.asSequence()}.toList()}
 val original=classes(args[0]);val candidate=classes(args[1])
 check(candidate.any{it.type=="Lapp/geminipatch/compat/HistoryUpdateCompatibility;"})
 check(candidate.none{it.type.contains("HistoryUpdateFixture")||it.type.contains("CapabilitySubsetProbe")||it.type.contains("UpdateCompatibilityProbe")})
 fun method(cs:List<com.android.tools.smali.dexlib2.iface.ClassDef>)=cs.single{it.type=="Latze;"}.methods.single{it.name=="a"&&it.parameterTypes==listOf("Ljava/lang/Object;")&&it.returnType=="V"}
 val before=method(original);val after=method(candidate);check(before.implementation!!.registerCount==after.implementation!!.registerCount)
 val ins=after.implementation!!.instructions.toList();val first=ins[0]
 check(first.opcode==Opcode.INVOKE_STATIC_RANGE&&(first as ReferenceInstruction).reference.toString()=="Lapp/geminipatch/compat/HistoryUpdateCompatibility;->filter(Ljava/lang/Object;)Ljava/lang/Object;")
 val parameter=after.implementation!!.registerCount-1
 check((first as RegisterRangeInstruction).startRegister==parameter&&first.registerCount==1)
 check(ins[1].opcode==Opcode.MOVE_RESULT_OBJECT&&(ins[1]as OneRegisterInstruction).registerA==parameter)
 check(ins.drop(2).map{it.opcode}==before.implementation!!.instructions.map{it.opcode}.toList())
 println("Core-only candidate verified: exact receive parameter, original register/instruction sequence retained, no fixture/subset/probe classes")
}
