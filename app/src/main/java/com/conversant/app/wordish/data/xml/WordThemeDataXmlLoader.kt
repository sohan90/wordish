package com.conversant.app.wordish.data.xml

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.conversant.app.wordish.model.GameTheme
import com.conversant.app.wordish.model.Word
import org.xml.sax.InputSource
import java.io.IOException
import javax.xml.parsers.SAXParserFactory


class WordThemeDataXmlLoader(val context: Context) {
    private val assetManager: AssetManager = context.assets
    private var _words: List<Word>? = null
    private var _gameThemes: List<GameTheme>? = null

    val words: List<Word>
        get() {
            if (_words == null) {
                loadData()
            }
            return _words ?: emptyList()
        }

    val gameThemes: List<GameTheme>
        get() {
            if (_gameThemes == null) {
                loadData()
            }
            return _gameThemes ?: emptyList()
        }

    fun release() {
        _words = null
        _gameThemes = null
    }

    private fun loadData() {
        try {
            val reader = SAXParserFactory.newInstance().newSAXParser().xmlReader
            val handler = SaxWordThemeHandler()
            reader.contentHandler = handler

            val fileName = assetManager.list(ASSET_BASE_FOLDER)!![0]
            reader.parse(getInputSource("$ASSET_BASE_FOLDER/$fileName"))

            _words = handler.words
            _gameThemes = handler.gameThemes

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseXmlString(chunk:String?) {
        var inDef = false
        var inWord = false
        var inAppends = false
        var definition = ""
        var word = ""
        val appendsList = mutableListOf<String>()


        if (chunk != null) {
            Log.d("XML...", "XML IS NOT Null")
            val xmlList = chunk.split("\n")

            for (s in xmlList) {
                when (s) {
                    "<entry>", "</entry>" -> { }

                    "<word>" -> inWord = true

                    "</word>" -> inWord = false

                    "<append>" -> inAppends = true

                    "</append>" -> inAppends = false

                    "<definitions>" -> inDef = true

                    "</definitions>" -> {
                        inDef = false
                        definitionsMap[word.uppercase()] = definition
                        definition = ""
                        if (appendsList.size > 0) {
                            appendWordsMap[word.uppercase()] = listOf(*appendsList.toTypedArray())
                            appendsList.clear()
                        }
                    }

                    else -> {
                        when {
                            inDef -> {
                                definition += s
                            }
                            inWord -> {
                                word = s
                            }
                            inAppends -> {
                                appendsList.add(s.uppercase())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getXmlStringForDefinition(): String? {
        val xmlString: String
        val fileName = assetManager.list(ASSET_BASE_FOLDER)!![1]
        val inputStream = assetManager.open("$ASSET_BASE_FOLDER/$fileName")
        try {
            val length: Int = inputStream.available()
            val data = ByteArray(length)
            inputStream.read(data)
            xmlString = String(data)
            return xmlString
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null

    }

    fun  loadDefinition(){
       val xmlString =  getXmlStringForDefinition()
        parseXmlString(xmlString)
    }

    @Throws(IOException::class)
    private fun getInputSource(fileName: String): InputSource {
        return InputSource(assetManager.open(fileName))
    }

    companion object {
         private const val ASSET_BASE_FOLDER = "dictionary"
         var definitionsMap = mutableMapOf<String, String>()
         val appendWordsMap = mutableMapOf<String, List<String>>()
    }


}