package com.conversant.app.wordish.features

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.conversant.app.wordish.R
import com.conversant.app.wordish.WordSearchApp
import javax.inject.Inject


const val GAME_OVER_ITEM_DIALOG_TAG = "definition_tag"

class GameOverDialogFragment : DialogFragment() {

    @Inject
    lateinit var soundPlayer: SoundPlayer

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
        return inflater.inflate(R.layout.fragment_game_over_dialog, container, false)
    }

    private fun injectDependency() {
        (requireActivity().application as WordSearchApp).appComponent.inject(this)
    }

    override fun onStart() {
        super.onStart()
        setStyleForAnimation()
    }

    private fun setStyleForAnimation() {
        dialog!!.window!!.setWindowAnimations(
            R.style.DialogTheme_transparent)
    }

}