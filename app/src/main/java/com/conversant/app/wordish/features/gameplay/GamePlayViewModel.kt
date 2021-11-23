package com.conversant.app.wordish.features.gameplay

import android.animation.ValueAnimator
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conversant.app.wordish.commons.GridIndex
import com.conversant.app.wordish.commons.SingleLiveEvent
import com.conversant.app.wordish.commons.Timer
import com.conversant.app.wordish.commons.Util
import com.conversant.app.wordish.custom.FireInfo
import com.conversant.app.wordish.custom.StreakView
import com.conversant.app.wordish.data.room.GameStatusSource
import com.conversant.app.wordish.data.room.ScoreBoardDataSource
import com.conversant.app.wordish.data.room.TopScoreSource
import com.conversant.app.wordish.data.room.WordDefinitionSource
import com.conversant.app.wordish.data.xml.WordThemeDataXmlLoader.Companion.mapWord
import com.conversant.app.wordish.features.settings.Preferences
import com.conversant.app.wordish.model.*
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GamePlayViewModel @Inject constructor(
    private val gameStatusDataSource: GameStatusSource,
    private val scoreBoardDataSource: ScoreBoardDataSource,
    private val topScoreSource: TopScoreSource,
    private val wordDefinitionSource: WordDefinitionSource,
    private val preferences: Preferences
) : ViewModel() {

    abstract class GameState
    internal class Generating(
        var rowCount: Int,
        var colCount: Int,
        var name: String
    ) : GameState()

    internal class Loading : GameState()
    internal class Finished(var gameData: GameData?, var win: Boolean) : GameState()
    internal class Paused : GameState()
    internal class Playing(var gameData: GameData?) : GameState()

    class AnswerResultWord(
        var correct: Boolean,
        var correctWord: String?,
        val streakLine: StreakView.StreakLine,
        var coins: Int = 0
    )

    private lateinit var disposable: Disposable

    private var correctWordLength: Int = 0

    private val gameDataCreator: GameDataCreator = GameDataCreator()
    private var currentGameData: GameData? = null
    private val timer: Timer = Timer(TIMER_TIMEOUT.toLong())
    private var currentState: GameState? = null

    private lateinit var onTimerLiveData: MutableLiveData<Int>
    private lateinit var onCountDownLiveData: MutableLiveData<Int>
    private lateinit var onCurrentWordCountDownLiveData: MutableLiveData<Int>
    private lateinit var onGameStateLiveData: MutableLiveData<GameState>
    private lateinit var onAnswerResultWordLiveData: SingleLiveEvent<AnswerResultWord>
    private lateinit var onCurrentWordChangedLiveData: MutableLiveData<UsedWord>
    private lateinit var foundedWord: MutableLiveData<String>
    private lateinit var _htmlFileName: MutableLiveData<String>
    private lateinit var _gameStatusData: MutableLiveData<List<GameStatus>>
    private lateinit var _usedWordListForAdapter: MutableLiveData<List<String>>
    private lateinit var _scoreBoardLiveData: MutableLiveData<ScoreBoard>
    private lateinit var _quitGame: MutableLiveData<Boolean>
    private lateinit var _openSettingItemDialog: MutableLiveData<Int>


    val onTimer: LiveData<Int>
        get() = onTimerLiveData

    val onCountDown: LiveData<Int>
        get() = onCountDownLiveData

    val onGameState: LiveData<GameState>
        get() = onGameStateLiveData


    val onAnswerResultWord: LiveData<AnswerResultWord>
        get() = onAnswerResultWordLiveData

    val onCurrentWordChanged: LiveData<UsedWord>
        get() = onCurrentWordChangedLiveData

    val onCurrentWordCountDown: LiveData<Int>
        get() = onCurrentWordCountDownLiveData

    val selectedWord: LiveData<String>
        get() = foundedWord

    val htmlFileName: LiveData<String>
        get() = _htmlFileName

    val gameStatus: LiveData<List<GameStatus>>
        get() = _gameStatusData

    val usedWordListForAdapter: LiveData<List<String>>
        get() = _usedWordListForAdapter

    val scoreBoardLiveData: LiveData<ScoreBoard>
        get() = _scoreBoardLiveData

    val quitGame: LiveData<Boolean>
        get() = _quitGame

    val openSettingItemDialog: LiveData<Int>
        get() = _openSettingItemDialog

    init {
        resetLiveData()
    }

    suspend fun saveGame(
        adapterList: MutableList<String>,
        gridData: Array<CharArray>,
        fireList: Array<Array<FireInfo>>,
        completedCellArray: Array<BooleanArray>,
        scoreBoard: ScoreBoard
    ) {

        saveScoreBoardToDb(scoreBoard)
        saveBoardDataToDb(gridData, fireList, completedCellArray)
        saveTopScore(scoreBoard)
        saveUserWordToDb(adapterList)
    }

    private suspend fun saveUserWordToDb(adapterList: MutableList<String>) {
        val list = adapterList.map {
            val wordDefinition = WordDefinition()
            wordDefinition.usedWord = it
            wordDefinition
        }
        wordDefinitionSource.deleteAll()
        wordDefinitionSource.insertAll(list)
    }

    suspend fun saveTopScore(scoreBoard: ScoreBoard) {
        val topScore = topScoreSource.getTopScore()
        if (topScore != null) {
            if (scoreBoard.turns > topScore.turns) {
                topScore.turns = scoreBoard.turns
            }
            if (scoreBoard.coins > topScore.coins) {
                topScore.coins = scoreBoard.coins
            }
            if (scoreBoard.words > topScore.words) {
                topScore.words = scoreBoard.words
            }
            topScoreSource.update(topScore)
        } else {
            val score = TopScore(
                turns = scoreBoard.turns, coins = scoreBoard.coins,
                words = scoreBoard.words
            )
            topScoreSource.insert(score)
        }
    }

    private suspend fun saveScoreBoardToDb(scoreBoard: ScoreBoard) {
        val scoreBoardDb = scoreBoardDataSource.getScoreBoard()
        if (scoreBoardDb != null) {
            scoreBoardDb.turns = scoreBoard.turns
            scoreBoardDb.coins = scoreBoard.coins
            scoreBoardDb.words = scoreBoard.words
            scoreBoardDb.boardFireCount = scoreBoard.boardFireCount
            scoreBoardDb.bombCount = scoreBoard.bombCount
            scoreBoardDb.waterCount = scoreBoard.waterCount
            scoreBoardDb.boardFirePlusCount = scoreBoard.boardFirePlusCount

            scoreBoardDb.bombCountProgress = scoreBoard.bombCountProgress
            scoreBoardDb.waterCountProgress = scoreBoard.waterCountProgress
            scoreBoardDb.boardFirePlusCountProgress = scoreBoard.boardFirePlusCountProgress

            scoreBoardDataSource.update(scoreBoardDb)
        } else {
            scoreBoardDataSource.insert(scoreBoard)
        }
    }

    private suspend fun saveBoardDataToDb(
        gridData: Array<CharArray>,
        fireList: Array<Array<FireInfo>>,
        completedCellArray: Array<BooleanArray>
    ) {
        val dbList = gameStatusDataSource.getGameStatus()

        val fireBuilder = StringBuilder()
        val firCounterBuilder = StringBuilder()
        val completeCellBuilder = StringBuilder()

        val list = mutableListOf<GameStatus>()

        gridData.forEachIndexed { row, chars ->

            val str = chars.concatToString()
            val gameStatus = GameStatus()

            for (col in 0..5) {

                val fireInfo = fireList[row][col]

                if (fireInfo.hasFire || fireInfo.fireCount > 0) {
                    val fire = "[$row,$col]"
                    fireBuilder.append(fire)
                    firCounterBuilder.append(fireInfo.fireCount)

                    fireBuilder.append(",,")
                    firCounterBuilder.append(",")

                }

                val completeCell = completedCellArray[row][col]
                if (completeCell) {
                    val complete = "[$row,$col]"
                    completeCellBuilder.append(complete)

                    completeCellBuilder.append(",,")
                }
            }

            gameStatus.letterColumn = str
            gameStatus.fireColumn = fireBuilder.toString().removeSuffix(",,")
            gameStatus.fireCount = firCounterBuilder.toString().removeSuffix(",")

            gameStatus.completedColumn = completeCellBuilder.toString().removeSuffix(",,")
            list.add(gameStatus)

            fireBuilder.clear()
            firCounterBuilder.clear()
            completeCellBuilder.clear()

        }

        if (dbList.isEmpty()) {
            gameStatusDataSource.insertAll(list)
        } else {
            gameStatusDataSource.deleteAll()
            gameStatusDataSource.insertAll(list)
        }
    }

    fun pauseGame() {
        if (currentState !is Playing) return

        currentGameData?.let {
            if (!it.isFinished && !it.isGameOver) {
                timer.stop()
                setGameState(Paused())
            }
        }
    }

    fun resumeGame() {
        if (currentState is Paused) {
            timer.start()
            setGameState(Playing(currentGameData))
        }
    }

    fun getGameStatusDataFromDb() {
        viewModelScope.launch {
            val list = gameStatusDataSource.getGameStatus()
            if (list.isNotEmpty()) {
                _gameStatusData.value = list
            } else {
                _gameStatusData.value = emptyList()
            }
        }
    }

    fun createGame(newGame: Boolean,gameStatusList: List<GameStatus>, gameMode: GameMode){
        setGameState(Generating(6, 6, gameDataName))
        viewModelScope.launch {
            val gameData: GameData = if (newGame) {
                gameDataCreator.newGameData(rowCount = 6, colCount = 6, gameMode = gameMode)
            } else {
                createGameDataFromSavedGame(gameStatusList)
            }
            currentGameData = gameData
            startGame()
        }
    }

    private fun createGameDataFromSavedGame(
        dbGameStatusList: List<GameStatus>
    ): GameData {

        val grid = Grid(6, 6)
        for (row in 0..5) {

            val gameStatus = dbGameStatusList[row]
            Util.fillFireInfoSavedValue(
                gameStatus.fireColumn, gameStatus.fireCount,
                grid.fireArray
            ) // udpate fireinfo

            Util.fillCompleteCellInfoFromDb(
                gameStatus.completedColumn,
                grid.completedCellHighlight
            )// update completed cell

            val charArray = gameStatus.letterColumn.toCharArray()
            for (col in 0..5) {
                val ch = charArray[col]
                grid.array[row][col] = ch // update grid array
            }
        }
        return GameData(grid = grid, newGame = false)
    }

    private fun startGame() {
        setGameState(Playing(currentGameData))
    }

    fun getCorrectWordLength(): Int = correctWordLength

    fun getWaterForLongWordLength(wordLength: Int): Int {
        if (wordLength >= 7) {
            val value = wordLength % 7
            return value + 1
        }
        return 0
    }

    fun answerWord(
        answerStr: String,
        streakLine: StreakView.StreakLine) {
        var correct = false

        val correctWord = checkWordFromWordList(answerStr)

        if (correctWord != null && correctWord.isNotEmpty()) {
            correct = true
            correctWordLength = correctWord.length
        }

        val coins = coinsForWordLength(correctWord)
        onAnswerResultWordLiveData.value = AnswerResultWord(correct, correctWord, streakLine, coins)

    }

    fun checkWordFromWordList(answerStr: String): String? {
        var correctWord1: String? = null

        if (answerStr.length >= MINIMUM_LENGTH) {

             val worList = mapWord[answerStr.length]

            if (worList != null) {
                val word = worList
                    .find { it.string.equals(answerStr, ignoreCase = true) }

                if (word != null) {
                    correctWord1 = word.string
                }
            }
        }
        return correctWord1
    }

    fun coinsForWordLength(str: String?): Int {
        return when (str?.length) {
            4 -> 22
            5 -> 30
            6 -> 42
            7 -> 50
            8 -> 62
            9 -> 70
            else -> 0
        }
    }

    private fun setGameState(state: GameState) {
        currentState = state
        onGameStateLiveData.value = currentState
    }

    private val gameDataName: String
        get() {
            val num = preferences.previouslySavedGameDataCount.toString()
            preferences.incrementSavedGameDataCount()
            return "Puzzle - $num"
        }


    private fun finishGame(win: Boolean) {
        setGameState(Finished(currentGameData, win))
    }

    private fun resetLiveData() {
        onTimerLiveData = MutableLiveData()
        onCountDownLiveData = MutableLiveData()
        onGameStateLiveData = MutableLiveData()
        onAnswerResultWordLiveData = SingleLiveEvent()
        onCurrentWordChangedLiveData = MutableLiveData()
        onCurrentWordCountDownLiveData = MutableLiveData()
        foundedWord = MutableLiveData()
        _gameStatusData = MutableLiveData()
        _usedWordListForAdapter = MutableLiveData()
        _scoreBoardLiveData = MutableLiveData()
        _htmlFileName = MutableLiveData()
        _quitGame = MutableLiveData()
        _openSettingItemDialog = MutableLiveData()
    }

    fun getTotalFireCountFromBoard(fireArray: Array<Array<FireInfo>>): Int {
        var count = 0
        fireArray.map {
            for (b in it) {
                if (b.hasFire) {
                    count++
                }
            }
        }
        return count
    }

    fun growFireCell(fireArray: Array<Array<FireInfo>>, backedGrid: Array<CharArray>) {
        for (row in fireArray.indices) {
            for (col in fireArray.indices) {
                val fireInfo = fireArray[row][col]
                if (fireInfo.hasFire && fireInfo.fireCount == 5) { // spread fire to its adjacent
                                                                  // sides each top left right bottom
                    val rowSides = getAdjacentSides(row)
                    val colSides = getAdjacentSides(col)

                    fireArray[rowSides.first][col].hasFire = true
                    fireArray[rowSides.second][col].hasFire = true

                    fireArray[row][colSides.first].hasFire = true
                    fireArray[row][colSides.second].hasFire = true
                }
            }
        }


        fireArray.map {
            for (b in it) {
                if (b.hasFire) {
                    if (b.fireCount < 5) {
                        b.fireCount = b.fireCount + 1
                    }
                }
            }
        }
    }

    private fun getAdjacentSides(rowOrCol: Int): Pair<Int, Int> {
        val leftOrTop: Int
        val rightOrBottom: Int

        when (rowOrCol) {
            0 -> {
                leftOrTop = 0
                rightOrBottom = rowOrCol + 1
            }
            5 -> {
                leftOrTop = rowOrCol - 1
                rightOrBottom = 5
            }
            else -> {
                leftOrTop = rowOrCol - 1
                rightOrBottom = rowOrCol + 1
            }
        }

        return Pair(leftOrTop, rightOrBottom)
    }

    fun setSelectedWord(s: String) {
        foundedWord.value = s
    }

    fun getUsedWordListForAdapter() {
        viewModelScope.launch {
            val list = wordDefinitionSource.getUsedWordList()
            if (list != null && list.isNotEmpty()) {
                _usedWordListForAdapter.value = list
            } else {
                _usedWordListForAdapter.value = mutableListOf()
            }
        }
    }

    fun getScoreBoardFromDb() {
        viewModelScope.launch {
            val scoreBoard = scoreBoardDataSource.getScoreBoard()
            if (scoreBoard != null) {
                _scoreBoardLiveData.value = scoreBoard
            } else {
                _scoreBoardLiveData.value = ScoreBoard(
                    turns = 0, bombCount = 1, bombCountProgress = 400,
                    waterCount = 1, waterCountProgress = 0, boardFirePlusCount = 1,
                    boardFirePlusCountProgress = 0
                )
            }
        }
    }

    fun setHtmlFileName(fileName: String) {
        _htmlFileName.value = fileName
    }

    suspend fun quitGame(showQuitDialog: Boolean = true) {
        wordDefinitionSource.deleteAll()
        gameStatusDataSource.deleteAll()
        scoreBoardDataSource.deleteAll()

        if (showQuitDialog) {
            _quitGame.value = true
        }
    }

    fun animateGameOverTile(gameOverCell: Array<BooleanArray>) {
        val valueAnimator = ValueAnimator.ofInt(0, 5)
        valueAnimator.addUpdateListener {
            val row: Int = it.animatedValue as Int
            for (col in 0..5) {
                gameOverCell[row][col] = true
            }
        }
        valueAnimator.duration = 5000
        valueAnimator.start()
    }

    fun highlightSelectedTileRange(
        highlightSelectedTilesRange: Array<BooleanArray>,
        list: List<GridIndex>
    ) {

        val endGridIndex = list[list.size - 1]
        val rowProgression = Util.getRowProgression(endGridIndex.row)
        val colProgression = Util.getColProgression(endGridIndex.col)

        for (row in 0..5) {
            for (col in 0..5) {
                highlightSelectedTilesRange[row][col] =
                    row in rowProgression && col in colProgression
            }
        }
    }

    fun resetHighlightedTileRange(highlightSelectedTilesRange: Array<BooleanArray>) {
        for (row in 0..5) {
            for (col in 0..5) {
                highlightSelectedTilesRange[row][col] = true
            }
        }
    }

    suspend fun getTopScore() = withContext(Dispatchers.Main){
        topScoreSource.getTopScore()
    }

    suspend fun updateWinCountToTopScore() {
        val topScore = topScoreSource.getTopScore()
        if (topScore != null) {
            topScore.won = topScore.won + 1
            topScoreSource.update(topScore)
        }
    }


    fun openSettingItemDialog(int: Int){
        _openSettingItemDialog.value = int
    }

    companion object {
        private const val TIMER_TIMEOUT = 1000
    }
}
