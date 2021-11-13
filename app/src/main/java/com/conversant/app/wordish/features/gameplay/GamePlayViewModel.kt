package com.conversant.app.wordish.features.gameplay

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conversant.app.wordish.commons.SingleLiveEvent
import com.conversant.app.wordish.commons.Timer
import com.conversant.app.wordish.commons.Timer.OnTimeoutListener
import com.conversant.app.wordish.commons.Util
import com.conversant.app.wordish.custom.FireInfo
import com.conversant.app.wordish.custom.StreakView
import com.conversant.app.wordish.data.room.UsedWordDataSource
import com.conversant.app.wordish.data.room.WordDataSource
import com.conversant.app.wordish.data.sqlite.GameDataSource
import com.conversant.app.wordish.features.settings.Preferences
import com.conversant.app.wordish.model.*
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.max

class GamePlayViewModel @Inject constructor(
    private val gameDataSource: GameDataSource,
    private val wordDataSource: WordDataSource,
    private val usedWordDataSource: UsedWordDataSource,
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

    init {
        timer.addOnTimeoutListener(object : OnTimeoutListener {
            override fun onTimeout(elapsedTime: Long) {
                onTimerTimeout()
            }
        })
        resetLiveData()
    }

    fun stopGame() {
        viewModelScope.launch(Dispatchers.IO) {
            usedWordDataSource.removeAll()
            currentGameData = null
            timer.stop()
            resetLiveData()
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

    @SuppressLint("CheckResult")
    fun loadGameRound(gid: Int) {
        if (currentState !is Generating) {
            setGameState(Loading())
            Observable
                .create { e: ObservableEmitter<GameData?> ->
                    e.onNext(gameDataSource.getGameDataSync(gid)!!)
                    e.onComplete()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { gameData: GameData? ->
                    currentGameData = gameData
                    currentDuration = currentGameData!!.duration
                    startGame()
                }
        }
    }

    @SuppressLint("CheckResult")
    fun generateNewGameRound(
        rowCount: Int,
        colCount: Int,
        gameThemeId: Int,
        gameMode: GameMode,
        difficulty: Difficulty
    ) {

        if (currentState is Generating) return

        val gameName = gameDataName
        setGameState(Generating(rowCount, colCount, gameName))
        val maxChar = max(rowCount, colCount)
        val flowableWords: Flowable<List<Word>> = if (gameThemeId == GameTheme.NONE.id) {
            wordDataSource.getWords(maxChar)
        } else {
            wordDataSource.getWords(gameThemeId, maxChar)
        }
        flowableWords.toObservable()
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
                val gameData =
                    gameDataCreator.newGameData(words, rowCount, colCount, gameName, gameMode)
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
            .doOnNext { gameRound: GameData? -> gameDataSource.saveGameData(gameRound!!) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { gameData: GameData? ->
                currentDuration = 0
                currentGameData = gameData
                startGame()
            }
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

    fun getCorrectWordLength():Int = correctWordLength

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

    private fun onTimerTimeout() {
        currentGameData?.let { gameData ->
            if (timer.isStarted) {
                gameData.duration = ++currentDuration
                val gameMode = gameData.gameMode
                if (gameMode === GameMode.CountDown) {
                    onCountDownLiveData.value = gameData.remainingDuration
                    if (gameData.remainingDuration <= 0) {
                        val win = gameData.answeredWordsCount ==
                                gameData.usedWords.size
                        timer.stop()
                        finishGame(win)
                    }
                } else if (gameMode == GameMode.Marathon) {
                    currentUsedWord?.let { usedWord ->
                        usedWord.duration = usedWord.duration + 1
                        onCurrentWordCountDownLiveData.value =
                            usedWord.maxDuration - usedWord.duration
                        Completable
                            .create { e: CompletableEmitter ->
                                usedWordDataSource.updateUsedWordDuration(
                                    usedWord.id,
                                    usedWord.duration
                                )
                                e.onComplete()
                            }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe()

                        if (usedWord.isTimeout) {
                            timer.stop()
                            finishGame(false)
                        }
                    }
                }
                onTimerLiveData.value = currentDuration
                gameDataSource.saveGameDataDuration(gameData.id, currentDuration)
            }
        }
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


    companion object {
        private const val TIMER_TIMEOUT = 1000
    }
}
