package com.conversant.app.wordish.commons

import android.graphics.Color
import com.conversant.app.wordish.custom.LetterGrid
import com.conversant.app.wordish.custom.StreakView
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Created by abdularis on 23/06/17.
 */
object Util {
    const val NULL_CHAR = '\u0000'
    private val sRand = Random()

    fun getRandomColorWithAlpha(alpha: Int): Int {
        val r = randomInt % 256
        val g = randomInt % 256
        val b = randomInt % 256
        return Color.argb(alpha, r, g, b)
    }

    // ASCII A = 65 - Z = 90
    val randomChar: Char
        get() =// ASCII A = 65 - Z = 90
            getRandomIntRange(65, 90).toChar()

    /**
     * generate random integer between min and max (inclusive)
     * example: min = 5, max = 7 output would be (5, 6, 7)
     *
     * @param min minimum integer number to be generated
     * @param max maximum integer number to be generated (inclusive)
     * @return integer between min - max
     */
    private fun getRandomIntRange(min: Int, max: Int): Int {
        return min + randomInt % (max - min + 1)
    }

    @JvmStatic
    val randomInt: Int
        get() = abs(sRand.nextInt())

    @JvmStatic
    fun getIndexLength(start: GridIndex, end: GridIndex): Int {
        val x = abs(start.col - end.col)
        val y = abs(start.row - end.row)
        return max(x, y) + 1
    }

    fun <T> randomizeList(list: MutableList<T>) {
        val count = list.size
        for (i in 0 until count) {
            val randIdx = getRandomIntRange(min(i + 1, count - 1), count - 1)
            val temp = list[randIdx]
            list[randIdx] = list[i]
            list[i] = temp
        }
    }

    fun getReverseString(str: String): String {
        val out = StringBuilder()
        for (i in str.length - 1 downTo 0) out.append(str[i])
        return out.toString()
    }

    /**
     * Isi slot / element yang masih kosong dengan karakter acak
     *
     */
    @JvmStatic
    fun fillNullCharWidthRandom(gridArr: Array<CharArray>) {
        for (i in gridArr.indices) {
            for (j in gridArr[i].indices) {
                if (gridArr[i][j] == NULL_CHAR) gridArr[i][j] = randomChar
            }
        }
    }

    fun replaceNewWordForCorrectWord(
        streakView: StreakView.StreakLine,
        adapterData: Array<CharArray>,
        completedCell: Array<BooleanArray>,
        fireList: Array<BooleanArray>
    ) {

        val startRow = streakView.startIndex.row
        val endRow = streakView.endIndex.row
        val startCol = streakView.startIndex.col
        val endCol = streakView.endIndex.col

        val rowProgression: IntProgression = if (startRow > endRow) {
            startRow.downTo(endRow)
        } else {
            startRow..endRow
        }

        val colProgression: IntProgression = if (startCol > endCol) {
            startCol.downTo(endCol)
        } else {
            startCol..endCol
        }


        val rowIterator = rowProgression.iterator()
        val colIterator = colProgression.iterator()

        val rowSize = rowProgression.size()
        val colSize = colProgression.size()

        val maxSize = rowSize.coerceAtLeast(colSize)

        var count = 0
        var row = 0
        var col = 0

        while (count < maxSize) {
            row = if (rowIterator.hasNext()) rowIterator.nextInt() else row
            col = if (colIterator.hasNext()) colIterator.nextInt() else col
            adapterData[row][col] = randomChar
            completedCell[row][col] = true
            fireList[row][col] = false
            count++
        }
    }

    fun getRowProgression(row: Int): IntProgression {
        var startRow = row - 1
        var endRow = row + 1

        if (row == 0) {
            startRow = row
        } else if (row == 5) {
            endRow = row
        }

        return startRow..endRow
    }

    fun getColProgression(col: Int): IntProgression {
        var startCol = col - 1
        var endCol = col + 1

        if (col == 0) {
            startCol = col
        } else if (col == 5) {
            endCol = col
        }
        return startCol..endCol
    }

    fun replaceNewWord(
        list: List<Pair<Int, Int>>, adapterData: Array<CharArray>,
        completedCell: Array<BooleanArray>,
        fireList: Array<BooleanArray>
    ) {
        for (pair in list) {

            adapterData[pair.first][pair.second] = randomChar
            completedCell[pair.first][pair.second] = true
            fireList[pair.first][pair.second] = false
        }
    }

    fun replaceNewWordForExplode(col: Int, adapterData: Array<CharArray>, letterGrid: LetterGrid) {
        val colEnd = if (col == 0) col + 1 else col
        for (i in 0..5) {
            for (j in colEnd - 1..colEnd) {
                letterGrid.bombCell[i][j].animate = false
                letterGrid.bombCell[i][j].xAxix = 0f
                adapterData[i][j] = randomChar
            }
        }
    }

    fun getNewCharList(): List<Char> {
        val list = mutableListOf<Char>()
        for (i in 0..5) {
            list.add(randomChar)
        }
        return list
    }

    fun replaceNewWordForRow(
        list: List<Char>,
        col: Int,
        letterGrid: LetterGrid,
        adapterData: Array<CharArray>
    ) {
        for (i in list.indices) {
            letterGrid.bombCell[i][col].animate = false
            letterGrid.bombCell[i][col].xAxix = 0f
            adapterData[i][col] = list[i]
        }
    }

    fun IntProgression.size(): Int {
        var size = 0
        for (i in this) {
            size++
        }
        return size
    }
}