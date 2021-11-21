package com.conversant.app.wordish.features

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conversant.app.wordish.R
import com.conversant.app.wordish.WordishApp
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

    private var currenTime: Long = 0

    @Inject
    lateinit var wordDataSource: WordDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        (application as WordishApp).appComponent.inject(this)
        start()
    }

    private fun start() {
        currenTime = System.currentTimeMillis()
        disposable = wordDataSource.getWordsMayBe()
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
        loadDefinition()
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

    private fun loadDefinition() {
        lifecycleScope.launch {
              GameDatabase.loadDefinition(this@SplashScreenActivity)
        }
        val difference = (System.currentTimeMillis() - currenTime) / 1000
        Log.d("Difference..", "$difference")
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }
}