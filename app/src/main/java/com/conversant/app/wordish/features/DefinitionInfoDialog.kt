package com.conversant.app.wordish.features

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.conversant.app.wordish.R
import com.conversant.app.wordish.WordishApp
import com.conversant.app.wordish.commons.gone
import com.conversant.app.wordish.data.room.WordDataSource
import com.conversant.app.wordish.data.xml.WordThemeDataXmlLoader
import com.conversant.app.wordish.features.gameplay.GamePlayViewModel
import kotlinx.android.synthetic.main.fragment_meanin_dialog.*
import javax.inject.Inject


const val DEFINITION_DIALOG_TAG = "definition_tag"
class DefinitionInfoDialog : DialogFragment() {

    @Inject
    lateinit var wordDataSource: WordDataSource

    @Inject
    lateinit var  soundPlayer:SoundPlayer

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
        wv_meaning.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun observeSelectedWord() {
        gameViewModel.selectedWord.observe(viewLifecycleOwner, {
            searchWordForWebview(it)
        })
    }

    private fun searchWordForWebview(it: String?) {
        if (it != null) {
            var parentDefinition = WordThemeDataXmlLoader.definitionsMap[it.uppercase()]

            val appendWordsList =
                WordThemeDataXmlLoader.appendWordsMap[it.uppercase()]

            if (appendWordsList != null && appendWordsList.isNotEmpty()) {
                appendWordsList.forEach { appendWord ->
                    val define = WordThemeDataXmlLoader.definitionsMap[appendWord]
                    parentDefinition += define
                }
            }
            showWebView(it, parentDefinition)
        }
    }

    private fun initClickListener() {
        et_search_view.addTextChangedListener { editable ->
            if (editable.toString().length >= 3){
                searchWordForWebview(editable.toString())
            }
        }
        iv_cancel.setOnClickListener {
            soundPlayer.stop()
            soundPlayer.play(SoundPlayer.Sound.Dismiss)
            dismiss()

        }
    }


    private fun showWebView(selectedWord: String, html: String?) {
        pg_view.gone()
        if (html != null && html.isNotEmpty()) {
            soundPlayer.play(SoundPlayer.Sound.SwipeCorrect)
            no_word.gone()
            wv_meaning.visibility = View.VISIBLE
            wv_meaning.loadDataWithBaseURL(
                null, html,
                "text/html", "utf-8", null
            )
        } else {
            no_word.visibility = View.VISIBLE
            wv_meaning.gone()
            val format =
                String.format(requireContext().getString(R.string.no_meaning), selectedWord)
            no_word.text = format
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}