/*
 * Apple Music artwork for YouTube Music (Morphe patch bundle).
 * License: AGPL-3.0-or-later. Motion-artwork lookup logic is a Java re-implementation of
 * ViviMusic Xten's apple-canvas.js (AGPL-3.0). Fingerprints copied from Morphe patches (GPLv3).
 */
package dev.archimetrix.patches.applemusic

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceXmlOrNull
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXT = "Ldev/archimetrix/applemusic/AppleMusicArtwork;"
private const val UI = "Ldev/archimetrix/applemusic/SettingsUi;"

private val COMPAT = Compatibility(
    name = "YouTube Music",
    packageName = "com.google.android.apps.youtube.music",
    apkFileType = ApkFileType.APK_REQUIRED,
    appIconColor = 0xFF0000,
    signatures = setOf(
        "6a2f65ec694a6a632acdcb5080912a565f903d4b8d83f0eb8e44fbdf2660d8e1",
        "a2a1ad7ba7f41dfca4514e2afeb90691719af6d0fdbed4b09bbf0ed897701ceb"
    ),
    targets = listOf(
        AppTarget(version = "9.46.51", minSdk = 28, isExperimental = true),
        AppTarget(version = "9.45.51", minSdk = 28, isExperimental = true),
        AppTarget(version = "9.44.52", minSdk = 26, isExperimental = true),
        AppTarget(version = "9.30.53", minSdk = 26),
        AppTarget(version = "9.20.53", minSdk = 26),
        AppTarget(version = "9.15.51", minSdk = 26),
    )
)

internal object AmActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/music/activities/MusicActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;")
)

internal object AmStartActivityFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/music/settings/SettingsCompatActivity;",
    name = "startActivity",
    returnType = "V",
    parameters = listOf("Landroid/content/Intent;")
)

internal object AmSetMetadataFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Landroid/media/MediaMetadata;"),
        methodCall(
            definingClass = "Landroid/media/session/MediaSession;",
            name = "setMetadata",
            parameters = listOf("Landroid/media/MediaMetadata;")
        )
    )
)

internal object AmSetPlaybackStateFingerprint : Fingerprint(
    filters = listOf(
        methodCall(smali = "Landroid/os/RemoteCallbackList;->beginBroadcast()I"),
        methodCall(
            definingClass = "Landroid/media/session/MediaSession;",
            name = "setPlaybackState",
            parameters = listOf("Landroid/media/session/PlaybackState;")
        )
    )
)

@Suppress("unused")
val appleMusicArtworkPatch = patch(
    name = "Apple Music artwork",
    description = "Plays Apple Music animated (HLS) album artwork over the player thumbnail, fetched directly from Apple.",
    default = true
) {
    compatibleWith(COMPAT)
    extendWith("extensions/apple-music-artwork.mpe")

    execute {
        // Settings: a popup opened from the root settings list. No manifest changes, because
        // root installs cannot register new activities. The entry is only added when the click
        // hook could be installed, otherwise tapping it would crash.
        val startHook = AmStartActivityFingerprint.matchOrNull()
        val headers = resourceXmlOrNull("res/xml/settings_headers.xml")
        if (startHook != null && headers != null) {
            startHook.method.addInstructionsWithLabels(
                0,
                """
                    invoke-static/range { p0 .. p1 }, $UI->handleStartActivity(Landroid/app/Activity;Landroid/content/Intent;)Z
                    move-result p1
                    if-eqz p1, :am_continue
                    return-void
                    :am_continue
                    invoke-static {}, $UI->takeIntent()Landroid/content/Intent;
                    move-result-object p1
                """
            )
            headers.use { xml ->
                xml.elements("PreferenceScreen").first().appendChild("Preference") {
                    setAttribute("android:key", "apple_music_artwork_settings")
                    setAttribute("android:title", "Apple Music artwork")
                    setAttribute("android:summary", "Animated artwork on/off, logs \u00b7 by Archimetrix")
                    appendChild("intent") { setAttribute("android:action", "dev.archimetrix.applemusic.SETTINGS") }
                }
            }
        }

        AmActivityOnCreateFingerprint.method.addInstruction(
            0, "invoke-static/range { p0 .. p0 }, $EXT->onActivityCreate(Landroid/app/Activity;)V"
        )
    }

    finalize {
        for ((fp, target) in listOf(
            AmSetMetadataFingerprint to "onMetadata(Landroid/media/MediaMetadata;)V",
            AmSetPlaybackStateFingerprint to "onPlaybackState(Landroid/media/session/PlaybackState;)V",
        )) {
            fp.clearMatch() // other bundles may have shifted the call
            fp.method.apply {
                val index = fp.instructionMatches.last().index
                val call = getInstruction<FiveRegisterInstruction>(index)
                addInstruction(index, "invoke-static { v${call.registerD} }, $EXT->$target")
            }
        }
    }
}
