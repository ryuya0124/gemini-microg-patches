import app.morphe.patches.google.common.HookId
import app.morphe.patches.google.common.VersionHookRegistry
import app.morphe.patcher.patch.Patch
import java.io.File
import java.lang.reflect.Modifier
import java.util.jar.JarFile

/** Assert unsupported inputs fail; keep reviewable JSON records in sync with executable profiles. */
fun main(args: Array<String>) {
    require(args.size in 1..2)
    val root = File(args[0])
    val write = args.getOrNull(1) == "--write"
    val declaredInputs = mutableMapOf<String, MutableSet<String>>()
    for (profile in VersionHookRegistry.profiles) {
        check(profile.versionCode.all(Char::isDigit))
        check(profile.validation.isNotEmpty())
        check(VersionHookRegistry.findProfile(profile.packageName, profile.versionName + ".unverified") == null)
        check(runCatching { VersionHookRegistry.requireProfile(profile.packageName, "unknown") }.isFailure)
        check(runCatching {
            VersionHookRegistry.requireProfile(profile.packageName, profile.versionName, "0")
        }.isFailure) { "Mismatched versionCode must fail" }
        for ((file, sha) in profile.inputSha256) {
            check(sha.matches(Regex("[0-9a-f]{64}")))
            declaredInputs.getOrPut(file) { mutableSetOf() }.add(sha)
        }
        for ((id, spec) in profile.hooks) {
            val selection = runCatching { VersionHookRegistry.getTargetSpec(id, profile.packageName, profile.versionName) }
            check(selection.isSuccess == spec.enabled) { "Disabled experiments must fail before editing DEX: $id" }
            if (!spec.enabled) check(selection.exceptionOrNull()!!.message!!.contains("Unsupported historical experiment"))

            spec.expectedMatches?.let { check(it > 0) }
            if (id == HookId.MICROG_RUNTIME_PERMISSIONS) {
                val scratch = listOf("scratchArray", "scratchIndex", "scratchString").map(spec::int)
                check(scratch.distinct().size == 3)
                check(scratch.all { it >= 0 && it < spec.int("minRegisters") - spec.int("thisParameterOffset") })
            }
        }
        val file = File(root, "docs/versions/${profile.packageName}/${profile.versionName}.json")
        val json = encodeJson(mapOf(
            "appName" to profile.appName, "packageName" to profile.packageName,
            "versionName" to profile.versionName, "versionCode" to profile.versionCode,
            "inputSha256" to profile.inputSha256, "validation" to profile.validation,
            "hooks" to profile.hooks.mapKeys { it.key.name }.mapValues { (_, spec) -> mapOf(
                "className" to spec.className, "methodName" to spec.methodName,
                "methodDescriptor" to spec.methodDescriptor, "fieldName" to spec.fieldName,
                "fieldType" to spec.fieldType, "anchorStrings" to spec.anchorStrings,
                "methodSignatures" to spec.methodSignatures, "expectedMatches" to spec.expectedMatches,
                "integers" to spec.integers, "values" to spec.values,
                "enabled" to spec.enabled, "description" to spec.description
            ) }
        )) + "\n"
        if (write) {
            file.parentFile.mkdirs()
            file.writeText(json)
        } else check(file.isFile && file.readText() == json) {
            "Stale version record: $file. Compile patches, then scripts/check-hook-profiles.sh --write"
        }
    }
    val lockedInputs = File(root, "config/inputs.sha256").readLines().filter { it.isNotBlank() }.associate {
        val parts = it.trim().split(Regex("\\s+")); parts[1] to parts[0]
    }
    check(lockedInputs.all { (file, sha) -> sha in declaredInputs[file].orEmpty() })
    val active = VersionHookRegistry.profiles.filter { profile ->
        profile.inputSha256.all { (file, sha) -> lockedInputs[file] == sha }
    }
    check(active.map { it.packageName }.toSet() == VersionHookRegistry.profiles.map { it.packageName }.toSet()) {
        "config/inputs.sha256 must select a complete recorded version for each app"
    }
    check(active.flatMap { it.inputSha256.entries }.associate { it.toPair() } == lockedInputs)

    // Inspect the exported patch objects, not source text: these are the values Morphe reads.
    val expected = VersionHookRegistry.profiles.groupBy { it.packageName }
        .mapValues { (_, profiles) -> profiles.map { it.versionName }.toSet() }
    val exported = linkedMapOf<String, Patch<*>>()
    JarFile(File(root, "build/gemini-patches.jar")).use { jar ->
        jar.entries().asSequence().filter { it.name.endsWith("Kt.class") }.forEach { entry ->
            val type = VersionHookRegistry::class.java.classLoader.loadClass(entry.name.removeSuffix(".class").replace('/', '.'))
            type.declaredMethods.filter {
                Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers) &&
                    it.parameterCount == 0 && Patch::class.java.isAssignableFrom(it.returnType)
            }.forEach { getter ->
                val patch = getter.invoke(null) as Patch<*>
                patch.name?.let { name -> exported[name] = patch }
            }
        }
    }
    check(exported.isNotEmpty()) { "No exported patch metadata was checked" }
    for ((name, patch) in exported) {
        @Suppress("DEPRECATION")
        val packages = checkNotNull(patch.compatiblePackages) { "$name advertises all packages" }
        check(packages.isNotEmpty())
        for ((pkg, versions) in packages) {
            check(!versions.isNullOrEmpty()) { "$name advertises every version of $pkg" }
            check(versions == expected.getValue(pkg)) { "$name has stale supported versions for $pkg" }
        }
    }

    val readme = File(root, "docs/versions/README.md")
    val start = "<!-- supported-versions:start -->"
    val end = "<!-- supported-versions:end -->"
    val text = readme.readText()
    val from = text.indexOf(start); val to = text.indexOf(end)
    check(from >= 0 && to > from) { "Missing generated supported-version table markers" }
    val table = buildString {
        append(start).append("\n| アプリ | 対応versionName | versionCode | フック記録 |\n| --- | --- | --- | --- |\n")
        for (profile in VersionHookRegistry.profiles) {
            append("| ${profile.appName} | `${profile.versionName}` | ${profile.versionCode} | ")
            append("[JSON](${profile.packageName}/${profile.versionName}.json) |\n")
        }
        append(end)
    }
    val updated = text.replaceRange(from, to + end.length, table)
    if (write) readme.writeText(updated) else check(updated == text) {
        "Stale supported-version table: run scripts/check-hook-profiles.sh --write"
    }
    println("Version profiles verified: exact versions/codes, unknown-version rejection, input hashes, JSON records; ${exported.size} Morphe patch declarations have exact version lists")
}

// Small JSON writer for the deliberately limited record types; no runtime dependencies.
private fun quote(value: String): String = buildString {
    append('"')
    for (character in value) when (character) {
        '"' -> append("\\\"")
        '\\' -> append("\\\\")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        else -> if (character.code < 32) append("\\u%04x".format(character.code)) else append(character)
    }
    append('"')
}
private fun encodeJson(value: Any?, depth: Int = 0): String {
    val indent = "  ".repeat(depth)
    val child = "  ".repeat(depth + 1)
    return when (value) {
        null -> "null"
        is String -> quote(value)
        is Int, is Boolean -> value.toString()
        is Map<*, *> -> if (value.isEmpty()) "{}" else value.entries.joinToString(
            ",\n", "{\n", "\n$indent}"
        ) { child + quote(it.key as String) + ": " + encodeJson(it.value, depth + 1) }
        is List<*> -> if (value.isEmpty()) "[]" else value.joinToString(
            ",\n", "[\n", "\n$indent]"
        ) { child + encodeJson(it, depth + 1) }
        else -> error("Unsupported JSON record type: ${value.javaClass}")
    }
}
