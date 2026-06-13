package com.stepandemianenko.sdtfitness.startworkout

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.stepandemianenko.sdtfitness.R

/**
 * Plays a short "set complete" chime and a light haptic tick when the user marks
 * a set as completed on the ongoing workout screen.
 *
 * The chime is chosen by the set's position: set 1 -> sound 1 ... set 4 -> sound 4,
 * then it loops (set 5 -> sound 1, set 6 -> sound 2, and so on), so each set always
 * plays the same chime and a full block of four sets forms a short ascending sequence.
 *
 * Backed by [SoundPool], which is purpose-built for low-latency playback of short,
 * frequently triggered clips. Call [release] when the owning screen is disposed.
 */
class SetCompletionFeedback(context: Context) {

    private val appContext = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sampleIds: List<Int> = SET_COMPLETE_SOUNDS.map { resId ->
        soundPool.load(appContext, resId, 1)
    }

    private val vibrator: Vibrator? = resolveVibrator(appContext)

    /**
     * Plays the chime tied to [setNumber] (1-based) and fires a short haptic tick.
     * Sets beyond the fourth wrap around, so set N plays sound ((N - 1) mod 4) + 1.
     */
    fun playSetCompleted(setNumber: Int) {
        if (sampleIds.isNotEmpty()) {
            val index = ((setNumber - 1) % sampleIds.size + sampleIds.size) % sampleIds.size
            soundPool.play(sampleIds[index], 1f, 1f, 1, 0, 1f)
        }
        vibrate()
    }

    private fun vibrate() {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Tuned "click" primitive devices reliably render, instead of a faint short pulse.
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        } else {
            VibrationEffect.createOneShot(VIBRATION_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        // Tag as touch feedback so the system doesn't suppress it (e.g. under Do Not Disturb).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attributes = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_TOUCH)
                .build()
            vib.vibrate(effect, attributes)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(effect, TOUCH_AUDIO_ATTRIBUTES)
        }
    }

    fun release() {
        soundPool.release()
    }

    private companion object {
        const val VIBRATION_MILLIS = 40L

        // Pre-Android 13 way to tag a vibration as touch feedback so it isn't filtered.
        val TOUCH_AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        @RawRes
        val SET_COMPLETE_SOUNDS = listOf(
            R.raw.set_complete_1,
            R.raw.set_complete_2,
            R.raw.set_complete_3,
            R.raw.set_complete_4
        )

        fun resolveVibrator(context: Context): Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
    }
}

/**
 * Remembers a [SetCompletionFeedback] tied to the current composition and releases
 * its [SoundPool] when the composable leaves the tree.
 */
@Composable
fun rememberSetCompletionFeedback(): SetCompletionFeedback {
    val context = LocalContext.current
    val feedback = remember(context) { SetCompletionFeedback(context) }
    DisposableEffect(feedback) {
        onDispose { feedback.release() }
    }
    return feedback
}
