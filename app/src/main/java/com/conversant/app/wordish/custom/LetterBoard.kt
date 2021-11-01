package com.conversant.app.wordish.custom

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.animation.BounceInterpolator
import androidx.core.animation.doOnEnd
import com.conversant.app.wordish.R
import com.conversant.app.wordish.commons.GridIndex
import com.conversant.app.wordish.commons.Util
import com.conversant.app.wordish.custom.StreakView.OnInteractionListener
import com.conversant.app.wordish.custom.StreakView.SnapType.Companion.fromId
import com.conversant.app.wordish.custom.StreakView.StreakLine
import com.conversant.app.wordish.custom.layout.CenterLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.LinkedHashMap

class LetterBoard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : CenterLayout(context, attrs), Observer {

    private var isSameTile: Boolean = false
    private var hashMap = LinkedHashMap<String, Int>()
    private var endGridIndex: GridIndex = GridIndex(-1, -1)
    private var shrinkFire: Boolean = false

    private val gridLineBackground: GridLine = GridLine(context)
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

            gridLineBackground.setColCount(_dataAdapter.getColCount())
            gridLineBackground.setRowCount(_dataAdapter.getRowCount())

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
        //addView(gridLineBackground, layoutParams)
        addView(streakView, layoutParams)
        addView(letterGrid, layoutParams)
    }


    fun shrinkFireWithWater(b: Boolean) {
        this.shrinkFire = b
    }

    private inner class StreakViewInteraction : OnInteractionListener {
        private fun getStringInRange(start: GridIndex, end: GridIndex): String {
            if (end.row == endGridIndex.row && end.col == endGridIndex.col) {
                isSameTile = true
            } else {
                isSameTile = false
                endGridIndex = end.copy()
                selectionListener?.onSelectionWord()
            }

            val filterMap: Map<String, Int>?

            val hashKey = "${end.row},${end.col}"

            if (hashMap.containsKey(hashKey)) {
                val value = hashMap[hashKey]!!
                val inc = value + 1
                hashMap[hashKey] = inc
            } else {
                hashMap[hashKey] = 1
            }

            filterMap = hashMap.filter { it.value > 3}
            var buffCount = 0
            val buff = CharArray(filterMap.size)

            filterMap.forEach {
                val rowCol = it.key.split(",")
                if (buffCount == 0) {
                    buff[buffCount] = _dataAdapter.getLetter(start.row, start.col)
                }
                val letter = _dataAdapter.getLetter(rowCol[0].toInt(), rowCol[1].toInt())
                dataAdapter.initHighlight(rowCol[0].toInt(), rowCol[1].toInt(), true)
                buff[buffCount] = letter
                buffCount++
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
                    it.onSelectionFireCell(streakLine, true)

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
                        it.onSelectionFireCell(streakLine, false)
                    }
                }

                if(!shrinkFire) {
                    it.onSelectionBegin(streakLine, str)
                }

                //reset the highlight tiles on release
                val hashKey = "${idx.row},${idx.col}"
                hashMap[hashKey] = 1
                dataAdapter.initHighlight(idx.row, idx.col, true)
            }
        }

        override fun onTouchDrag(streakLine: StreakLine) {
            if(!shrinkFire) {
                selectionListener?.onSelectionDrag(
                    streakLine,
                    getStringInRange(streakLine.startIndex, streakLine.endIndex)
                )
            }

        }

        override fun onTouchEnd(streakLine: StreakLine) {
            if (!shrinkFire) {
                selectionListener?.let {
                    val str = getStringInRange(streakLine.startIndex, streakLine.endIndex)
                    it.onSelectionEnd(streakLine, str)
                }
            }


            hashMap.clear()
            endGridIndex.row = -1
            endGridIndex.col = -1
        }
    }

    fun explodeCells(streakLine: StreakLine) {
        val list = generateBombCells(streakLine)

        val valueAnimator =
            ValueAnimator.ofPropertyValuesHolder(*list.toTypedArray())

        valueAnimator.doOnEnd {}
        valueAnimator.addUpdateListener { animIt ->

            var propertyNameIndex = 0
            var propertyCellIndex = 100

            val rowProgression = Util.getRowProgression(streakLine.startIndex.row)

            val colProgression = Util.getColProgression(streakLine.startIndex.col)

            for (i in rowProgression) {

                for (j in colProgression) {

                    propertyNameIndex++
                    propertyCellIndex--

                    val value: Float = animIt.getAnimatedValue("$propertyNameIndex") as Float
                    val value2: Float = animIt.getAnimatedValue("$propertyCellIndex") as Float

                    letterGrid.bombCell[i][j].animate = true
                    letterGrid.bombCell[i][j].xAxix = value
                    letterGrid.bombCell[i][j].cellYaxis = value2
                    letterGrid.explodeCells()
                }
            }
        }
        valueAnimator.duration = 500
        valueAnimator.interpolator = BounceInterpolator()
        valueAnimator.start()
    }


    private fun generateBombCells(streakLine: StreakLine): List<PropertyValuesHolder> {
        var animationDir: Int

        val propertyValueList = arrayListOf<PropertyValuesHolder>()
        var propertyNameIndex = 0
        var propertyCellIndex = 100

        val rowProgression = Util.getRowProgression(streakLine.startIndex.row)
        val colProgression = Util.getColProgression(streakLine.startIndex.col)

        for (i in rowProgression) {

            animationDir = 0

            for (j in colProgression) {

                propertyNameIndex++
                propertyCellIndex --

                val xAxis = if (animationDir == 0) {
                    animationDir = 1
                    -100f
                } else {
                  width.toFloat()
                }

                val cellX = streakView.grid!!.getCenterColFromIndex(j).toFloat()
                val propertyAnim = PropertyValuesHolder.ofFloat("$propertyNameIndex", cellX, xAxis)
                val propertyAnim2 = PropertyValuesHolder.ofFloat("$propertyCellIndex", -100f, 0f)
                propertyValueList.add(propertyAnim)
                propertyValueList.add(propertyAnim2)

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
        fun onSelectionWord()
    }

    companion object {
        private const val DEFAULT_GRID_WIDTH_PIXEL = 50
        private const val DEFAULT_GRID_HEIGHT_PIXEL = 50
        private const val DEFAULT_GRID_SIZE = 6
        private const val DEFAULT_LINE_WIDTH_PIXEL = 2
        private const val DEFAULT_LETTER_SIZE_PIXEL = 32.0f
        private const val DEFAULT_STREAK_WIDTH_PIXEL = 35
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

    }
}