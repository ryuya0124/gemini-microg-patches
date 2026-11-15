import app.morphe.patcher.patch.Patch
import app.morphe.patches.google.common.VersionHookRegistry
import java.lang.reflect.Modifier
import java.util.jar.JarFile
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import java.util.Properties
import java.security.MessageDigest
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

fun main(args:Array<String>) {
    val expected=VersionHookRegistry.profiles.groupBy{it.packageName}.mapValues{(_,v)->v.map{it.versionName}.toSet()}
    var count=0;var diagnostic=false;var compatibility=false;var retention=false
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
                    check(versions==if(patch.name in setOf("Conversation diagnostics","Conversation update warning compatibility","Preserve displayed replies"))setOf("17.72.17.ve.arm64")else expected.getValue(pkg))
                }
                if(patch.name=="Conversation diagnostics")diagnostic=true
                if(patch.name=="Preserve displayed replies"){check(!patch.default);retention=true}
                if(patch.name=="Conversation update warning compatibility"){
                    check(patch.default){"Warning compatibility must be selected by default"}
                    compatibility=true
                }
                count++
            }
        }
        check(diagnostic&&compatibility&&retention&&count==19) { "Expected 19 named patches, got $count" }
        val retentionPayload=checkNotNull(jar.getJarEntry("extensions/displayed-replies.bin"))
        val retentionTypes=jar.getInputStream(retentionPayload).buffered().use{DexBackedDexFile.fromInputStream(Opcodes.getDefault(),it).classes.map{c->c.type}.toSet()}
        check(setOf("Lapp/geminipatch/retention/RetainedReplies;","Lapp/geminipatch/retention/ReplyStore;").all{it in retentionTypes})
        check(retentionTypes.none{it.contains("Fixture")||it.contains("Probe")||it.contains("Test")})
        val payload=checkNotNull(jar.getJarEntry("extensions/conversation-diagnostics.bin"))
        val runtime=jar.getInputStream(payload).buffered().use{DexBackedDexFile.fromInputStream(Opcodes.getDefault(),it).classes.toList()}
        val types=runtime.map{it.type}.toSet()
        check(setOf("Lapp/geminipatch/diagnostics/Capture;","Lapp/geminipatch/diagnostics/CaptureActivity;","Lapp/geminipatch/diagnostics/Schema;").all{it in types})
        check("Lapp/geminipatch/compat/HistoryUpdateCompatibility;" in types)
        check(types.none{it.contains("UpdateFixture")||it.contains("CapabilitySubsetProbe")||it.contains("UpdateCompatibilityProbe")})
        check(jar.getJarEntry("diagnostics/profile.json")!=null)
        val properties=Properties().apply{jar.getInputStream(checkNotNull(jar.getJarEntry("diagnostics/profile.properties"))).use{load(it)}}
        val version=checkNotNull(jar.manifest.mainAttributes.getValue("Version"))
        val schema=runtime.single{it.type=="Lapp/geminipatch/diagnostics/Schema;"}
        val embedded=(schema.fields.single{it.name=="BUILD"}.initialValue as StringEncodedValue).value
        check(embedded==version&&properties.getProperty("build")==version){"Runtime/profile build does not match manifest"}
        val profileHash=MessageDigest.getInstance("SHA-256").digest(jar.getInputStream(jar.getJarEntry("diagnostics/profile.json")).readBytes()).joinToString(""){"%02x".format(it.toInt() and 255)}
        check((schema.fields.single{it.name=="PROFILE_SHA256"}.initialValue as StringEncodedValue).value==profileHash)
        val literalBuilds=runtime.filter{it.type.startsWith("Lapp/geminipatch/diagnostics/Capture")}.flatMap{it.methods.toList()}.map{m->m.implementation?.instructions?.mapNotNull{((it as? ReferenceInstruction)?.reference as? StringReference)?.string}?.filter{it.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(?:-(?:preview|diagnostics)\\.[0-9]+)?"))}?.toSet()?:emptySet()}
        check(literalBuilds.count{version in it}>=2){"Capture/export executable build literals missing"}
        check(literalBuilds.flatten().all{it==version||it=="0.3.5"}){"Stale inlined capture build literal"}
        val helper=runtime.single{it.type=="Lapp/geminipatch/compat/HistoryUpdateCompatibility;"}
        fun strings(method:String)=helper.methods.single{it.name==method}.implementation!!.instructions.mapNotNull{((it as? ReferenceInstruction)?.reference as? StringReference)?.string}.toSet()
        check(version in strings("buildIdentity")&&profileHash in strings("profileIdentity"))
        if(properties.getProperty("streamUpdateReceiveMethod")!=null){
            check(runtime.single{it.type=="Lapp/geminipatch/compat/HistoryUpdateCompatibility;"}.methods.any{it.name=="filterStream"})
        }
        println("Embedded diagnostic build verified: $embedded")
    }
    println("Preview inventory verified: 19 named patches, opt-in reply preservation, exact versions and no test fixtures")
}
