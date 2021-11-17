package com.conversant.app.wordish.features.gameplay

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conversant.app.wordish.commons.SingleLiveEvent
import com.conversant.app.wordish.commons.Timer
import com.conversant.app.wordish.commons.Util
import com.conversant.app.wordish.custom.FireInfo
import com.conversant.app.wordish.custom.StreakView
import com.conversant.app.wordish.data.room.GameStatusSource
import com.conversant.app.wordish.data.room.ScoreBoardDataSource
import com.conversant.app.wordish.data.room.WordDataSource
import com.conversant.app.wordish.features.settings.Preferences
import com.conversant.app.wordish.model.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.max

class GamePlayViewModel @Inject constructor(
    private val wordDataSource: WordDataSource,
    private val gameStatusDataSource: GameStatusSource,
    private val scoreBoardDataSource: ScoreBoardDataSource,
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

    class AnswerResult(
        var correct: Boolean,
        var usedWord: UsedWord?,
        var totalAnsweredWord: Int
    )

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
    private var currentDuration = 0
    private var currentUsedWord: UsedWord? = null
    private var currentState: GameState? = null

    private lateinit var onTimerLiveData: MutableLiveData<Int>
    private lateinit var onCountDownLiveData: MutableLiveData<Int>
    private lateinit var onCurrentWordCountDownLiveData: MutableLiveData<Int>
    private lateinit var onGameStateLiveData: MutableLiveData<GameState>
    private lateinit var onAnswerResultLiveData: SingleLiveEvent<AnswerResult>
    private lateinit var onAnswerResultWordLiveData: SingleLiveEvent<AnswerResultWord>
    private lateinit var onCurrentWordChangedLiveData: MutableLiveData<UsedWord>
    private lateinit var foundedWord: MutableLiveData<String>
    private lateinit var _htmlFileName: MutableLiveData<String>
    private lateinit var _gameStatusData: MutableLiveData<List<GameStatus>>
    private lateinit var _usedWordListForAdapter: MutableLiveData<List<String>>
    private lateinit var _scoreBoardLiveData: MutableLiveData<ScoreBoard>
    private lateinit var _quitGame: MutableLiveData<Boolean>


    val onTimer: LiveData<Int>
        get() = onTimerLiveData

    val onCountDown: LiveData<Int>
        get() = onCountDownLiveData

    val onGameState: LiveData<GameState>
        get() = onGameStateLiveData

    val onAnswerResult: LiveData<AnswerResult>
        get() = onAnswerResultLiveData

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

    init {
        resetLiveData()
    }

    suspend fun stopGame(
        gridData: Array<CharArray>,
        fireList: Array<Array<FireInfo>>,
        scoreBoard: ScoreBoard) {

        disposable.dispose()
        saveScoreBoardToDb(scoreBoard)
        saveBoardDataToDb(gridData, fireList)
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
        fireList: Array<Array<FireInfo>>
    ) {
        val dbList = gameStatusDataSource.getGameStatus()
        wordDataSource.updateWordForUsedWord(currentGameData!!.wordsList)

        val fireBuilder = StringBuilder()
        val firCounterBuilder = StringBuilder()

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
            }

            gameStatus.letterColumn = str
            gameStatus.fireColumn = fireBuilder.toString().removeSuffix(",,")
            gameStatus.fireCount = firCounterBuilder.toString().removeSuffix(",")
            list.add(gameStatus)

            fireBuilder.clear()
            firCounterBuilder.clear()

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

    fun generateNewGameRound(
        gameThemeId: Int,
        gameMode: GameMode,
        difficulty: Difficulty,
        newGame: Boolean,
        gameStatusList: List<GameStatus>
    ) {

        if (currentState is Generating) return

        val gameName = gameDataName
        setGameState(Generating(6, 6, gameName))
        val maxChar = max(6, 6)
        val flowableWords: Flowable<List<Word>> = if (gameThemeId == GameTheme.NONE.id) {
            wordDataSource.getWords(maxChar)
        } else {
            wordDataSource.getWords(gameThemeId, maxChar)
        }
        disposable = flowableWords.toObservable()
            .flatMap { words: List<Word> ->
                Flowable.fromIterable(words)
                    .distinct(Word::string)
                    .map { word: Word ->
                        word.string = word.string.toUpperCase(Locale.getDefault())
                        word
                    }
                    .toList()
                    .toObservable()
            }
            .flatMap { words: MutableList<Word> ->
                val gameData: GameData = if (newGame) {
                    gameDataCreator.newGameData(words, 6, 6, gameName, gameMode)
                } else {
                    getGameDataForSavedGame(words, gameStatusList)
                }
                if (gameMode === GameMode.CountDown) {
                    gameData.maxDuration =
                        getMaxCountDownDuration(gameData.usedWords.size, difficulty)
                } else if (gameMode === GameMode.Marathon) {
                    val maxDuration = getMaxDurationPerWord(difficulty)
                    for (usedWord in gameData.usedWords) {
                        usedWord.maxDuration = maxDuration
                    }
                }
                Observable.just(gameData)
            }
            .doOnNext { }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { gameData: GameData? ->
                currentDuration = 0
                currentGameData = gameData
                startGame()
            }

    }


    private fun getGameDataForSavedGame(
        words: List<Word>,
        dbGameStatusList: List<GameStatus>
    ): GameData {

        val grid = Grid(6, 6)
        for (row in 0..5) {

            val gameStatus = dbGameStatusList[row]
            Util.fillFireInfoSavedValue(
                gameStatus.fireColumn,
                gameStatus.fireCount,
                grid.fireArray
            ) // udpate fireinfo

            val charArray = gameStatus.letterColumn.toCharArray()
            for (col in 0..5) {
                val ch = charArray[col]
                grid.array[row][col] = ch // update grid array
            }
        }
        val gameData = GameData(grid = grid, newGame = false)
        gameData.addAllWords(words)
        return gameData
    }

    private fun startGame() {
        setGameState(Playing(currentGameData))
        currentGameData?.let {
            if (!it.isFinished && !it.isGameOver) {
                if (it.gameMode == GameMode.Marathon) {
                    nextWord()
                }

                timer.start()
            }
        }
    }

    fun getCorrectWordLength(): Int = correctWordLength

    fun getWaterForLongWordLength(wordLength:Int):Int{
        val value = wordLength % 7
        if (wordLength >= 7){
           return value + 1
        }
        return 0
    }

    fun answerWord(answerStr: String, streakLine: StreakView.StreakLine, reverseMatching: Boolean) {
        var correct = false

        val correctWord = checkWordFromWordList(answerStr, reverseMatching)

        if (correctWord != null && correctWord.isNotEmpty()) {
            correct = true
            correctWordLength = correctWord.length
        }

        val coins = coinsForWordLength(correctWord)
        onAnswerResultWordLiveData.value = AnswerResultWord(correct, correctWord, streakLine, coins)

    }

    fun checkWordFromWordList(answerStr: String, reverseMatching: Boolean): String? {

        var correctWord1: String? = null

        if (answerStr.length >= MINIMUM_LENGTH) {
            val answerStrRev = Util.getReverseString(answerStr)
            for (word in currentGameData?.wordsList.orEmpty()) {

                val dictionaryWord = word.string
                if (dictionaryWord.equals(answerStr, ignoreCase = true) ||
                    dictionaryWord.equals(answerStrRev, ignoreCase = true) && reverseMatching
                ) {
                    word.usedWord = true
                    correctWord1 = dictionaryWord
                    break
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

    private fun getMaxCountDownDuration(usedWordsCount: Int, difficulty: Difficulty): Int {
        return when {
            difficulty === Difficulty.Easy -> usedWordsCount * 19 // 19s per word
            difficulty === Difficulty.Medium -> usedWordsCount * 10 // 10s per word
            else -> usedWordsCount * 5 // 5s per word
        }
    }

    private fun getMaxDurationPerWord(difficulty: Difficulty): Int {
        return when {
            difficulty === Difficulty.Easy -> 25 // 19s per word
            difficulty === Difficulty.Medium -> 16 // 10s per word
            else -> 10 // 5s per word
        }
    }

    private fun nextWord() {
        currentGameData?.let {
            if (it.gameMode === GameMode.Marathon) {
                currentUsedWord = null

                for (usedWord in it.usedWords) {
                    if (!usedWord.isAnswered && !usedWord.isTimeout) {
                        currentUsedWord = usedWord
                        break
                    }
                }

                currentUsedWord?.let {
                    timer.stop()
                    timer.start()
                    onCurrentWordChangedLiveData.value = currentUsedWord
                }
            }
        }
    }

    private fun finishGame(win: Boolean) {
        setGameState(Finished(currentGameData, win))
    }

    private fun resetLiveData() {
        onTimerLiveData = MutableLiveData()
        onCountDownLiveData = MutableLiveData()
        onGameStateLiveData = MutableLiveData()
        onAnswerResultLiveData = SingleLiveEvent()
        onAnswerResultWordLiveData = SingleLiveEvent()
        onCurrentWordChangedLiveData = MutableLiveData()
        onCurrentWordCountDownLiveData = MutableLiveData()
        foundedWord = MutableLiveData()
        _gameStatusData = MutableLiveData()
        _usedWordListForAdapter = MutableLiveData()
        _scoreBoardLiveData = MutableLiveData()
        _htmlFileName = MutableLiveData()
        _quitGame = MutableLiveData()
    }

    fun getTotalFireCountFromBoard(fireArray: Array<Array<FireInfo>>): Int {
        var count = 0//16
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
                if (fireInfo.hasFire && fireInfo.fireCount == 5) { // spread fire to its adjacent sides each top left right bottom
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
            val list = wordDataSource.getUsedWordsList()
            if (list.isNotEmpty()) {
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

    suspend fun quitGame(updateQuitGame:Boolean = true) {
        disposable.dispose()
        wordDataSource.deleteAll()
        gameStatusDataSource.deleteAll()
        scoreBoardDataSource.deleteAll()
        if (updateQuitGame) {
            _quitGame.value = true
        }
    }

    companion object {
        private const val TIMER_TIMEOUT = 1000
    }
}
