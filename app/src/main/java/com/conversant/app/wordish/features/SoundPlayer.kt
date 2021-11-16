package com.conversant.app.wordish.features

import android.content.Context
import android.media.SoundPool
import android.util.SparseIntArray
import com.conversant.app.wordish.R
import com.conversant.app.wordish.features.settings.Preferences
import javax.inject.Inject

/**
 * Created by abdularis on 22/07/17.
 */
class SoundPlayer @Inject constructor(context: Context, private val mPreferences: Preferences) {

    enum class Sound {
        Correct, Wrong, Winning, Lose, Bomb, Fire, WaterDroplets, Highlight, Siren, PowerUp,
        SwipeCorrect, Dismiss, Open
    }

    private var streadId: Int = 0

    private var soundPool: SoundPool = SoundPool.Builder().setMaxStreams(2).build()
    private val soundPoolMap: SparseIntArray = SparseIntArray()

    fun play(sound: Sound) {
        if (mPreferences.enableSound()) {
            streadId = soundPool.play(
                soundPoolMap[sound.ordinal],
                1.0f, 1.0f, 0, 0, 1.0f
            )
        }
    }

    fun resume() {
        if (streadId != 0) {
            soundPool.resume(streadId)
        }
    }

    fun pause(){
        if (streadId != 0) {
            soundPool.pause(streadId)
        }
    }

    fun stop() {
        if (streadId != 0) {
            soundPool.stop(streadId)
        }
    }

    init {
        soundPoolMap.put(
            Sound.Correct.ordinal,
            soundPool.load(context, R.raw.correct_2, 1)
        )
        soundPoolMap.put(
            Sound.Wrong.ordinal,
            soundPool.load(context, R.raw.wrong_2, 1)
        )
        soundPoolMap.put(
            Sound.Winning.ordinal,
            soundPool.load(context, R.raw.applause, 1)
        )
        soundPoolMap.put(
            Sound.Lose.ordinal,
            soundPool.load(context, R.raw.lose, 1)
        )

        soundPoolMap.put(
            Sound.Bomb.ordinal,
            soundPool.load(context, R.raw.explosion_bomb, 1)
        )

        soundPoolMap.put(
            Sound.WaterDroplets.ordinal,
            soundPool.load(context, R.raw.water_droplets, 1)
        )

        soundPoolMap.put(
            Sound.Fire.ordinal,
            soundPool.load(context, R.raw.matches_fire, 1)
        )

        soundPoolMap.put(
            Sound.Highlight.ordinal,
            soundPool.load(context, R.raw.cell_highlight, 1)
        )

        soundPoolMap.put(
            Sound.Siren.ordinal,
            soundPool.load(context, R.raw.siren, 1)
        )

        soundPoolMap.put(
            Sound.PowerUp.ordinal,
            soundPool.load(context, R.raw.enable_power_up, 1)
        )

        soundPoolMap.put(
            Sound.SwipeCorrect.ordinal,
            soundPool.load(context, R.raw.swipe_correct, 1)
        )

        soundPoolMap.put(
            Sound.Open.ordinal,
            soundPool.load(context, R.raw.open_dialog, 1)
        )

        soundPoolMap.put(
            Sound.Dismiss.ordinal,
            soundPool.load(context, R.raw.dialog_dismiss, 1)
        )
    }
}