package app.morphe.patches.google.common

/** Provider identities must agree in the manifest, URI builders and provider validation. */
object CloneProviderNames {
    fun renamed(authority: String, originalPackage: String): String {
        require(authority.isNotBlank() && !authority.startsWith('@') && !authority.startsWith('.'))
        val clone = "$originalPackage.morphe"
        return if (authority == originalPackage || authority.startsWith("$originalPackage."))
            clone + authority.removePrefix(originalPackage)
        else "${clone}_$authority"
    }

    fun replacement(value: String, authorities: Map<String, String>, originalPackage: String): String? {
        // A raw package string also names process/identity constants. Package-specific
        // patches own those; a content URI unambiguously names its provider.
        if (value != originalPackage) authorities[value]?.let { return it }
        if (!value.startsWith("content://")) return null
        val end = value.indexOfAny(charArrayOf('/', '?', '#'), "content://".length)
            .let { if (it < 0) value.length else it }
        val authority = value.substring("content://".length, end)
        return authorities[authority]?.let { "content://$it" + value.substring(end) }
    }
}
