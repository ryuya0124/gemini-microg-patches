import app.morphe.patcher.patch.Patch
import app.morphe.patches.google.common.VersionHookRegistry
import java.lang.reflect.Modifier
import java.util.jar.JarFile
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile

fun main(args:Array<String>) {
    val expected=VersionHookRegistry.profiles.groupBy{it.packageName}.mapValues{(_,v)->v.map{it.versionName}.toSet()}
    var count=0;var diagnostic=false;var compatibility=false
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
                    check(versions==if(patch.name in setOf("Conversation diagnostics","Conversation update warning compatibility"))setOf("17.72.17.ve.arm64")else expected.getValue(pkg))
                }
                if(patch.name=="Conversation diagnostics")diagnostic=true
                if(patch.name=="Conversation update warning compatibility"){
                    check(patch.default){"Warning compatibility must be selected by default"}
                    compatibility=true
                }
                count++
            }
        }
        check(diagnostic&&compatibility&&count==18) { "Expected the 16 base patches plus diagnostics and compatibility, got $count" }
        val payload=checkNotNull(jar.getJarEntry("extensions/conversation-diagnostics.bin"))
        val types=jar.getInputStream(payload).buffered().use{DexBackedDexFile.fromInputStream(Opcodes.getDefault(),it).classes.map{c->c.type}.toSet()}
        check(setOf("Lapp/geminipatch/diagnostics/Capture;","Lapp/geminipatch/diagnostics/CaptureActivity;","Lapp/geminipatch/diagnostics/Schema;").all{it in types})
        check("Lapp/geminipatch/compat/HistoryUpdateCompatibility;" in types)
        check(types.none{it.contains("HistoryUpdateFixture")||it.contains("CapabilitySubsetProbe")||it.contains("UpdateCompatibilityProbe")})
        check(jar.getJarEntry("diagnostics/profile.json")!=null)
    }
    println("Preview inventory verified: 18 named patches, exact versions, core compatibility and no test fixtures")
}
