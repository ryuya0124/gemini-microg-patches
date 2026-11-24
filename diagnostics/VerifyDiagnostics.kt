import app.morphe.patcher.patch.Patch
import app.morphe.patches.google.common.VersionHookRegistry
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import java.lang.reflect.Modifier
import java.security.MessageDigest
import java.util.jar.JarFile

/** Checks the experimental bundle: diagnostics on top of the stable patches, opaque build code only. */
fun main(args: Array<String>) {
    val (bundle, code) = args
    val names = mutableListOf<String>()
    JarFile(bundle).use { jar ->
        jar.entries().asSequence().filter { it.name.endsWith("Kt.class") }.forEach { entry ->
            val cls = VersionHookRegistry::class.java.classLoader.loadClass(entry.name.removeSuffix(".class").replace('/', '.'))
            cls.declaredMethods.filter { Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers) && it.parameterCount == 0 && Patch::class.java.isAssignableFrom(it.returnType) }.forEach { getter ->
                val patch = getter.invoke(null) as Patch
                val name = patch.name ?: return@forEach
                if (name == "Conversation diagnostics") {
                    check(patch.default) { "Diagnostics must be selected by default in this build" }
                    val target = checkNotNull(patch.compatibility).single()
                    check(target.packageName == "com.google.android.googlequicksearchbox" && target.targets.map { it.version } == listOf("17.72.17.ve.arm64"))
                }
                names += name
            }
        }
        check("Conversation diagnostics" in names && "Conversation update warning compatibility" in names) { "Missing patches: $names" }
        val payload = checkNotNull(jar.getJarEntry("extensions/conversation-diagnostics.bin"))
        val runtime = jar.getInputStream(payload).buffered().use { DexBackedDexFile.fromInputStream(Opcodes.getDefault(), it).classes.toList() }
        val types = runtime.map { it.type }.toSet()
        check(setOf("Capture", "CaptureActivity", "CaptureInbox", "BuildAudit", "Schema").all { "Lapp/geminipatch/diagnostics/$it;" in types }) { "Runtime incomplete: $types" }
        check(types.all { it.startsWith("Lapp/geminipatch/diagnostics/") }) { "Unexpected runtime classes: $types" }
        val schema = runtime.single { it.type == "Lapp/geminipatch/diagnostics/Schema;" }
        val embedded = (schema.fields.single { it.name == "R" }.initialValue as StringEncodedValue).value
        check(embedded == code) { "Build code $embedded does not match $code" }
        val profileHash = MessageDigest.getInstance("SHA-256").digest(jar.getInputStream(jar.getJarEntry("diagnostics/profile.json")).readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        check((schema.fields.single { it.name == "PROFILE_SHA256" }.initialValue as StringEncodedValue).value == profileHash)
        // Reports and the screen must not show the bundle version in readable form.
        val version = checkNotNull(jar.manifest.mainAttributes.getValue("Version"))
        val strings = runtime.flatMap { c -> c.methods.flatMap { m -> m.implementation?.instructions?.mapNotNull { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string }.orEmpty() } } +
            runtime.flatMap { c -> c.fields.mapNotNull { (it.initialValue as? StringEncodedValue)?.value } }
        check(strings.none { it.contains(version) }) { "Runtime contains the readable version $version" }
    }
    println("Diagnostic bundle verified: ${names.size} patches, build code $code, no readable version in the runtime")
}
