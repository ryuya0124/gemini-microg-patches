import java.util.zip.ZipFile
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.*
fun main(args:Array<String>){
 fun classes(path:String)=ZipFile(path).use{z->z.entries().asSequence().filter{it.name.matches(Regex("classes[0-9]*\\.dex"))}.flatMap{e->z.getInputStream(e).buffered().use{DexBackedDexFile.fromInputStream(Opcodes.getDefault(),it).classes.toList()}.asSequence()}.toList()}
 val original=classes(args[0]);val selected=classes(args[1]);val default=classes(args[2])
 val prefix="Lapp/geminipatch/retention/"
 check(default.none{it.type.startsWith(prefix)}){"Unselected patch included local archive runtime"}
 check(selected.any{it.type==prefix+"RetainedReplies;"})
 check(selected.none{it.type.contains("retentiontest")})
 fun header(cs:List<ClassDef>)=cs.single{it.type=="Lbprr;"}.methods.single{it.name=="b"&&it.parameterTypes==listOf("Lhiiq;")}
 val before=header(original).implementation!!;val after=header(selected).implementation!!
 check(before.registerCount==after.registerCount)
 val old=before.instructions.toList();val ins=after.instructions.toList()
 fun observer(i:com.android.tools.smali.dexlib2.iface.instruction.Instruction)=i.opcode==Opcode.INVOKE_STATIC_RANGE&&(i as ReferenceInstruction).reference.toString()==prefix+"RetainedReplies;->rememberVideoHeaders(Ljava/lang/Object;)V"
 val hook=ins.withIndex().single{observer(it.value)}
 check(ins[hook.index+1].opcode==Opcode.RETURN_OBJECT)
 check((hook.value as RegisterRangeInstruction).registerCount==1)
 check((hook.value as RegisterRangeInstruction).startRegister==(ins[hook.index+1] as OneRegisterInstruction).registerA)
 val retained=ins.filterNot{observer(it)};check(retained.map{it.opcode}==old.map{it.opcode})
 // Every original branch still reaches its original instruction; return targets now enter the observer.
 var address=0;val oldAddresses=old.map{val a=address;address+=it.codeUnits;a}
 address=0;val newAddresses=ins.map{val a=address;address+=it.codeUnits;a}
 val oldToNew=old.indices.associateWith{i->if(i<hook.index)i else i+1}
 for(i in old.indices)if(old[i] is OffsetInstruction){
  val oldTarget=oldAddresses.indexOf(oldAddresses[i]+(old[i] as OffsetInstruction).codeOffset)
  val next=oldToNew.getValue(i);val actual=newAddresses.indexOf(newAddresses[next]+(ins[next] as OffsetInstruction).codeOffset)
  val expected=if(oldTarget==hook.index)hook.index else oldToNew.getValue(oldTarget)
  check(actual==expected){"Header observer changed a native branch target"}
 }
 fun entry(cs:List<ClassDef>)=cs.single{it.type=="Lcom/google/android/apps/search/assistant/surfaces/voice/robin/main/MainActivity;"}.methods.single{it.name=="onCreate"&&it.parameterTypes==listOf("Landroid/os/Bundle;")}.implementation!!
 val activity=entry(selected);check(activity.registerCount==entry(original).registerCount)
 val attach=activity.instructions.first();check(attach.opcode==Opcode.INVOKE_STATIC_RANGE)
 check((attach as ReferenceInstruction).reference.toString()==prefix+"RetainedReplies;->attach(Landroid/app/Activity;)V")
 check((attach as RegisterRangeInstruction).startRegister==activity.registerCount-2&&attach.registerCount==1)
 println("Retention DEX verified: opt-in payload isolation, exact Activity parameter, unchanged header registers/returns/branch targets, no fixtures")
}
