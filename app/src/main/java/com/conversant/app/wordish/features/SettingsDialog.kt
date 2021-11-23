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
import com.conversant.app.wordish.features.settings.Preferences
import kotlinx.android.synthetic.main.fragment_meanin_dialog.iv_cancel
import kotlinx.android.synthetic.main.fragment_settings_dialog.*
import kotlinx.coroutines.launch
import javax.inject.Inject


const val SETTINGS_DIALOG_TAG = "definition_tag"

class SettingsDialog : DialogFragment() {
    @Inject
    lateinit var wordDataSource: WordDataSource

    @Inject
    lateinit var preferences: Preferences

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
        return inflater.inflate(R.layout.fragment_settings_dialog, container, false)
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
        val heightPadding = resources.getDimension(R.dimen.setting_height_padding).toInt()

        dialog?.window?.setLayout(width - widhtPadding, height - heightPadding)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initClickListener()
    }

    private fun initViews() {
        switch_sound.isChecked = preferences.isMutedSound()
        switch_rules.isChecked = preferences.isRuleEnabled()

        switch_sound.setOnCheckedChangeListener { _, isChecked ->
            preferences.muteSound(isChecked)
        }
        switch_rules.setOnCheckedChangeListener { _, isChecked ->
            preferences.showRules(isChecked)
        }
    }

    private fun initClickListener() {
        tv_rules.setOnClickListener {
            openItemDialog(1)
        }
        tv_credits.setOnClickListener {
            openItemDialog(0)
        }

        tv_quit.setOnClickListener {
            soundPlayer.play(SoundPlayer.Sound.GameOver)
            lifecycleScope.launch {
                gameViewModel.quitGame()
                dismiss()
            }
        }

        tv_achievement.setOnClickListener {
            soundPlayer.play(SoundPlayer.Sound.Open)
            AchievmentDialog().show(childFragmentManager, ACHIEVMENT_ITEM_DIALOG_TAG)
        }

        iv_cancel.setOnClickListener {
            soundPlayer.stop()
            soundPlayer.play(SoundPlayer.Sound.Dismiss)
            dismiss()
        }
    }

    private fun openItemDialog(index: Int) {
       gameViewModel.openSettingItemDialog(index)
    }
}