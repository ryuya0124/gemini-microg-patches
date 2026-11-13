import app.morphe.patcher.patch.Patch
import app.morphe.patches.google.common.VersionHookRegistry
import java.lang.reflect.Modifier
import java.util.jar.JarFile
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile

fun main(args:Array<String>) {
    val expected=VersionHookRegistry.profiles.groupBy{it.packageName}.mapValues{(_,v)->v.map{it.versionName}.toSet()}
    var count=0;var diagnostic=false
    JarFile(args[0]).use { jar ->
        jar.entries().asSequence().filter{it.name.endsWith("Kt.class")}.forEach { entry ->
            val cls=VersionHookRegistry::class.java.classLoader.loadClass(entry.name.removeSuffix(".class").replace('/','.'))
            cls.declaredMethods.filter{Modifier.isStatic(it.modifiers)&&Modifier.isPublic(it.modifiers)&&it.parameterCount==0&&Patch::class.java.isAssignableFrom(it.returnType)}.forEach { getter ->
                val patch=getter.invoke(null) as Patch<*>
                if(patch.name==null)return@forEach
                @Suppress("DEPRECATION")
                val packages=checkNotNull(patch.compatiblePackages)
                check(packages.isNotEmpty())
                for((pkg,versions)in packages) {
                    check(!versions.isNullOrEmpty()) { "${patch.name} claims all versions" }
                    check(versions==if(patch.name=="Conversation diagnostics")setOf("17.72.17.ve.arm64")else expected.getValue(pkg))
                }
                if(patch.name=="Conversation diagnostics")diagnostic=true
                count++
            }
        }
        check(diagnostic&&count==17) { "Expected the 16 base patches plus diagnostics, got $count" }
        val payload=checkNotNull(jar.getJarEntry("extensions/conversation-diagnostics.bin"))
        val types=jar.getInputStream(payload).buffered().use{DexBackedDexFile.fromInputStream(Opcodes.getDefault(),it).classes.map{c->c.type}.toSet()}
        check(setOf("Lapp/geminipatch/diagnostics/Capture;","Lapp/geminipatch/diagnostics/CaptureActivity;","Lapp/geminipatch/diagnostics/Schema;").all{it in types})
        check(jar.getJarEntry("diagnostics/profile.json")!=null)
    }
    println("Diagnostics inventory verified: 17 named patches, exact versions and both runtime entry points")
}
