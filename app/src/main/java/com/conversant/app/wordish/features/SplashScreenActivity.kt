package com.conversant.app.wordish.features

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conversant.app.wordish.R
import com.conversant.app.wordish.WordSearchApp
import com.conversant.app.wordish.data.room.GameDatabase
import com.conversant.app.wordish.data.room.WordDataSource
import com.conversant.app.wordish.features.gameplay.GamePlayActivity
import com.conversant.app.wordish.model.Difficulty
import com.conversant.app.wordish.model.GameMode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import javax.inject.Inject

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var disposable: Disposable

    @Inject
    lateinit var wordDataSource: WordDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        (application as WordSearchApp).appComponent.inject(this)
        start()
    }

    private fun start() {
        disposable = wordDataSource.getWordsMayBe(6)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.isEmpty()) {
                    insertWord()
                } else {
                    nextScreen()
                }
            }
    }

    private fun insertWord() {
        lifecycleScope.launch {
            GameDatabase.prepopulateDatabase(this@SplashScreenActivity)
            nextScreen()
        }
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

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }
}