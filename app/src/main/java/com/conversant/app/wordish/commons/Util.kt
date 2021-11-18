package com.conversant.app.wordish.commons

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import com.conversant.app.wordish.custom.FireInfo
import com.conversant.app.wordish.custom.LetterGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
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

    var distributionCount = 1643315
    var letterBuckets = arrayListOf(
        125341,
        156814,
        223129,
        280000,
        468904,
        489615,
        534942,
        573298,
        718854,
        721638,
        736957,
        824529,
        871067,
        981510,
        1089191,
        1137539,
        1140235,
        1256584,
        1413051,
        1520736,
        1574753,
        1590639,
        1603594,
        1608537,
        1635344,
        1643315
    )


    fun getRandomColorWithAlpha(alpha: Int): Int {
        val r = randomInt % 256
        val g = randomInt % 256
        val b = randomInt % 256
        return Color.argb(alpha, r, g, b)
    }

    // ASCII A = 65 - Z = 90
    val randomChar: Char
        get() =// ASCII A = 65 - Z = 90
            getRandomChars()

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

    private fun getRandomChars(): Char {
        val pickNumber = sRand.nextInt(distributionCount)
        var idx = 0
        while (pickNumber > letterBuckets[idx]) {
            idx += 1
        }
        val character = 'A'.code + idx
        return character.toChar()
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

    /**
     * Saved db value string format will be like "[0,3],,[0,2],,[0,5]" with respect to fire count as "1,4,5"
     */
    fun fillFireInfoSavedValue(
        dbSavedFireInfo: String,
        fireCountString: String,
        fireInfoArray: Array<Array<FireInfo>>
    ) {
        if (!TextUtils.isEmpty(dbSavedFireInfo)) {
            val fireCount = fireCountString.split(",")
            val rowColArr = dbSavedFireInfo.split(",,")

            rowColArr.forEachIndexed { index, rowCol ->
                val frC = rowCol.replace("[", "")
                val srC = frC.replace("]", "")

                val row = srC.split(",")[0].toInt()
                val col = srC.split(",")[1].toInt()

                fireInfoArray[row][col].hasFire = true
                fireInfoArray[row][col].fireCount = fireCount[index].toInt()
            }
        }
    }

    fun fillCompleteCellInfoFromDb(dbSaveCompleteCell: String, gridCompleteCellArr: Array<BooleanArray>){
        if (!TextUtils.isEmpty(dbSaveCompleteCell)) {
            val rowColArr = dbSaveCompleteCell.split(",,")
            rowColArr.forEachIndexed { _, rowCol ->
                val frC = rowCol.replace("[", "")
                val srC = frC.replace("]", "")

                val row = srC.split(",")[0].toInt()
                val col = srC.split(",")[1].toInt()

                gridCompleteCellArr[row][col] = true

            }
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
        fireList: Array<Array<FireInfo>>
    ) {
        for (pair in list) {

            adapterData[pair.first][pair.second] = randomChar
            completedCell[pair.first][pair.second] = true
            val fireInfo = fireList[pair.first][pair.second]
            fireInfo.fireCount = 0
            fireInfo.hasFire = false
        }
    }

    fun winGame(completedCell: Array<BooleanArray>){
        for (row in 0..5){
           for (col in 0..5){
               completedCell[row][col] = true
           }
        }
    }

    fun looseGame(fireList: Array<Array<FireInfo>>){
        for (row in 0..5){
            for (col in 0..5){
                fireList[row][col].hasFire = true
            }
        }
    }

    fun IntProgression.size(): Int {
        var size = 0
        for (i in this) {
            size++
        }
        return size
    }

    fun animateReplaceWordCell(
        list: MutableList<Pair<Int, Int>>,
        letterGrid: LetterGrid, onAnimationEndCallBack: () -> Unit
    ) {

        val selectionCellList = ArrayList<Pair<Int, Int>>()
        selectionCellList.addAll(list)

        val propertyValueList = arrayListOf<PropertyValuesHolder>()

        val propertyAnim = PropertyValuesHolder.ofFloat("$1", -100f, 0f)
        propertyValueList.add(propertyAnim)

        val valueAnimator =
            ValueAnimator.ofPropertyValuesHolder(propertyAnim)

        valueAnimator.addUpdateListener {

            onAnimationEndCallBack()
            for (i in selectionCellList.indices) {

                val pair = selectionCellList[i]
                val value: Float = it.getAnimatedValue("$1") as Float
                letterGrid.bombCell[pair.first][pair.second].replaceWordAnimate = true
                letterGrid.bombCell[pair.first][pair.second].cellYaxis = value
            }
        }

        valueAnimator.doOnEnd {
            for (pair in selectionCellList) {
                letterGrid.bombCell[pair.first][pair.second].replaceWordAnimate = false
            }
        }

        valueAnimator.duration = 200
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.start()
    }

    suspend fun readStringFromAssets(context: Context, folderName: String) =
        withContext(Dispatchers.IO) {
            val assetsManager = context.assets
            val inputStream = assetsManager.open(folderName)
            try {
                val length: Int = inputStream.available()
                val data = ByteArray(length)
                inputStream.read(data)
                String(data)
            } catch (e: IOException) {
                e.printStackTrace()
                ""
            }
        }

}