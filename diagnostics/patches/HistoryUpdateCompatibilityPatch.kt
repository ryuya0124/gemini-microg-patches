package app.morphe.patches.google.diagnostics
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.google.common.VersionHookRegistry
import java.util.Properties

private object HistoryCompatibilityPayload
private val compatibilityIdentity=resourcePatch {
 dependsOn(conversationDiagnosticsPatch)
 execute {
  val profile=Properties().apply{checkNotNull(HistoryCompatibilityPayload::class.java.getResourceAsStream("/diagnostics/profile.properties")).use{load(it)}}
  document("AndroidManifest.xml").use{doc->
   val app=doc.getElementsByTagName("application").item(0)
   check((0 until app.childNodes.length).none{val n=app.childNodes.item(it);n is org.w3c.dom.Element&&n.tagName=="meta-data"&&n.getAttribute("android:name")=="app.geminipatch.conversation_update_compatibility"})
   val marker=doc.createElement("meta-data");marker.setAttribute("android:name","app.geminipatch.conversation_update_compatibility")
   marker.setAttribute("android:value",checkNotNull(profile.getProperty("build")));app.appendChild(marker)
  }
 }
}
val historyUpdateCompatibilityPatch=bytecodePatch(
 name="Conversation update warning compatibility",
 description="Ignores the recognized conversation update warning while keeping chat history. Experimental.",
 default=true
){
 compatibleWith("com.google.android.googlequicksearchbox"("17.72.17.ve.arm64"))
 dependsOn(conversationDiagnosticsPatch,compatibilityIdentity)
 execute{
  VersionHookRegistry.requireProfile(packageMetadata)
  val profile=Properties().apply{checkNotNull(HistoryCompatibilityPayload::class.java.getResourceAsStream("/diagnostics/profile.properties")).use{load(it)}}
  check(packageMetadata.packageName==profile.getProperty("packageName")&&packageMetadata.versionName==profile.getProperty("versionName")&&packageMetadata.versionCode==profile.getProperty("versionCode"))
  val descriptor=checkNotNull(profile.getProperty("historyUpdateReceiveMethod"))
  fun id(m:com.android.tools.smali.dexlib2.iface.Method)="${m.definingClass}->${m.name}(${m.parameterTypes.joinToString("")})${m.returnType}"
  var sites=0
  classDefForEach{owner->
   if(owner.type!=descriptor.substringBefore("->"))return@classDefForEach
   check(owner.superclass=="Lhggx;")
   val original=owner.methods.single{ id(it)==descriptor }
   val count=checkNotNull(original.implementation).registerCount
   val target=mutableClassDefBy(owner).methods.single{ id(it)==descriptor }
   target.addInstructions(0,"""
    invoke-static/range {p1 .. p1}, Lapp/geminipatch/compat/HistoryUpdateCompatibility;->filter(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object p1
   """.trimIndent())
   check(target.implementation!!.registerCount==count);sites++
  }
  check(sites==1){"History response profile changed: $sites sites"}
  val stream=checkNotNull(profile.getProperty("streamUpdateReceiveMethod"))
  var streamSites=0
  classDefForEach{owner->
   if(owner.type!=stream.substringBefore("->"))return@classDefForEach
   val original=owner.methods.single{id(it)==stream}
   val registers=checkNotNull(original.implementation).registerCount
   val target=mutableClassDefBy(owner).methods.single{id(it)==stream}
   target.addInstructions(0,"""
    invoke-static/range {p1 .. p1}, Lapp/geminipatch/compat/HistoryUpdateCompatibility;->filterStream(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object p1
    check-cast p1, Lgfeh;
   """.trimIndent())
   check(target.implementation!!.registerCount==registers);streamSites++
  }
  check(streamSites==1){"Stream response profile changed: $streamSites sites"}
  println("[HistoryUpdateCompatibility] Exact-profile receive hook applied; original registers retained")
 }
}
