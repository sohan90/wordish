package com.conversant.app.wordish.features

import android.content.Context
import android.media.AudioManager
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
        Correct, Wrong, Winning, Lose, Bomb, Fire, WaterDroplets
    }

    private var soundPool: SoundPool = SoundPool(2, AudioManager.STREAM_MUSIC, 0)
    private val soundPoolMap: SparseIntArray = SparseIntArray()

    fun play(sound: Sound) {
        if (mPreferences.enableSound()) {
            soundPool.play(soundPoolMap[sound.ordinal],
                1.0f, 1.0f, 0, 0, 1.0f)
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
            soundPool.load(context, R.raw.winning, 1)
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
    }
}