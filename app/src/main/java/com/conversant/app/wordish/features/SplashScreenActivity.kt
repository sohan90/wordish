package com.conversant.app.wordish.features

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.INFINITE
import android.animation.ValueAnimator.REVERSE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conversant.app.wordish.R
import com.conversant.app.wordish.WordishApp
import com.conversant.app.wordish.commons.gone
import com.conversant.app.wordish.data.room.GameDatabase
import com.conversant.app.wordish.data.room.WordDataSource
import com.conversant.app.wordish.data.xml.WordThemeDataXmlLoader.Companion.mapWord
import com.conversant.app.wordish.features.gameplay.GamePlayActivity
import com.conversant.app.wordish.model.Difficulty
import com.conversant.app.wordish.model.GameMode
import com.conversant.app.wordish.model.Word
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MAXIMUM_WORD_LENGTH = 15

class SplashScreenActivity : AppCompatActivity() {

    private var startedToFetch: Boolean = false

    private var disposable: Disposable? = null

    private var currentTime: Long = 0

    @Inject
    lateinit var wordDataSource: WordDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        (application as WordishApp).appComponent.inject(this)
        initView()
        parent_lyt.post {
            animateSwipe()
        }
    }

    private fun animateSwipe() {
        val lp: ViewGroup.MarginLayoutParams = iv_swipe.layoutParams as ViewGroup.MarginLayoutParams
        val valueAnimator = ValueAnimator.ofFloat(
            iv_swipe.x + lp.leftMargin,
            resources.getDimension(R.dimen.double_arrow_x)
        )
        valueAnimator.addUpdateListener {
            val xAxix: Float = it.animatedValue as Float
            iv_swipe.x = xAxix
        }
        valueAnimator.duration = 500
        valueAnimator.repeatCount = INFINITE
        valueAnimator.repeatMode = REVERSE
        valueAnimator.start()
    }

    private fun start() {
        currentTime = System.currentTimeMillis()
        disposable = wordDataSource.getWordsMayBe()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.isEmpty()) {
                    insertWord()
                } else {
                    loadDefinitionAndMap(it)
                }
            }
    }

    private fun insertWord() {
        lifecycleScope.launch {
            GameDatabase.prepopulateDatabase(this@SplashScreenActivity)
            val wordList = wordDataSource.getWordListForMax(15)
            loadDefinitionAndMap(wordList)
        }
    }

    private fun loadDefinitionAndMap(word: List<Word>) {
        lifecycleScope.launch {
            GameDatabase.loadDefinition(this@SplashScreenActivity)
            mapChunkWordList(word)
            nextScreen()
        }
    }

    private fun mapChunkWordList(word: List<Word>) {
        var i = 4
        while (i <= MAXIMUM_WORD_LENGTH) {
            val wordList = word.filter { it.string.length == i }
            mapWord[i] = wordList
            i++
        }

        val difference = (System.currentTimeMillis() - currentTime) / 1000
        Log.d("Difference..", "$difference")
    }

    private fun nextScreen() {
        val dim = 6
        val intent = Intent(this, GamePlayActivity::class.java)
        intent.putExtra(GamePlayActivity.EXTRA_GAME_DIFFICULTY, Difficulty.Easy)
        intent.putExtra(GamePlayActivity.EXTRA_GAME_MODE, GameMode.Normal)
        intent.putExtra(GamePlayActivity.EXTRA_GAME_THEME_ID, 1)
        intent.putExtra(GamePlayActivity.EXTRA_ROW_COUNT, dim)
        intent.putExtra(GamePlayActivity.EXTRA_COL_COUNT, dim)
        startActivity(intent)
        finish()
    }

    private fun initView() {
        val viewArray = arrayListOf<Button>(bt_s, bt_t, bt_a, bt_r, bt_tt)
        start_lyt.setOnTouchListener { v, event ->
            v.performClick()
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    enableButtons(viewArray, event)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    enableButtons(viewArray, event)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    checkAllButtonEnabled(viewArray)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun enableButtons(
        viewArray: ArrayList<Button>,
        event: MotionEvent
    ) {
        for (view in viewArray) {
            val xRange = view.left..view.right
            val yRange = view.top..view.bottom

            if (xRange.contains(event.x.toInt())) {
                if (yRange.contains(event.y.toInt())) {
                    view.isEnabled = true
                    break
                }
            }
        }
    }

    private fun checkAllButtonEnabled(viewArray: ArrayList<Button>) {
        var isAllButtonEnabled = true
        for (button in viewArray) {
            if (!button.isEnabled) {
                disableAllButtons(viewArray)
                isAllButtonEnabled = false
                break
            }
        }
        if (isAllButtonEnabled) {
            iv_swipe.gone()
            startButtonMoveAnimation(viewArray)
        }
    }

    private fun startButtonMoveAnimation(sourceBtn: List<Button>) {
        val targetView = bt_a

        for (button in sourceBtn) {
            val objectAnimator = ObjectAnimator.ofFloat(
                button, "x", button.x,
                targetView.x
            )
            objectAnimator.duration = 500
            objectAnimator.start()

            objectAnimator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    animation!!.removeListener(this)
                    fadeOutAnimation(button)

                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationRepeat(animation: Animator?) {}

            })

        }
    }

    private fun fadeOutAnimation(button: Button) {
        button.animate().alpha(0f).setDuration(500).withEndAction {
            pg_circle.visibility = View.VISIBLE

            if (!startedToFetch) {
                startedToFetch = true
                start()
            }

        }.start()
    }

    private fun disableAllButtons(viewArray: ArrayList<Button>) {
        for (button in viewArray) {
            button.isEnabled = false
        }
    }


    override fun onDestroy() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        super.onDestroy()
    }
}