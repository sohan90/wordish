package com.conversant.app.wordish.features.settings

import android.content.Context
import android.content.SharedPreferences
import com.conversant.app.wordish.R
import com.conversant.app.wordish.custom.StreakView.SnapType

/**
 * Created by abdularis on 19/07/17.
 */
class Preferences  constructor(
    context: Context,
    private val preferences: SharedPreferences
) {
    fun showGridLine(): Boolean {
        return preferences.getBoolean(KEY_SHOW_GRID_LINE, false)
    }

    val snapToGrid: SnapType
        get() {
            val str = preferences.getString(KEY_SNAP_TO_GRID, DEF_SNAP_TO_GRID)
            return SnapType.valueOf(str!!)
        }

    fun isMutedSound(): Boolean {
        return preferences.getBoolean(KEY_ENABLE_SOUND, false)
    }

    fun isRuleEnabled(): Boolean {
        return preferences.getBoolean(KEY_ENABLE_RULES, true)
    }

    fun enableFullscreen(): Boolean {
        return preferences.getBoolean(KEY_ENABLE_FULLSCREEN, true)
    }

    fun deleteAfterFinish(): Boolean {
        return preferences.getBoolean(KEY_DELETE_AFTER_FINISH, true)
    }

    fun autoScaleGrid(): Boolean {
        return preferences.getBoolean(KEY_AUTO_SCALE_GRID, true)
    }

    fun reverseMatching(): Boolean {
        return preferences.getBoolean(KEY_REVERSE_MATCHING, true)
    }

    fun grayscale(): Boolean {
        return preferences.getBoolean(KEY_GRAYSCALE, false)
    }

    fun muteSound(isEnable:Boolean){
        preferences.edit()
            .putBoolean(KEY_ENABLE_SOUND, isEnable)
            .apply()
    }

    fun showRules(isEnable:Boolean){
        preferences.edit()
            .putBoolean(KEY_ENABLE_RULES, isEnable)
            .apply()
    }

    fun resetSaveGameDataCount() {
        preferences.edit()
            .putInt(KEY_PREV_SAVE_GAME_DATA_COUNT, 1)
            .apply()
    }

    fun incrementSavedGameDataCount() {
        preferences.edit()
            .putInt(KEY_PREV_SAVE_GAME_DATA_COUNT, previouslySavedGameDataCount + 1)
            .apply()
    }

    val previouslySavedGameDataCount: Int
        get() = preferences.getInt(KEY_PREV_SAVE_GAME_DATA_COUNT, 1)

    companion object {
        private lateinit var KEY_SHOW_GRID_LINE: String
        private lateinit var KEY_SNAP_TO_GRID: String
        private lateinit var KEY_ENABLE_SOUND: String
        private lateinit var KEY_ENABLE_RULES: String
        private lateinit var KEY_DELETE_AFTER_FINISH: String
        private lateinit var KEY_ENABLE_FULLSCREEN: String
        private lateinit var KEY_AUTO_SCALE_GRID: String
        private lateinit var KEY_REVERSE_MATCHING: String
        private lateinit var KEY_GRAYSCALE: String
        private lateinit var DEF_SNAP_TO_GRID: String
        private const val KEY_PREV_SAVE_GAME_DATA_COUNT = "prevSaveGameDataCount"
    }

    init {
        KEY_SHOW_GRID_LINE = context.getString(R.string.pref_showGridLine)
        KEY_SNAP_TO_GRID = context.getString(R.string.pref_snapToGrid)
        KEY_ENABLE_SOUND = context.getString(R.string.pref_enableSound)
        KEY_ENABLE_RULES = context.getString(R.string.pref_enableRules)
        KEY_DELETE_AFTER_FINISH = context.getString(R.string.pref_deleteAfterFinish)
        KEY_ENABLE_FULLSCREEN = context.getString(R.string.pref_enableFullscreen)
        KEY_AUTO_SCALE_GRID = context.getString(R.string.pref_autoScaleGrid)
        KEY_REVERSE_MATCHING = context.getString(R.string.pref_reverseMatching)
        KEY_GRAYSCALE = context.getString(R.string.pref_grayscale)
        DEF_SNAP_TO_GRID = context.getString(R.string.snap_to_grid_def_val)
    }
}