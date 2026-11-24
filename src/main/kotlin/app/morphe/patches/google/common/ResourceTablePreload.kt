package app.morphe.patches.google.common

import app.morphe.patcher.patch.resourcePatch

/**
 * Patcher 2 decodes resources only when a patch asks for them. If the first request comes after a
 * bytecode patch built the instruction indexes, the large Google resource table is loaded while those
 * indexes are held, which raises peak memory. Bytecode patches depend on this read-only step so the
 * table is loaded first, before the indexes exist. It does not change the manifest.
 */
internal val resourceTablePreloadPatch = resourcePatch {
    execute {
        VersionHookRegistry.requireProfile(packageMetadata)
        document("AndroidManifest.xml").close()
    }
}
