import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.*
import java.util.zip.ZipFile

/** A shared RETURN must not remain a branch target that bypasses its observer. */
fun main(args:Array<String>) {
    ZipFile(args.single()).use { zip ->
        val classes=zip.entries().asSequence().filter{it.name.matches(Regex("classes[0-9]*\\.dex"))}.flatMap{e->
            zip.getInputStream(e).buffered().use{DexBackedDexFile.fromInputStream(Opcodes.getDefault(),it).classes.toList()}.asSequence()
        }.toList()
        for((owner,name,helper)in listOf(Triple("Latuc;","a","status"),Triple("Latuf;","b","classified"))) {
            val method=classes.single{it.type==owner}.methods.single{it.name==name&&it.parameterTypes==listOf("Ljava/lang/Throwable;")}
            val instructions=method.implementation!!.instructions.toList()
            var offset=0
            val byOffset=instructions.withIndex().associate{(index,ins)->(offset.also{offset+=ins.codeUnits})to(index to ins)}
            val returns=byOffset.filterValues{it.second.opcode==Opcode.RETURN_OBJECT}.keys
            check(returns.isNotEmpty())
            for(ret in returns) {
                val index=byOffset.getValue(ret).first
                val before=instructions[index-1]
                check(before.opcode==Opcode.INVOKE_STATIC_RANGE&&(before as ReferenceInstruction).reference.toString().contains("Lapp/geminipatch/diagnostics/Capture;->$helper("))
            }
            for((code,indexed)in byOffset) {
                val ins=indexed.second
                if(ins !is OffsetInstruction)continue
                val target=code+ins.codeOffset
                check(target !in returns){"$owner.$name branches past diagnostics to RETURN at $target"}
                if(ins.opcode in setOf(Opcode.PACKED_SWITCH,Opcode.SPARSE_SWITCH)) {
                    val payload=byOffset.getValue(target).second as SwitchPayload
                    check(payload.switchElements.none{code+it.offset in returns})
                }
            }
            check(method.implementation!!.tryBlocks.flatMap{it.exceptionHandlers}.none{it.handlerCodeAddress in returns})
            println("All ${returns.size} $owner.$name return paths retain the observer")
        }
        check(classes.none{it.type.contains("ReducedCaps")||it.type.contains("Issue8Trace;")})
        var setterCount=0
        for(c in classes.filterNot{it.type.startsWith("Lapp/geminipatch/diagnostics/")})for(m in c.methods){
            val instructions=m.implementation?.instructions?.toList()?:continue
            for((i,ins)in instructions.withIndex()){
                val ref=(ins as? ReferenceInstruction)?.reference?.toString()?:continue
                if(ins.opcode !in setOf(Opcode.INVOKE_VIRTUAL,Opcode.INVOKE_VIRTUAL_RANGE)||!ref.startsWith("Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;"))continue
                val next=instructions[i+1]
                check(next.opcode==Opcode.INVOKE_STATIC_RANGE&&(next as ReferenceInstruction).reference.toString()=="Lapp/geminipatch/diagnostics/Capture;->textSet(Ljava/lang/CharSequence;)V")
                val text=if(ins is RegisterRangeInstruction)ins.startRegister+1 else (ins as FiveRegisterInstruction).registerD
                check((next as RegisterRangeInstruction).startRegister==text&&next.registerCount==1)
                setterCount++
            }
        }
        check(setterCount==1685){"Native setter sites changed: $setterCount"}
        println("All $setterCount native TextView setters retain their value and the correct text register")
    }
}
