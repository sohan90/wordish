package com.conversant.app.wordish.features.gameplay

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.conversant.app.wordish.R
import com.conversant.app.wordish.WordSearchApp
import com.conversant.app.wordish.commons.*
import com.conversant.app.wordish.commons.DurationFormatter.fromInteger
import com.conversant.app.wordish.custom.LetterBoard.OnLetterSelectionListener
import com.conversant.app.wordish.custom.StreakView.StreakLine
import com.conversant.app.wordish.data.room.WordDataSource
import com.conversant.app.wordish.features.FullscreenActivity
import com.conversant.app.wordish.features.SoundPlayer
import com.conversant.app.wordish.features.gameover.GameOverActivity
import com.conversant.app.wordish.features.gameplay.GamePlayViewModel.*
import com.conversant.app.wordish.model.*
import kotlinx.android.synthetic.main.activity_game_play.*
import kotlinx.android.synthetic.main.partial_game_complete.*
import kotlinx.android.synthetic.main.partial_game_content.*
import kotlinx.android.synthetic.main.partial_game_header.*
import javax.inject.Inject


class GamePlayActivity : FullscreenActivity() {

    private var fireViewList = mutableListOf<View>()

    private lateinit var endStreakLine: StreakLine

    private lateinit var beginStreakLine: StreakLine

    private var clickedState: Boolean = false

    @Inject
    lateinit var soundPlayer: SoundPlayer

    @Inject
    lateinit var wordDataSource: WordDataSource

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: GamePlayViewModel by viewModels { viewModelFactory }

    private var letterAdapter: ArrayLetterGridDataAdapter? = null

    private var popupTextAnimation: Animation? = null

    private val extraGameMode: GameMode by lazy {
        (intent.extras?.get(EXTRA_GAME_MODE) as? GameMode) ?: GameMode.Normal
    }

    private val extraDifficulty: Difficulty by lazy {
        (intent.extras?.get(EXTRA_GAME_DIFFICULTY) as? Difficulty) ?: Difficulty.Easy
    }

    private val extraGameThemeId: Int by lazy {
        intent.extras?.getInt(EXTRA_GAME_THEME_ID).orZero()
    }

    private val extraRowCount: Int by lazy {
        intent.extras?.getInt(EXTRA_ROW_COUNT).orZero()
    }

    private val extraColumnCount: Int by lazy {
        intent.extras?.getInt(EXTRA_COL_COUNT).orZero()
    }

    private val extraGameId: Int by lazy {
        intent.extras?.getInt(EXTRA_GAME_DATA_ID).orZero()
    }

