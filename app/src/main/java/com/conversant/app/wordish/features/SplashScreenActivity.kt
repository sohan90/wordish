package com.conversant.app.wordish.features

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conversant.app.wordish.R
import com.conversant.app.wordish.features.gameplay.GamePlayActivity
import com.conversant.app.wordish.features.gamethemeselector.ThemeSelectorActivity
import com.conversant.app.wordish.features.mainmenu.MainMenuActivity
import com.conversant.app.wordish.model.Difficulty
import com.conversant.app.wordish.model.GameMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        start()
    }

    private fun start(){
       lifecycleScope.launch {
           delay(3000)
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

//        startActivity(Intent(this, MainMenuActivity::class.java))
//        finish()
    }
}