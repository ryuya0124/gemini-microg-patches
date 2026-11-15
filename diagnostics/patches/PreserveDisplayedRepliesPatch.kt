package app.morphe.patches.google.diagnostics
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.google.common.VersionHookRegistry
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
private object RetentionPayload
val preserveDisplayedRepliesPatch=bytecodePatch(
 name="Preserve displayed replies",
 description="Keeps displayed text/images in private encrypted storage across restarts, with optional video saving and manual clear.",
 default=false
){
 compatibleWith("com.google.android.googlequicksearchbox"("17.72.17.ve.arm64"))
 extendWith(java.util.function.Supplier{checkNotNull(RetentionPayload::class.java.getResourceAsStream("/extensions/displayed-replies.bin"))})
 execute{
  VersionHookRegistry.requireProfile(packageMetadata)
  check(packageMetadata.versionCode=="301831138")
  var sites=0
  classDefForEach{owner->
   if(owner.type!="Lcom/google/android/apps/search/assistant/surfaces/voice/robin/main/MainActivity;")return@classDefForEach
   val target=mutableClassDefBy(owner).methods.single{it.name=="onCreate"&&it.parameterTypes==listOf("Landroid/os/Bundle;")}
   val registers=checkNotNull(target.implementation).registerCount
   target.addInstructions(0,"invoke-static/range {p0 .. p0}, Lapp/geminipatch/retention/RetainedReplies;->attach(Landroid/app/Activity;)V")
   check(target.implementation!!.registerCount==registers);sites++
  }
  check(sites==1){"Displayed reply Activity profile changed"}
  var headers=0
  classDefForEach{owner->
   if(owner.type!="Lbprr;")return@classDefForEach
   val target=mutableClassDefBy(owner).methods.single{it.name=="b"&&it.parameterTypes==listOf("Lhiiq;")&&it.returnType=="Ljava/lang/Object;"}
   val registers=checkNotNull(target.implementation).registerCount
   val returns=target.instructions.toList().withIndex().filter{it.value.opcode==Opcode.RETURN_OBJECT}
   // DEX shares one return for the coroutine marker and the completed header map.
   check(returns.size==1){"Generated-video header return profile changed"}
   for((i,ins)in returns.asReversed()){
    val register=(ins as OneRegisterInstruction).registerA
    target.replaceInstruction(i,"invoke-static/range {v$register .. v$register}, Lapp/geminipatch/retention/RetainedReplies;->rememberVideoHeaders(Ljava/lang/Object;)V")
    target.addInstructions(i+1,"return-object v$register")
   }
   check(target.implementation!!.registerCount==registers);headers++
  }
  check(headers==1)
  check(classDefBy("Landroidx/media3/ui/PlayerView;")!!.fields.any{it.name=="f"&&it.type=="Lnfm;"})
  check(classDefBy("Lnfm;")!!.methods.any{it.name=="h"&&it.parameterTypes.isEmpty()&&it.returnType=="Lneu;"})
  check(classDefBy("Lneu;")!!.fields.any{it.name=="c"&&it.type=="Lneo;"})
  check(classDefBy("Lneo;")!!.fields.any{it.name=="i"&&it.type=="Landroid/net/Uri;"})
 }
}
