package privateretention
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
private object Payload
private val fixtureScreen=resourcePatch{
 execute{document("AndroidManifest.xml").use{doc->
  val app=doc.getElementsByTagName("application").item(0)
  for(name in listOf("FixtureActivity","BlankActivity")){
   val activity=doc.createElement("activity");activity.setAttribute("android:name","app.geminipatch.retentiontest.$name")
   activity.setAttribute("android:exported","false");activity.setAttribute("android:hardwareAccelerated","true");activity.setAttribute("android:theme","@android:style/Theme.Material.Light.NoActionBar");app.appendChild(activity)
   activity.setAttribute("android:process",":search")
  }
 }}
}
val retentionFixture=bytecodePatch(name="Private displayed-reply UI fixture",default=true){
 compatibleWith("com.google.android.googlequicksearchbox.morphe"("17.72.17.ve.arm64"))
 dependsOn(fixtureScreen)
 extendWith(java.util.function.Supplier{checkNotNull(Payload::class.java.classLoader.getResourceAsStream("retention-fixture.bin"))})
 execute{}
}
