package com.conversant.app.wordish.features.gameplay

import com.conversant.app.wordish.commons.Util
import com.conversant.app.wordish.commons.generator.StringListGridGenerator
import com.conversant.app.wordish.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min


class GameDataCreator {

    fun newGameData(words: MutableList<Word> = mutableListOf(),
                    rowCount: Int, colCount: Int,
                    name: String = "",
                    gameMode: GameMode
    ): GameData {
        Util.randomizeList(words)
        val maxIndex = min(256, words.size)
        val grid = Grid(rowCount, colCount)
        val usedWords = StringListGridGenerator().setGrid(words.subList(0, maxIndex), grid.array)
        val gameName = if (name.isNullOrEmpty()) "Puzzle ${getDate()}" else name

        return GameData(
            name = gameName,
            gameMode = gameMode,
            grid = grid
        ).apply {
            addUsedWords(buildUsedWordFromWord(usedWords))
            addAllWords(words)
        }
    }

    private fun buildUsedWordFromWord(words: List<Word>): List<UsedWord> {
        val usedWords: MutableList<UsedWord> = ArrayList()
        for (i in words.indices) {
            val word = words[i]
            val uw = UsedWord()
            uw.gameThemeId = word.gameThemeId
            uw.string = word.string
            usedWords.add(uw)
        }
        Util.randomizeList(usedWords)
        return usedWords
    }

    private fun getDate(): String {
        return SimpleDateFormat("HH.mm.ss", Locale.ENGLISH)
            .format(Date(System.currentTimeMillis()))
    }
}