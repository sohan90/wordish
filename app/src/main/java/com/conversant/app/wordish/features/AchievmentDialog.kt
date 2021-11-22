package com.conversant.app.wordish.features

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.conversant.app.wordish.R
import com.conversant.app.wordish.WordishApp
import com.conversant.app.wordish.data.room.WordDataSource
import com.conversant.app.wordish.features.gameplay.GamePlayViewModel
import kotlinx.android.synthetic.main.fragment_highscore_dialog.*
import kotlinx.android.synthetic.main.fragment_meanin_dialog.iv_cancel
import kotlinx.coroutines.launch
import javax.inject.Inject


const val ACHIEVMENT_ITEM_DIALOG_TAG = "definition_tag"

class AchievmentDialog : DialogFragment() {

    @Inject
    lateinit var wordDataSource: WordDataSource

    @Inject
    lateinit var soundPlayer: SoundPlayer

    private val gameViewModel: GamePlayViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (dialog != null && dialog?.window != null) {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE);
        }
        injectDependency()
        return inflater.inflate(R.layout.fragment_highscore_dialog, container, false)
    }

    private fun injectDependency() {
        (requireActivity().application as WordishApp).appComponent.inject(this)
    }

    override fun onStart() {
        super.onStart()
        setWidthHeight()
        setStyleForAnimation()
    }

    private fun setStyleForAnimation() {
        dialog!!.window!!.setWindowAnimations(
            R.style.DialogTheme_transparent
        );
    }

    private fun setWidthHeight() {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)

        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val widhtPadding = resources.getDimension(R.dimen.dialog_width_padding).toInt()
        val heightPadding = resources.getDimension(R.dimen.dialog_heigh_padding).toInt()

        dialog?.window?.setLayout(width - widhtPadding, height - heightPadding)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initClickListener()

    }

    private fun initViews() {
        lifecycleScope.launch {
            val topScore = gameViewModel.getTopScore()
            if (topScore != null) {
                val turnsStr = getString(R.string.highest_turns, topScore.turns)
                val wordsStr = getString(R.string.highest_words, topScore.words)
                val coinsStr = getString(R.string.highest_coins, topScore.coins)
                val wonStr = getString(R.string.won, topScore.won)

                tv_turns.text = turnsStr
                tv_words.text = wordsStr
                tv_coins.text = coinsStr
                tv_win.text = wonStr
            }
        }
    }

    private fun initClickListener() {
        iv_cancel.setOnClickListener {
            soundPlayer.stop()
            soundPlayer.play(SoundPlayer.Sound.Dismiss)
            dismiss()
        }
    }

}