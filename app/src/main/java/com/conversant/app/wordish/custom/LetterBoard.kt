package com.conversant.app.wordish.custom

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import androidx.core.animation.doOnEnd
import com.conversant.app.wordish.R
import com.conversant.app.wordish.commons.Direction
import com.conversant.app.wordish.commons.Direction.Companion.fromLine
import com.conversant.app.wordish.commons.GridIndex
import com.conversant.app.wordish.commons.Util.getIndexLength
import com.conversant.app.wordish.custom.StreakView.OnInteractionListener
import com.conversant.app.wordish.custom.StreakView.SnapType.Companion.fromId
import com.conversant.app.wordish.custom.StreakView.StreakLine
import com.conversant.app.wordish.custom.layout.CenterLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by abdularis on 26/06/17.
 *
 * Compound view untuk wsp game
 * yang memiliki tiga layer yaitu
 * - GridLine sebagai background
 * - StreakView sebagai middleground jadi akan dirender diatas background
 * dan dibawah foreground
 * - LetterGrid sebagai foreground yang menampilkan letters (huruf-huruf)
 * yang akan dirender paling atas
 */

const val LEFT: Int = 0
const val RIGHT: Int = 100

class LetterBoard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : CenterLayout(context, attrs), Observer {

    private var shrinkFire: Boolean = false

    val gridLineBackground: GridLine = GridLine(context)
    val streakView: StreakView = StreakView(context)
    val letterGrid: LetterGrid = LetterGrid(context)
    private var initialized = false
    private lateinit var _dataAdapter: LetterGridDataAdapter

    var selectionListener: OnLetterSelectionListener? = null

    init {
        init(context, attrs)
    }

    override fun update(observable: Observable, arg: Any) {
        if (observable == _dataAdapter) {
            // ketika data grid berubah maka update row dan column count
            // dari grid line agar memiliki dimensi yang sama
            gridLineBackground.setColCount(_dataAdapter.getColCount())
            gridLineBackground.setRowCount(_dataAdapter.getRowCount())

            // ketika dimensi row dan column dari data grid berubah
            // maka harus di layout/dikalkulasikan kembali ukuran dari streak view
            streakView.invalidate()
            streakView.requestLayout()
        }
    }

    fun scale(scaleX: Float, scaleY: Float) {
        if (initialized) {
            gridLineBackground.gridWidth = (gridLineBackground.gridWidth * scaleX).toInt()
            gridLineBackground.gridHeight = (gridLineBackground.gridHeight * scaleY).toInt()
            // mGridLineBg.setLineWidth((int) (mGridLineBg.getLineWidth() * scaleX));
            letterGrid.gridWidth = (letterGrid.gridWidth * scaleX).toInt()
            letterGrid.gridHeight = (letterGrid.gridHeight * scaleY).toInt()
            letterGrid.letterSize = letterGrid.letterSize * scaleY
            streakView.streakWidth = (streakView.streakWidth * scaleY).toInt()

            // remove all views and re attach them, so this layout get re measure
            removeAllViews()
            attachAllViews()
            streakView.invalidateStreakLine()
        }
    }

    val gridColCount: Int
        get() = _dataAdapter.getColCount()
    val gridRowCount: Int
        get() = _dataAdapter.getRowCount()

    var dataAdapter: LetterGridDataAdapter
        get() = _dataAdapter
        set(dataAdapter) {
            if (dataAdapter != _dataAdapter) {
                _dataAdapter.deleteObserver(this)
                _dataAdapter = dataAdapter
                _dataAdapter.addObserver(this)
                letterGrid.dataAdapter = _dataAdapter
                gridLineBackground.setColCount(_dataAdapter.getColCount())
                gridLineBackground.setRowCount(_dataAdapter.getRowCount())

                startFireAnim()
            }
        }

