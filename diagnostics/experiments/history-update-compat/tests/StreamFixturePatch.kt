package privatestream
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
private object Payload
val streamFixture=bytecodePatch(name="Private one-shot stream warning fixture",default=true){
 compatibleWith("com.google.android.googlequicksearchbox.morphe"("17.72.17.ve.arm64"))
 extendWith(java.util.function.Supplier{checkNotNull(Payload::class.java.classLoader.getResourceAsStream("stream.bin"))})
 execute{
  val owner=checkNotNull(classDefBy("Laopx;"));val method=mutableClassDefBy(owner).methods.single{it.name=="<init>"&&it.parameterTypes==listOf("Lgfeh;")}
  val registers=checkNotNull(method.implementation).registerCount
  method.replaceInstruction(0,"invoke-static/range {p1 .. p1}, Lapp/geminipatch/fixture/StreamUpdateFixture;->receive(Ljava/lang/Object;)Ljava/lang/Object;")
  check(method.implementation!!.registerCount==registers)
 }
}
