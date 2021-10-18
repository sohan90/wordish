package com.conversant.app.wordish.custom

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.conversant.app.wordish.R
import com.conversant.app.wordish.commons.orZero
import java.util.*

/**
 * Created by abdularis on 22/06/17.
 *
 * Render grid of letters
 */
class LetterGrid @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    var letterBoardListener: LetterBoard.OnLetterSelectionListener? = null
) : GridBehavior(context, attrs), Observer {

    var bombCell = Array(6) { Array(6) { BombCell(0f, false) } }

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val roundCornerPaint: Paint = Paint()

    private val highlightPaint = Paint()

    private val backgroundColor = Paint()

    private val fireGif = mutableListOf<Bitmap>()

    private val waterGif = mutableListOf<Bitmap>()

    private var fireGifIndex = 0

    private var waterGifIndex = 0


    private val waterDropBitmap = AppCompatResources.getDrawable(
        context,
        R.drawable.ic_baseline_water_drop_24
    )?.toBitmap()

    private var rect = Rect(0, 0, 0, 0)

    private val charBounds: Rect = Rect()

    private var gridDataAdapter: LetterGridDataAdapter? = SampleLetterGridDataAdapter(
        DEFAULT_LETTER_GRID_SAMPLE_SIZE,
        DEFAULT_LETTER_GRID_SAMPLE_SIZE
    )

    init {
        init(context, attrs)
        initPaintObject()
    }

    private fun initPaintObject() {
        backgroundColor.color = Color.BLACK
        backgroundColor.style = Paint.Style.FILL
        backgroundColor.strokeWidth = 15f
        backgroundColor.isAntiAlias = true

        highlightPaint.color = Color.RED
        highlightPaint.style = Paint.Style.FILL
        highlightPaint.isAntiAlias = true

        roundCornerPaint.color = Color.GREEN
        roundCornerPaint.style = Paint.Style.STROKE
        roundCornerPaint.strokeWidth = 5f
        roundCornerPaint.isAntiAlias = true

    }

    var letterSize: Float
        get() = paint.textSize
        set(letterSize) {
            paint.textSize = letterSize
            invalidate()
        }

    var letterColor: Int
        get() = paint.color
        set(color) {
            paint.color = color
            invalidate()
        }

    var dataAdapter: LetterGridDataAdapter?
        get() = gridDataAdapter
        set(newDataAdapter) {
            requireNotNull(newDataAdapter) { "Data Adapter can't be null" }
            if (newDataAdapter !== gridDataAdapter) {
                gridDataAdapter?.deleteObserver(this)
                gridDataAdapter = newDataAdapter
                gridDataAdapter?.addObserver(this)
                invalidate()
                requestLayout()
            }
        }

    fun explodeCells() {
        invalidate()
    }

    fun setListener(selectionListener: LetterBoard.OnLetterSelectionListener) {
        letterBoardListener = selectionListener
    }

    fun startFireAnim() {
        fireGifIndex += 1
        invalidate()
    }

    fun startWaterDropAnim() {
        waterGifIndex += 1
        invalidate()
    }

    override fun getColCount(): Int {
        return gridDataAdapter?.getColCount().orZero()
    }

    override fun getRowCount(): Int {
        return gridDataAdapter?.getRowCount().orZero()
    }

    override fun setColCount(colCount: Int) {
        // do nothing
    }

    override fun setRowCount(rowCount: Int) {
        // do nothing
    }

    override fun update(o: Observable, arg: Any) {
        invalidate()
        requestLayout()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDraw(canvas: Canvas) {
        val gridColCount = getColCount()
        val gridRowCount = getRowCount()
        val halfWidth = gridWidth / 2
        val halfHeight = gridHeight / 2
        var x: Int
        var y = halfHeight + paddingTop

        var waterY = 0
        var waterX: Int

        var cornerX = 0
        var cornerY = 0

        // first draw the background cell and then draw the other stuff on top of that
        for (i in 0 until gridRowCount) {
            cornerX = 0
            for (j in 0 until gridColCount) {
                canvas.drawRoundRect(
                    cornerX.toFloat(), cornerY.toFloat(), cornerX + gridWidth.toFloat(),
                    cornerY + gridHeight.toFloat(), 30f, 30f, backgroundColor)
                cornerX += gridWidth
            }
            cornerY += gridHeight
        }

        cornerY = 0

        // iterate and render all letters found in grid data adapter
        for (i in 0 until gridRowCount) {
            x = halfWidth + paddingLeft
            waterX = 0
            cornerX = 0

            for (j in 0 until gridColCount) {

                val letter = gridDataAdapter?.getLetter(i, j)
                paint.getTextBounds(letter.toString(), 0, 1, charBounds)

                //highlight or selected cell color
                val selected = gridDataAdapter?.highLight(i, j)
                if (selected!!) {
                    highlightPaint.color = Color.RED
                    canvas.drawRoundRect(
                        cornerX.toFloat(), cornerY.toFloat(), cornerX + gridWidth.toFloat(),
                        cornerY + gridHeight.toFloat(), 30f, 30f, highlightPaint
                    )
                }

                //border color
                roundCornerPaint.setShadowLayer(20f, 0f, 0f, Color.GRAY)
                canvas.drawRoundRect(
                    cornerX.toFloat(), cornerY.toFloat(), cornerX + gridWidth.toFloat(),
                    cornerY + gridHeight.toFloat(), 30f, 30f, roundCornerPaint
                )

                if (bombCell[i][j].animate) {
                    canvas.drawText(
                        letter.toString(),
                        bombCell[i][j].xAxix, y - charBounds.exactCenterY(), paint
                    )
                } else {
                    canvas.drawText(
                        letter.toString(),
                        (x - charBounds.exactCenterX()), (y - charBounds.exactCenterY()) , paint
                    )
                }

                if (gridDataAdapter?.hasFire(i, j) == true) {
                    rect.left = x
                    rect.top = y - 20
                    rect.right = rect.left + 100
                    rect.bottom = rect.top + 100
                    canvas.drawBitmap(fireGif[fireGifIndex % 4], null, rect, paint)
                }

                if (gridDataAdapter?.hasWaterDrop(i, j) == true) {
                    rect.left = waterX + (halfWidth - waterDropBitmap!!.width)
                    rect.top = waterY
                    rect.right = rect.left + 150
                    rect.bottom = rect.top + 200
                    canvas.drawBitmap(waterGif[waterGifIndex % 5], null, rect, paint)
                }

                x += gridWidth
                waterX += gridWidth
                cornerX += gridWidth
            }

            y += gridHeight
            waterY += gridHeight
            cornerY += gridHeight

        }
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        paint.textSize = DEFAULT_TEXT_SIZE
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.LetterGrid, 0, 0)
            paint.textSize = a.getDimension(R.styleable.LetterGrid_letterSize, paint.textSize)
            paint.color = a.getColor(R.styleable.LetterGrid_letterColor, Color.GRAY)
            a.recycle()
        }

        fireGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_0_delay))
        fireGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_1_delay))
        fireGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_2_delay))
        fireGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_3_delay))

        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_water_01_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_water_02_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_water_03_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_water_04_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_water_05_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_water_06_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_water_07_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_water_08_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_water_09_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_water_10_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_water_11_delay))

    }

    companion object {
        private const val DEFAULT_LETTER_GRID_SAMPLE_SIZE = 6
        private const val DEFAULT_TEXT_SIZE = 32f
    }
}