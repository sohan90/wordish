package com.conversant.app.wordish.features.gameover

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conversant.app.wordish.commons.SingleLiveEvent
import com.conversant.app.wordish.data.room.UsedWordDataSource
import com.conversant.app.wordish.data.sqlite.GameDataSource
import com.conversant.app.wordish.model.GameData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GameOverViewModel @Inject constructor(private val gameDataSource: GameDataSource, private val usedWordDataSource: UsedWordDataSource) : ViewModel() {
    private var gameData: GameData? = null
    private val _onGameDataLoaded = MutableLiveData<GameData?>()
    private val _onGameDataReset = SingleLiveEvent<Int>()

    @SuppressLint("CheckResult")
    suspend fun loadData(gid: Int) {
        gameData = gameDataSource.getGameData(gid)
        _onGameDataLoaded.value = gameData
    }

    suspend fun deleteGameRound() {
        gameData?.let {
            gameDataSource.deleteGameData(it.id)
        }
    }

    suspend fun resetCurrentGameData() {
        gameData?.let { gameData ->
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    usedWordDataSource.resetUsedWords(gameData.id)
                    gameDataSource.saveGameDataDuration(gameData.id, 0)
                }
                _onGameDataReset.value = gameData.id
            }
        }
    }

    val onGameDataReset: LiveData<Int>
        get() = _onGameDataReset

    val onGameDataLoaded: LiveData<GameData?>
        get() = _onGameDataLoaded

}