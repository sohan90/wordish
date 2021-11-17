package com.conversant.app.wordish.features

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.conversant.app.wordish.R
import com.conversant.app.wordish.WordishApp
import com.conversant.app.wordish.commons.Util
import com.conversant.app.wordish.commons.gone
import com.conversant.app.wordish.data.room.WordDataSource
import com.conversant.app.wordish.features.gameplay.GamePlayViewModel
import kotlinx.android.synthetic.main.fragment_meanin_dialog.*
import kotlinx.coroutines.launch
import javax.inject.Inject


const val SETTING_ITEM_DIALOG_TAG = "definition_tag"

class SettingItemDialog : DialogFragment() {

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
        return inflater.inflate(R.layout.fragment_meanin_dialog, container, false)
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
        observeSelectedWord()
    }

    private fun initViews() {
        fl_webview_lyt.background = AppCompatResources.getDrawable(
            requireContext(),
            R.drawable.round_corner_webview_white
        )
        wv_meaning.setBackgroundColor(Color.TRANSPARENT)
        tv_title.text = getString(R.string.rules_amp_tips)
        tv_check.gone()
        et_search_view.gone()

    }

    private fun observeSelectedWord() {
        gameViewModel.htmlFileName.observe(viewLifecycleOwner, {
            if (it != null) {
                if (it.contains("credits"))  tv_title.text = getString(R.string.credits)
                lifecycleScope.launch {
                    val htmlString = Util.readStringFromAssets(requireContext(), it)
                    showWebView(htmlString)
                }
            }
        })
    }

    private fun initClickListener() {
        iv_cancel.setOnClickListener {
            soundPlayer.play(SoundPlayer.Sound.Dismiss)
            dismiss()
        }
    }


    private fun showWebView(html: String?) {
        pg_view.gone()
        if (html != null && html.isNotEmpty()) {
            soundPlayer.play(SoundPlayer.Sound.SwipeCorrect)
            no_word.gone()
            wv_meaning.visibility = View.VISIBLE
            wv_meaning.loadDataWithBaseURL(
                null, html,
                "text/html", "utf-8", null
            )
        }
    }
}