    private fun startFireAnim() {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(100)
                letterGrid.startFireAnim()
            }
        }
    }

    fun addStreakLines(streakLines: List<StreakLine>) {
        streakView.addStreakLines(streakLines, false)
    }

    fun addStreakLine(streakLine: StreakLine?) {
        if (streakLine != null) streakView.addStreakLine(streakLine, true)
    }

    fun popStreakLine() {
        streakView.popStreakLine()
    }

    fun removeAllStreakLine() {
        streakView.removeAllStreakLine()
    }

    private fun setGridWidth(width: Int) {
        gridLineBackground.gridWidth = width
        letterGrid.gridWidth = width
    }

    private fun setGridHeight(height: Int) {
        gridLineBackground.gridHeight = height
        letterGrid.gridHeight = height
    }

    private fun setGridLineVisibility(visible: Boolean) {
        if (!visible) gridLineBackground.visibility = INVISIBLE else gridLineBackground.visibility =
            VISIBLE
    }

    private fun setGridLineColor(color: Int) {
        gridLineBackground.lineColor = color
    }

    private fun setGridLineWidth(width: Int) {
        gridLineBackground.lineWidth = width
    }

    private fun setLetterSize(size: Float) {
        letterGrid.letterSize = size
    }

    private fun setLetterColor(color: Int) {
        letterGrid.letterColor = color
    }

    private fun setStreakWidth(width: Int) {
        streakView.streakWidth = width
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        var gridWidth = DEFAULT_GRID_WIDTH_PIXEL
        var gridHeight = DEFAULT_GRID_HEIGHT_PIXEL
        var gridColCount = DEFAULT_GRID_SIZE
        var gridRowCount = DEFAULT_GRID_SIZE
        var lineColor = Color.GRAY
        var lineWidth = DEFAULT_LINE_WIDTH_PIXEL
        var letterSize = DEFAULT_LETTER_SIZE_PIXEL
        var letterColor = Color.GRAY
        var streakWidth = DEFAULT_STREAK_WIDTH_PIXEL
        var snapToGrid = 0
        var gridLineVisibility = true
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.LetterBoard, 0, 0)
            gridWidth = a.getDimensionPixelSize(R.styleable.LetterBoard_gridWidth, gridWidth)
            gridHeight = a.getDimensionPixelSize(R.styleable.LetterBoard_gridHeight, gridHeight)
            gridColCount = a.getInteger(R.styleable.LetterBoard_gridColumnCount, gridColCount)
            gridRowCount = a.getInteger(R.styleable.LetterBoard_gridRowCount, gridRowCount)
            lineColor = a.getColor(R.styleable.LetterBoard_lineColor, lineColor)
            lineWidth = a.getDimensionPixelSize(R.styleable.LetterBoard_lineWidth, lineWidth)
            letterSize = a.getDimension(R.styleable.LetterBoard_letterSize, letterSize)
            letterColor = a.getColor(R.styleable.LetterBoard_letterColor, letterColor)
            streakWidth = a.getDimensionPixelSize(R.styleable.LetterBoard_streakWidth, streakWidth)
            snapToGrid = a.getInteger(R.styleable.LetterBoard_snapToGrid, snapToGrid)
            gridLineVisibility =
                a.getBoolean(R.styleable.LetterBoard_gridLineVisibility, gridLineVisibility)
            setGridWidth(gridWidth)
            setGridHeight(gridHeight)
            setGridLineColor(lineColor)
            setGridLineWidth(lineWidth)
            setLetterSize(letterSize)
            setLetterColor(letterColor)
            setStreakWidth(streakWidth)
            setGridLineVisibility(gridLineVisibility)
            a.recycle()
        }
        _dataAdapter = SampleLetterGridDataAdapter(gridRowCount, gridColCount)
        gridLineBackground.setColCount(gridColCount)
        gridLineBackground.setRowCount(gridRowCount)
        streakView.grid = gridLineBackground
        streakView.isInteractive = true
        streakView.isRememberStreakLine = true
        streakView.isSnapToGrid = fromId(snapToGrid)
        streakView.setOnInteractionListener(StreakViewInteraction())
        attachAllViews()
        initialized = true
        scaleX = scaleX
        scaleY = scaleY

    }

    private fun attachAllViews() {
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(gridLineBackground, layoutParams)
        addView(streakView, layoutParams)
        addView(letterGrid, layoutParams)
    }


    fun shrinkFireWithWater(b: Boolean) {
        this.shrinkFire = b
    }

    private inner class StreakViewInteraction : OnInteractionListener {
        private fun getStringInRange(start: GridIndex, end: GridIndex): String {
            val dir = fromLine(start, end)
            if (dir === Direction.NONE) return ""
            val count = getIndexLength(start, end)
            val buff = CharArray(count)
            for (i in 0 until count) {
                buff[i] =
                    _dataAdapter.getLetter(start.row + dir.yOffset * i, start.col + dir.xOffset * i)
            }
            return String(buff)
        }

        override fun onTouchBegin(streakLine: StreakLine) {
            selectionListener?.let { it ->
                val idx = streakLine.startIndex
                val str = _dataAdapter.getLetter(idx.row, idx.col).toString()

                val hasFire = _dataAdapter.hasFire(idx.row, idx.col)
                if (hasFire && shrinkFire) { // shrink fire with water
                    dataAdapter.initWaterDrop(idx.row, idx.col, true)

                    CoroutineScope(Dispatchers.Main).launch {
                        var i = 0
                        while (i < 2000) {
                            delay(100)
                            i += 100;
                            if (i >= 1500) { // shrink the fire in the halfway
                                dataAdapter.initFire(idx.row, idx.col, false)
                            }
                            letterGrid.startWaterDropAnim()
                        }
                        dataAdapter.initWaterDrop(idx.row, idx.col, false)
                        letterGrid.invalidate()
                        it.onSelectionFireCell(streakLine, true)
                    }
                } else {

                    //explodeCells(streakLine)

                }

                it.onSelectionBegin(streakLine, str)
            }
        }

        override fun onTouchDrag(streakLine: StreakLine) {
            selectionListener?.onSelectionDrag(
                streakLine,
                getStringInRange(streakLine.startIndex, streakLine.endIndex)
            )
        }

        override fun onTouchEnd(streakLine: StreakLine) {
            selectionListener?.let {
                val str = getStringInRange(streakLine.startIndex, streakLine.endIndex)
                it.onSelectionEnd(streakLine, str)
            }
        }
    }

    fun explodeCells(streakLine: StreakLine) {
        val list = generateBombCells(streakLine)

        val valueAnimator =
            ValueAnimator.ofPropertyValuesHolder(*list.toTypedArray())

        val col = streakLine.startIndex.col
        val colEnd = if (col == 0) col + 1 else col

        valueAnimator.doOnEnd {
            //todo update cells with new data
        }
        valueAnimator.addUpdateListener { animIt ->

            var propertyNameIndex = 0
            for (i in 0..5) {
                for (j in colEnd - 1..colEnd) {

                    propertyNameIndex++
                    val value: Float = animIt.getAnimatedValue("$propertyNameIndex") as Float

                    letterGrid.bombCell[i][j].animate = true
                    letterGrid.bombCell[i][j].xAxix = value
                    letterGrid.explodeCells()
                }
            }
        }
        valueAnimator.duration = 500
        valueAnimator.start()
    }

    private fun generateBombCells(streakLine: StreakLine): List<PropertyValuesHolder> {
        val col = streakLine.startIndex.col

        val colEnd = if (col == 0) col + 1 else col
        var animationDir: Int

        val propertyValueList = arrayListOf<PropertyValuesHolder>()
        var propertyNameIndex = 0

        for (i in 0..5) {

            animationDir = 0

            for (j in colEnd - 1..colEnd) {

                propertyNameIndex++

                val xAxis = if (animationDir == 0) {
                    animationDir = 1
                    -100f
                } else {
                    width.toFloat()
                }

                val cellX = streakView.grid!!.getCenterColFromIndex(j).toFloat()
                val propertyAnim = PropertyValuesHolder.ofFloat("$propertyNameIndex", cellX, xAxis)
                propertyValueList.add(propertyAnim)

                letterGrid.bombCell[i][j].animate = true
                letterGrid.bombCell[i][j].xAxix = xAxis
            }
        }

        return propertyValueList

    }

    interface OnLetterSelectionListener {
        fun onSelectionBegin(streakLine: StreakLine, str: String)
        fun onSelectionDrag(streakLine: StreakLine, str: String)
        fun onSelectionEnd(streakLine: StreakLine, str: String)
        fun onSelectionFireCell(streakLine: StreakLine, hasFire: Boolean)
    }

    companion object {
        private const val DEFAULT_GRID_WIDTH_PIXEL = 50
        private const val DEFAULT_GRID_HEIGHT_PIXEL = 50
        private const val DEFAULT_GRID_SIZE = 8
        private const val DEFAULT_LINE_WIDTH_PIXEL = 2
        private const val DEFAULT_LETTER_SIZE_PIXEL = 32.0f
        private const val DEFAULT_STREAK_WIDTH_PIXEL = 35
    }
}