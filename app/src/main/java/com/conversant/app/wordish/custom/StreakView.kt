package com.conversant.app.wordish.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import com.conversant.app.wordish.R
import com.conversant.app.wordish.commons.GridIndex
import com.conversant.app.wordish.commons.math.Vec2
import com.conversant.app.wordish.commons.orZero
import com.conversant.app.wordish.custom.TouchProcessor.OnTouchProcessed
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Created by abdularis on 20/06/17.
 *
 * Garis yang bisa didrag (coretan didalam word search game)
 */
class StreakView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class SnapType(var id: Int) {
        NONE(0), START_END(1), ALWAYS_SNAP(2);

        companion object {
            @JvmStatic
            fun fromId(id: Int): SnapType {
                for (t in values()) {
                    if (t.id == id) return t
                }
                throw IllegalArgumentException()
            }
        }
    }

    private lateinit var cellPlacmentMap: java.util.HashMap<Rect, String>

    private  var valueAnimator: ValueAnimator = ValueAnimator.ofInt(255, 0)
    private var path: Path
    private val rect: RectF = RectF()
    private var streakLineWidth = DEFAULT_STREAK_LINE_WIDTH_PIXEL
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint: Paint = Paint()
    private var gridId = -1
    private var snapToGridType: SnapType = SnapType.NONE
    private val touchProcessor: TouchProcessor = TouchProcessor(OnTouchProcessedListener(), 3.0f)
    private var lines: Stack<StreakLine> = Stack()
    private var interactionListener: OnInteractionListener? = null
    private var _enableOverrideStreakLineColor = false
    private var _overrideStreakLineColor = 0

    var grid: GridBehavior? = null
    var isInteractive = false
    var isRememberStreakLine = false

    var streakWidth: Int
        get() = streakLineWidth
        set(width) {
            streakLineWidth = width
            invalidate()
        }
    var isSnapToGrid: SnapType
        get() = snapToGridType
        set(snapToGrid) {
            check(!(snapToGridType != snapToGrid && gridId == -1 && grid == null)) { "setGrid() first to set the grid object!" }
            snapToGridType = snapToGrid
        }

    init {
        paint.color = Color.GREEN
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.StreakView, 0, 0)
            paint.color = a.getInteger(R.styleable.StreakView_streakColor, paint.color)
            streakLineWidth = a.getDimensionPixelSize(R.styleable.StreakView_streakWidth, streakLineWidth)
            gridId = a.getResourceId(R.styleable.StreakView_strekGrid, gridId)
            isSnapToGrid = SnapType.fromId(a.getInt(R.styleable.StreakView_snapToGrid, 0))
            isInteractive = a.getBoolean(R.styleable.StreakView_interactive, isInteractive)
            isRememberStreakLine = a.getBoolean(R.styleable.StreakView_rememberStreakLine, isRememberStreakLine)
            a.recycle()
        }
        linePaint.color = Color.RED
        linePaint.isAntiAlias = true
        linePaint.strokeWidth = 5f
        linePaint.style = Paint.Style.STROKE
        path = Path()
    }

    fun setEnableOverrideStreakLineColor(enableOverrideStreakLineColor: Boolean) {
        _enableOverrideStreakLineColor = enableOverrideStreakLineColor
    }

    fun setOverrideStreakLineColor(overrideStreakLineColor: Int) {
        _overrideStreakLineColor = overrideStreakLineColor
    }

    fun setOnInteractionListener(listener: OnInteractionListener?) {
        interactionListener = listener
    }

    private fun pushStreakLine(streakLine: StreakLine, snapToGrid: Boolean) {
        lines.push(streakLine)
        grid?.let {
            streakLine.start.x = it.getCenterColFromIndex(streakLine.startIndex.col).toFloat()
            streakLine.start.y = it.getCenterRowFromIndex(streakLine.startIndex.row).toFloat()
            streakLine.end.x = it.getCenterColFromIndex(streakLine.endIndex.col).toFloat()
            streakLine.end.y = it.getCenterRowFromIndex(streakLine.endIndex.row).toFloat()
        }
    }

    fun invalidateStreakLine() {
        for (streakLine in lines) {
            grid?.let {
                streakLine.start.x = it.getCenterColFromIndex(streakLine.startIndex.col).toFloat()
                streakLine.start.y = it.getCenterRowFromIndex(streakLine.startIndex.row).toFloat()
                streakLine.end.x = it.getCenterColFromIndex(streakLine.endIndex.col).toFloat()
                streakLine.end.y = it.getCenterRowFromIndex(streakLine.endIndex.row).toFloat()
            }
        }
    }

    fun addStreakLines(streakLines: List<StreakLine>, snapToGrid: Boolean) {
        for (line in streakLines) pushStreakLine(line, snapToGrid)
        invalidate()
    }

    fun addStreakLine(streakLine: StreakLine, snapToGrid: Boolean) {
        pushStreakLine(streakLine, snapToGrid)
        invalidate()
    }

    fun popStreakLine() {
        if (lines.isNotEmpty()) {
            lines.pop()
            invalidate()
        }
    }

    fun removeAllStreakLine() {
        lines.clear()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (isInteractive) touchProcessor.onTouchEvent(event) else super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (gridId != -1 && snapToGridType != SnapType.NONE) {
            grid = rootView.findViewById<View>(gridId) as GridBehavior
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        var measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (snapToGridType != SnapType.NONE) {
            grid?.let {
                measuredWidth = it.requiredWidth
                measuredHeight = it.requiredHeight
            }
        }
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    fun cellPlacementMap(cellPlacementMap:HashMap<Rect, String>){
        this.cellPlacmentMap = cellPlacementMap
    }

    override fun onDraw(canvas: Canvas) {
       // canvas.drawPath(path, linePaint)
    }

    private inner class OnTouchProcessedListener : OnTouchProcessed {
        override fun onDown(event: MotionEvent) {
            if (!isRememberStreakLine) {
                if (lines.isEmpty()) lines.push(StreakLine())
            } else {
                lines.push(StreakLine())
            }

            val line = lines.peek()
            val colIdx = grid?.getColIndex(event.x.toInt()).orZero()
            val rowIdx = grid?.getRowIndex(event.y.toInt()).orZero()

            val pair  = grid!!.getRowColumn(cellPlacmentMap, event.x.toInt(), event.y.toInt())


            if (pair != null) {
                line.startIndex.set(pair.first, pair.second)
            }

           // line.startIndex.set(rowIdx, colIdx)

            if (snapToGridType != SnapType.NONE) {
                val centerCol = grid?.getCenterColFromIndex(colIdx)?.toFloat() ?: 0f
                val centerRow = grid?.getCenterRowFromIndex(rowIdx)?.toFloat() ?: 0f

                line.start.set(centerCol, centerRow)
                line.end.set(event.x, event.y)
            } else {
                line.start.set(event.x, event.y)
                line.end.set(event.x, event.y)
            }
            path.moveTo(event.x, event.y)

            if (pair != null) {
                interactionListener?.onTouchBegin(line)
            }

           // invalidate()
        }

        override fun onUp(event: MotionEvent) {
            if (lines.isEmpty()) return
            val line = lines.peek()
            val colIdx = grid?.getColIndex(event.x.toInt()).orZero()
            val rowIdx = grid?.getRowIndex(event.y.toInt()).orZero()

            val pair  = grid!!.getRowColumn(cellPlacmentMap, event.x.toInt(), event.y.toInt())

            if (pair != null) {
                line.endIndex.set(pair.first, pair.second)
            }

            //line.endIndex.set(rowIdx, colIdx)

            if (snapToGridType != SnapType.NONE) {
                val centerCol = grid?.getCenterColFromIndex(colIdx)?.toFloat() ?: 0f
                val centerRow = grid?.getCenterRowFromIndex(rowIdx)?.toFloat() ?: 0f
                line.end.set(centerCol, centerRow)
            } else {
                line.end.set(event.x, event.y)
            }

            path.reset()

            interactionListener?.onTouchEnd(line)

          //  invalidate()
        }

        override fun onMove(event: MotionEvent) {
            if (lines.isEmpty()) return

            val line = lines.peek()
            val colIdx = grid?.getColIndex(event.x.toInt()).orZero()
            val rowIdx = grid?.getRowIndex(event.y.toInt()).orZero()

            val pair  = grid!!.getRowColumn(cellPlacmentMap, event.x.toInt(), event.y.toInt())
            if (pair != null) {
                line.endIndex.set(pair.first, pair.second)
            }

           // line.endIndex.set(rowIdx, colIdx)

            if (snapToGridType != SnapType.NONE) {
                val centerCol = grid?.getCenterColFromIndex(colIdx)?.toFloat() ?: 0f
                val centerRow = grid?.getCenterRowFromIndex(rowIdx)?.toFloat() ?: 0f
                line.end.set(centerCol, centerRow)
            } else {
                val halfWidth = streakLineWidth / 2
                val x = max(min(event.x, width - halfWidth.toFloat()), halfWidth.toFloat())
                val y = max(min(event.y, height - halfWidth.toFloat()), halfWidth.toFloat())
                line.end.set(event.x, event.y)
            }
            if (!path.isEmpty) {
                path.lineTo(event.x, event.y)
            } else{
                path.moveTo(event.x, event.y)
                path.lineTo(event.x, event.y)
            }
            startPaintAnimation()
            if (pair != null) {
                interactionListener?.onTouchDrag(line)
            }
            //invalidate()
        }
    }

    private fun startPaintAnimation() {
        if (!valueAnimator.isRunning) {
            valueAnimator.addUpdateListener {
                val i = it.animatedValue as Int
                linePaint.alpha = i
                invalidate()
                if (i == 0) {
                    path.reset()
                }
            }

            valueAnimator.duration = 2000
            valueAnimator.interpolator = AccelerateInterpolator()
            valueAnimator.start()
        }
    }

    //
    interface OnInteractionListener {
        fun onTouchBegin(streakLine: StreakLine)
        fun onTouchDrag(streakLine: StreakLine)
        fun onTouchEnd(streakLine: StreakLine)
    }

    class StreakLine {
        var start: Vec2 = Vec2()
        var end: Vec2 = Vec2()
        var startIndex: GridIndex = GridIndex(-1, -1)
        var endIndex: GridIndex = GridIndex(-1, -1)
        var color = Color.RED


        override fun toString(): String {
            return "start $start end $end startIndex $startIndex endIndex  $endIndex"
        }
    }

    companion object {
        private const val DEFAULT_STREAK_LINE_WIDTH_PIXEL = 26
    }
}