    private var rowColListPair = arrayListOf<Pair<Int, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_play)
        (application as WordSearchApp).appComponent.inject(this)

        initViews()
        initViewModel()
        initClickListener()
        loadOrGenerateNewGame()
    }

    private fun addFireView(fireCount: Int) {
        val constraintLayout = parent_layout
        val constraintSet = ConstraintSet()

        fireViewList.forEach {
            constraintLayout.removeView(it)
        }

        fireViewList.clear()

        for (i in 0 until fireCount) {
            val imageView = ImageView(this)
            imageView.visibility = View.INVISIBLE
            fireViewList.add(imageView)

            imageView.id = View.generateViewId()
            imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.frame_0_delay))
            val size = resources.getDimension(R.dimen.bomb_width_height)
            val layoutParams = ConstraintLayout.LayoutParams(size.toInt(), size.toInt())
            imageView.layoutParams = layoutParams

            constraintLayout.addView(imageView)

            constraintSet.clone(constraintLayout)
            constraintSet.connect(
                imageView.id,
                ConstraintSet.BOTTOM,
                iv_water.id,
                ConstraintSet.BOTTOM
            )
            constraintSet.connect(imageView.id, ConstraintSet.START, iv_water.id, ConstraintSet.END)
            constraintSet.connect(
                imageView.id,
                ConstraintSet.END,
                constraintLayout.id,
                ConstraintSet.END
            )

            constraintSet.applyTo(constraintLayout)
        }

    }

    private fun animateNewWord(newCharList: List<Char>, colCount: Int) {
        val animation = AnimationUtils.loadAnimation(this, R.anim.new_words_text)
        new_word_tv.visible()
        animation.interpolator = DecelerateInterpolator()
        animation.duration = 1000
        new_word_tv.startAnimation(animation)
        animation.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                new_word_tv.gone()

                val col = endStreakLine.endIndex.col
                val colEnd = if (col == 0) col + 1 else col

                if (colCount == 1) {
                    Util.replaceNewWordForRow(
                        newCharList, colEnd - 1,
                        letter_board.letterGrid, letterAdapter!!.backedGrid
                    )

                    resetNewCharacters(2)
                } else {
                    Util.replaceNewWordForRow(
                        newCharList, colEnd,
                        letter_board.letterGrid, letterAdapter!!.backedGrid)

                    for (i in 0..5){
                        for (j in colEnd - 1..colEnd){
                            letterAdapter!!.initFire(i, j , true)
                        }
                    }
                    updateFireCountTxt(letterAdapter!!.fireList)
                }

            }
        })

    }

    private fun createRowColListForFireMoveAnim(fireCount: Int) {
        val rowList = mutableListOf(0, 1, 2, 3, 4, 5)
        val colList = mutableListOf(5,4,3,2,1,0)
        for (i in 0 until fireCount) {
            val pair = createRowColList(rowList, colList)
            rowColListPair.add(pair)

            rowList.remove(pair.first) // to get unique random number
            colList.remove(pair.first)
        }
    }

    private fun createRowColList(rowList:List<Int>, colList:List<Int>): Pair<Int, Int> {
        val first = rowList.shuffled().take(1)[0]
        val second = colList.shuffled().take(1)[0]
        val pair = Pair(first, second)

        val fireCountOnBoard = tv_fire_count.text.toString() ?: ""
        if (letterAdapter!!.fireList[first][second] &&
            fireCountOnBoard != "36"
        ) {
            createRowColList(rowList, colList)
        } else {
            return pair
        }

        return pair
    }

    private fun animateFireMove(
        v: View,
        targetX: Float,
        targetY: Float,
        row: Int = 0,
        col: Int = 0
    ) {
        val animSetXY = AnimatorSet()
        val x: ObjectAnimator = ObjectAnimator.ofFloat(v, "x", v.x, targetX)

        val y: ObjectAnimator = ObjectAnimator.ofFloat(v, "y", v.y, targetY)

        animSetXY.playTogether(x, y)
        animSetXY.duration = 1000
        animSetXY.start()

        animSetXY.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                if (iv_bomb.scaleX != 1.2f) {
                    letterAdapter!!.fireList[row][col] = true
                    updateFireCountTxt(letterAdapter!!.fireList)
                    v.x = iv_fire_plus.x
                    v.y = iv_fire_plus.y
                    v.visibility = View.INVISIBLE
                } else {
                    // bomb activated
                    explodeBomb()
                    soundPlayer.play(SoundPlayer.Sound.Bomb)
                    disableOrEnableOtherPowerUps(1f, iv_bomb)
                }
            }
        })
    }

    private fun explodeBomb() {
        val params: ConstraintLayout.LayoutParams =
            bomb.layoutParams as ConstraintLayout.LayoutParams
        params.width = WRAP_CONTENT
        params.height = WRAP_CONTENT
        bomb.layoutParams = params

        val valueAnimator = ValueAnimator.ofInt(0, 12)
        valueAnimator.addUpdateListener {
            val i = it.animatedValue as Int
            val imageName = "frame_bomb_${i}_delay"
            val res = resources.getIdentifier(imageName, "drawable", packageName);
            val drawableImg = ContextCompat.getDrawable(this, res)
            bomb.setImageDrawable(drawableImg)

            if (i == 4) {
                letter_board.explodeCells(streakLine = beginStreakLine)
            }
        }
        valueAnimator.doOnEnd {
            bomb.alpha = 1f
            bomb.animate().withEndAction {
                resetBombImage()
                resetNewCharacters(1)

            }.alpha(0f)
                .setDuration(300)
                .start()
        }
        valueAnimator.duration = 1000
        valueAnimator.start()
    }

    private fun resetNewCharacters(colCount: Int) {
        val newCharList = Util.getNewCharList()
        new_word_tv.text = newCharList.joinToString(separator = ",")
        animateNewWord(newCharList, colCount)
    }

    private fun resetBombImage() {
        bomb.visibility = View.GONE
        bomb.alpha = 1f
        bomb.x = iv_bomb.x
        bomb.y = iv_bomb.y

        val params: ConstraintLayout.LayoutParams =
            bomb.layoutParams as ConstraintLayout.LayoutParams
        params.width = resources.getDimension(R.dimen.bomb_width_height).toInt()
        params.height = resources.getDimension(R.dimen.bomb_width_height).toInt()
        bomb.layoutParams = params

        bomb.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_bomb))

    }

    private fun initClickListener() {
        iv_water.setOnClickListener {
            clickedState = if (!clickedState) {
                disableOrEnableOtherPowerUps(0.4f, it)
                true
            } else {
                disableOrEnableOtherPowerUps(1f, it)
                false
            }
        }
        iv_bomb.setOnClickListener {
            clickedState = if (!clickedState) {
                disableOrEnableOtherPowerUps(0.4f, it)
                true
            } else {
                disableOrEnableOtherPowerUps(1f, it)
                false
            }
        }
    }

    private fun animateFireMoveFromBank() {
        fireViewList.forEachIndexed { index, view ->
            val pair = rowColListPair[index]

            val row = pair.first
            val col = pair.second

            val cellX = letter_board.streakView.grid?.getCenterColFromIndex(col)!!.toFloat()
            val cellY = letter_board.streakView.grid?.getCenterRowFromIndex(row)!!
                .toFloat() + letter_board.y

            view.visibility = View.VISIBLE
            animateFireMove(view, cellX - 30, cellY - 40, row, col)
        }
        rowColListPair.clear()
    }

    private fun disableOrEnableOtherPowerUps(alphaVal: Float, view: View) {
        iv_fire.alpha = alphaVal
        iv_bomb.alpha = alphaVal
        iv_water.alpha = alphaVal
        iv_fire_plus.alpha = alphaVal
        tv_fire_count.alpha = alphaVal

        if (alphaVal == 1f) {
            view.animate().scaleX(1.0f).scaleY(1.0f).start()
            letter_board.shrinkFireWithWater(false)
            clickedState = false
        } else {
            view.animate().scaleX(1.2f).scaleY(1.2f).start()
            if (view.id == iv_water.id) {
                letter_board.shrinkFireWithWater(true)
            }
            clickedState = true
        }
        view.alpha = 1f
    }

    private fun initViews() {
        text_current_selected_word.setInAnimation(this, android.R.anim.slide_in_left)
        text_current_selected_word.setOutAnimation(this, android.R.anim.slide_out_right)
        letter_board.streakView.setEnableOverrideStreakLineColor(preferences.grayscale())
        letter_board.streakView.setOverrideStreakLineColor(resources.getColor(R.color.gray))
        letter_board.selectionListener = object : OnLetterSelectionListener {

            override fun onSelectionWord() {
                soundPlayer.play(SoundPlayer.Sound.Highlight)
            }

            override fun onSelectionBegin(streakLine: StreakLine, str: String) {
                beginStreakLine = streakLine
                animateBombIfActivated(streakLine)
                streakLine.color = Util.getRandomColorWithAlpha(170)
                text_selection_layout.visible()
                text_selection.text = str
            }

            override fun onSelectionDrag(streakLine: StreakLine, str: String) {
                text_selection_layout.visible()
                text_selection.text = if (str.isEmpty()) "..." else str
            }

            override fun onSelectionEnd(streakLine: StreakLine, str: String) {
                endStreakLine = streakLine
                viewModel.answerWord(str, streakLine, preferences.reverseMatching())
                text_selection_layout.gone()
                text_selection.text = str
            }

            override fun onSelectionFireCell(streakLine: StreakLine, hasFire: Boolean) {
                if (hasFire) {
                    if (iv_water.scaleX == 1.2f) {// water droplets enabled
                        soundPlayer.play(SoundPlayer.Sound.WaterDroplets)
                    }
                } else {
                    updateFireCountTxt(letterAdapter!!.fireList)
                    disableOrEnableOtherPowerUps(1f, iv_water)
                }
            }
        }

        letter_board.letterGrid.setListener(letter_board.selectionListener!!)
        letter_board.streakView.isSnapToGrid = preferences.snapToGrid
        text_game_finished.gone()
        popupTextAnimation = AnimationUtils.loadAnimation(this, R.anim.popup_text)
        popupTextAnimation?.duration = 1000
        popupTextAnimation?.interpolator = DecelerateInterpolator()
        popupTextAnimation?.setAnimationListener(object : AnimationListener {
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                text_popup_correct_word.gone()
                text_popup_correct_word.text = ""
            }
        })
    }

    private fun animateBombIfActivated(streakLine: StreakLine) {
        if (iv_bomb.scaleX == 1.2f) {//is active

            bomb.visibility = View.VISIBLE

            val row = streakLine.startIndex.row
            val col = streakLine.startIndex.col

            val X = letter_board.streakView.grid?.getCenterColFromIndex(col)?.toFloat()
            val Y = letter_board.streakView.grid?.getCenterRowFromIndex(row)
                ?.toFloat()!! + letter_board.y

            animateFireMove(bomb, X!! - 50, Y - 100)
        }
    }

    private fun initViewModel() {
        viewModel.onTimer.observe(this, Observer { duration: Int -> showDuration(duration) })
        viewModel.onCountDown.observe(this, Observer { countDown: Int -> showCountDown(countDown) })
        viewModel.onGameState.observe(
            this,
            Observer { gameState: GameState -> onGameStateChanged(gameState) })
        viewModel.onAnswerResult.observe(
            this,
            Observer { answerResult: AnswerResult ->}) //onAnswerResult(answerResult) })

        viewModel.onAnswerResultWord.observe(this, {
            onAnswerResult(it)
        })
        viewModel.onCurrentWordChanged.observe(this, Observer { usedWord: UsedWord ->
            text_current_selected_word.setText(usedWord.string)
            progress_word_duration.max = usedWord.maxDuration * 100
            animateProgress(progress_word_duration, usedWord.remainingDuration * 100)
        })
        viewModel.onCurrentWordCountDown.observe(
            this,
            Observer { duration: Int -> animateProgress(progress_word_duration, duration * 100) })
    }

    private fun loadOrGenerateNewGame() {
        if (shouldOpenExistingGameData()) {
            viewModel.loadGameRound(extraGameId)
        } else {
            generateNewGame()
        }
    }

    private fun generateNewGame() {
        viewModel.generateNewGameRound(
            rowCount = extraRowCount,
            colCount = extraColumnCount,
            gameThemeId = extraGameThemeId,
            gameMode = extraGameMode,
            difficulty = extraDifficulty
        )
    }

    private fun shouldOpenExistingGameData(): Boolean {
        return intent.extras?.containsKey(EXTRA_GAME_DATA_ID) ?: false
    }

    override fun onStart() {
        super.onStart()
        viewModel.resumeGame()
    }

    override fun onStop() {
        super.onStop()
        viewModel.pauseGame()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopGame()
    }

    private fun animateProgress(progressBar: ProgressBar?, progress: Int) {
        val anim = ObjectAnimator.ofInt(progressBar, "progress", progress)
        anim.duration = 250
        anim.start()
    }

    private fun onAnswerResult(onAnswerWord: AnswerResultWord) {
        if (onAnswerWord.correct) {
            text_popup_correct_word.visible()
            text_popup_correct_word.text = onAnswerWord.correctWord
            text_popup_correct_word.startAnimation(popupTextAnimation)
            soundPlayer.play(SoundPlayer.Sound.Correct)

            Util.replaceNewWordForCorrectWord(
                onAnswerWord.streakLine, letterAdapter!!.backedGrid,
                letterAdapter!!.completedCell, letterAdapter!!.fireList)

            addFireToCellFromBank(1)
            fireViewList.forEach {
                it.post {
                    animateFireMoveFromBank()
                }
            }
        } else {
            soundPlayer.play(SoundPlayer.Sound.Wrong)
        }
    }

    private fun onGameStateChanged(gameState: GameState) {
        showLoading(false, null)
        when (gameState) {
            is Generating -> {
                var text = getString(R.string.text_generating)
                text = text.replace(":row".toRegex(), gameState.rowCount.toString())
                text = text.replace(":col".toRegex(), gameState.colCount.toString())
                showLoading(true, text)
            }
            is Loading -> {
                showLoading(true, getString(R.string.lbl_load_game_data))
            }
            is Finished -> {
                showFinishGame(gameState)
            }
            is Playing -> {
                gameState.gameData?.let { onGameRoundLoaded(it)
                    addFireToCellFromBank(1)
                    Handler(Looper.getMainLooper()).postDelayed({
                        animateFireMoveFromBank()
                    }, 2000)
                }
            }
        }
    }

    private fun addFireToCellFromBank(fireCount: Int) {
        createRowColListForFireMoveAnim(fireCount)
        addFireView(fireCount)
    }

    private fun onGameRoundLoaded(gameData: GameData) {
        if (gameData.isFinished) {
            letter_board.streakView.isInteractive = false
            text_game_finished.visible()
            layout_complete_popup.visible()
            text_complete_popup.setText(R.string.lbl_complete)
        } else if (gameData.isGameOver) {
            letter_board.streakView.isInteractive = false
            layout_complete_popup.visible()
            text_complete_popup.setText(R.string.lbl_game_over)
        }
        showLetterGrid(
            gameData.grid!!.array,
            gameData.grid!!.fireArray,
            gameData.grid!!.highlight,
            gameData.grid!!.waterDrop,
            gameData.grid!!.completedCellHighlight
        )
        showDuration(gameData.duration)
        showUsedWords(gameData.usedWords, gameData)
        showWordsCount(gameData.usedWords.size)
        showAnsweredWordsCount(gameData.answeredWordsCount)
        doneLoadingContent()
        when {
            gameData.gameMode === GameMode.CountDown -> {
                progress_overall_duration.visible()
                progress_overall_duration.max = gameData.maxDuration * PROGRESS_SCALE
                progress_overall_duration.progress = gameData.remainingDuration * PROGRESS_SCALE
                layout_current_selected_word.gone()
            }
            gameData.gameMode === GameMode.Marathon -> {
                progress_overall_duration.gone()
                layout_current_selected_word.visible()
            }
            else -> {
                progress_overall_duration.gone()
                layout_current_selected_word.gone()
            }
        }
    }

    private fun tryScale() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val boardWidth = letter_board.width
        val screenWidth = metrics.widthPixels
        if (preferences.autoScaleGrid() || boardWidth > screenWidth) {
            val scale = screenWidth.toFloat() / boardWidth.toFloat()
            letter_board.scale(scale, scale)
        }
    }

    private fun doneLoadingContent() {
        // call tryScale() on the next render frame
        Handler().postDelayed({ tryScale() }, 100)
    }

    private fun showLoading(enable: Boolean, text: String?) {
        if (enable) {
            loading.visible()
            loadingText.visible()
            content_layout.gone()
            loadingText.text = text
        } else {
            loading.gone()
            loadingText.gone()
            if (content_layout.visibility == View.GONE) {
                content_layout.visible()
                content_layout.scaleY = 0.5f
                content_layout.alpha = 0f
                content_layout.animate()
                    .scaleY(1f)
                    .setDuration(400)
                    .start()
                content_layout.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .start()
            }
        }
    }

    private fun showLetterGrid(
        grid: Array<CharArray>,
        fireArray: Array<BooleanArray>,
        highlight: Array<BooleanArray>,
        waterDrop: Array<BooleanArray>,
        completedCellHighlight: Array<BooleanArray>
    ) {
        if (letterAdapter == null) {
            letterAdapter = ArrayLetterGridDataAdapter(
                grid,
                fireArray,
                highlight,
                waterDrop,
                completedCellHighlight
            )
            letterAdapter?.let {
                letter_board.dataAdapter = it
            }
        } else {
            letterAdapter?.grid = grid
        }
    }

    private fun updateFireCountTxt(fireArray: Array<BooleanArray>) {
        var count = 0
        fireArray.map {
            for (b in it) {
                if (b) {
                    count++
                }
            }
        }
        updateFireTxt(count)
    }

    private fun showDuration(duration: Int) {
        text_overall_duration.text = fromInteger(duration)
    }

    private fun showCountDown(countDown: Int) {
        animateProgress(progress_overall_duration, countDown * PROGRESS_SCALE)
    }

    private fun showUsedWords(usedWords: List<UsedWord>, gameData: GameData) {
        flexbox_layout.removeAllViews()
        for (uw in usedWords) {
            flexbox_layout.addView(createUsedWordTextView(uw, gameData))
        }
    }

    private fun showAnsweredWordsCount(count: Int) {
        text_answered_string_count.text = count.toString()
    }

    private fun showWordsCount(count: Int) {
        text_words_count.text = count.toString()
    }

    private fun updateFireTxt(count: Int) {
        tv_fire_count.text = count.toString()
    }

    private fun showFinishGame(state: Finished) {
        letter_board.streakView.isInteractive = false
        val anim = AnimationUtils.loadAnimation(this, R.anim.game_complete)
        anim.interpolator = DecelerateInterpolator()
        anim.duration = 500
        anim.startOffset = 1000
        anim.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                Handler(Looper.myLooper()!!).postDelayed({
                    val intent = Intent(this@GamePlayActivity, GameOverActivity::class.java)
                    intent.putExtra(
                        GameOverActivity.EXTRA_GAME_ROUND_ID,
                        state.gameData?.id.orZero()
                    )
                    startActivity(intent)
                    finish()
                }, 800)
            }
        })
        if (state.win) {
            text_complete_popup.setText(R.string.lbl_complete)
            Handler(Looper.myLooper()!!).postDelayed(
                { soundPlayer.play(SoundPlayer.Sound.Winning) },
                600
            )
        } else {
            text_complete_popup.setText(R.string.lbl_game_over)
            Handler().postDelayed({ soundPlayer.play(SoundPlayer.Sound.Lose) }, 600)
        }
        layout_complete_popup.visible()
        layout_complete_popup.startAnimation(anim)
    }

    //
    private fun createUsedWordTextView(usedWord: UsedWord, gameData: GameData): View {
        val view = layoutInflater.inflate(R.layout.item_word, flexbox_layout, false)
        val str = view.findViewById<TextView>(R.id.textStr)
        if (usedWord.isAnswered) {
            if (preferences.grayscale()) {
                usedWord.answerLine?.color = resources.getColor(R.color.gray)
            }

            view.background.setColorFilter(
                usedWord.answerLine!!.color,
                PorterDuff.Mode.MULTIPLY
            )
            str.text = usedWord.string
            str.setTextColor(Color.WHITE)
            str.paintFlags = str.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            letter_board.addStreakLine(STREAK_LINE_MAPPER.map(usedWord.answerLine!!))
        } else {
            if (gameData.gameMode === GameMode.Hidden) {
                str.text = getHiddenMask(usedWord.string)
            } else {
                str.text = usedWord.string
            }
        }
        view.tag = usedWord.id
        return view
    }

    private fun getHiddenMask(string: String): String {
        val sb = StringBuilder(string.length)
        for (i in string.indices) sb.append(resources.getString(R.string.hidden_mask))
        return sb.toString()
    }

    companion object {
        const val EXTRA_GAME_DIFFICULTY = "game_max_duration"
        const val EXTRA_GAME_DATA_ID = "game_data_id"
        const val EXTRA_GAME_MODE = "game_mode"
        const val EXTRA_GAME_THEME_ID = "game_theme_id"
        const val EXTRA_ROW_COUNT = "row_count"
        const val EXTRA_COL_COUNT = "col_count"
        private val STREAK_LINE_MAPPER = StreakLineMapper()
        private const val PROGRESS_SCALE = 100
    }
}