package privateupdate
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
private object Payload
val historyUpdatePrototype=bytecodePatch(name="History update warning compatibility candidate",default=true){
 compatibleWith("com.google.android.googlequicksearchbox.morphe"("17.72.17.ve.arm64"))
 extendWith(java.util.function.Supplier{checkNotNull(Payload::class.java.classLoader.getResourceAsStream("update.bin"))})
 execute{
  val owner=checkNotNull(classDefBy("Latze;")){"Exact-version response interceptor not found"}
  val method=owner.methods.single{it.name=="a"&&it.parameterTypes==listOf("Ljava/lang/Object;")&&it.returnType=="V"}
  val registers=checkNotNull(method.implementation).registerCount
  val target=mutableClassDefBy(owner).methods.single{it.name==method.name&&it.parameterTypes==method.parameterTypes&&it.returnType==method.returnType}
  target.addInstructions(0,"""
    invoke-static/range {p1 .. p1}, Lapp/geminipatch/compat/HistoryUpdateCompatibility;->filter(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object p1
  """.trimIndent())
  check(target.implementation!!.registerCount==registers)
  println("History update prototype: one exact-version receive hook, no register growth")
 }
}
