package com.conversant.app.wordish.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.conversant.app.wordish.R
import com.conversant.app.wordish.commons.orZero
import java.util.*
import kotlin.math.abs


const val SPREAD_FIRE_RATIO = 1
const val FIRE_RATIO_2 = 3
const val FIRE_RATIO_3 = 2
const val FIRE_RATIO_4 = 1
const val START_FIRE_RATIO = 4

class LetterGrid @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    var letterBoardListener: LetterBoard.OnLetterSelectionListener? = null
) : GridBehavior(context, attrs), Observer {

    private var spotFireImage: Boolean = false

    private var startCalculateCellSize: Boolean = false


    private val roundCornerSize = resources.getDimension(R.dimen.round_corner).toInt()

    private val borderSpace = resources.getDimension(R.dimen.border_space).toInt()

    private val letterSpace = borderSpace / 2

    var bombCell = Array(6) { Array(6) { BombCell(0f, 0f, false, false) } }

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val greyBorderColor: Paint = Paint()

    private val greenBorderColor: Paint = Paint()

    private val redBckColor = Paint()

    private val greyWhiteBckColor = Paint()

    private val fireGif = mutableListOf<Bitmap>()

    private val waterGif = mutableListOf<Bitmap>()

    private var fireGifIndex = 0

    private var waterGifIndex = 0


    private val waterDropBitmap = AppCompatResources.getDrawable(
        context,
        R.drawable.ic_baseline_water_drop_24
    )?.toBitmap()

    private var rect = Rect()

    private var cellRect = Rect()

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
        greyWhiteBckColor.color = Color.parseColor("#D8BD96")//Color.BLACK //
        greyWhiteBckColor.style = Paint.Style.FILL
        greyWhiteBckColor.strokeWidth = 15f
        greyWhiteBckColor.isAntiAlias = true

        redBckColor.color = Color.RED // red background
        redBckColor.style = Paint.Style.FILL
        redBckColor.isAntiAlias = true

        greyBorderColor.color = Color.parseColor("#800000")//grey border
        greyBorderColor.style = Paint.Style.STROKE
        greyBorderColor.strokeWidth = 7f
        greyBorderColor.isAntiAlias = true

        greenBorderColor.color = Color.GREEN //green border
        greenBorderColor.style = Paint.Style.STROKE
        greenBorderColor.strokeWidth = 7f
        greenBorderColor.isAntiAlias = true


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

    var startSizeCellCalc: Boolean
        get() = startCalculateCellSize
        set(start) {
            startCalculateCellSize = start
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

    fun spotFire() {
        spotFireImage = true
        //invalidate()
    }

    fun releaseFire() {
        spotFireImage = false
        //invalidate()
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

                val gameOverAnimate = gridDataAdapter!!.gameOverTileAnimation(i, j)
                if (gameOverAnimate) {
                    greyWhiteBckColor.alpha = 0
                    greenBorderColor.alpha = 0
                    greyBorderColor.alpha = 0

                } else {
                    greyWhiteBckColor.alpha = 255
                    greenBorderColor.alpha = 255
                    greyBorderColor.alpha = 255
                }

                if (!bombCell[i][j].animate && !bombCell[i][j].replaceWordAnimate) {
                    canvas.drawRoundRect(
                        cornerX.toFloat(),
                        cornerY.toFloat(),
                        cornerX + gridWidth.toFloat() - borderSpace,
                        cornerY + gridHeight.toFloat() - borderSpace,
                        roundCornerSize.toFloat(),
                        roundCornerSize.toFloat(),
                        greyWhiteBckColor
                    )

                    if (startCalculateCellSize) {
                        cellRect.setEmpty()
                        cellRect.left = cornerX
                        cellRect.top = cornerY
                        cellRect.right = cornerX + gridWidth - abs(borderSpace + 20)
                        cellRect.bottom = cornerY + gridHeight - abs(borderSpace + 20)
                        letterBoardListener?.onCellPlacementLaidOut(cellRect, "$i,$j")
                    }
                }

                cornerX += gridWidth
            }
            cornerY += gridHeight
        }

        startCalculateCellSize = false
        cornerY = 0

        // iterate and render all letters found in grid data adapter
        for (i in 0 until gridRowCount) {
            x = halfWidth + paddingLeft
            waterX = 0
            cornerX = 0

            for (j in 0 until gridColCount) {

                val gameOverAnimate = gridDataAdapter!!.gameOverTileAnimation(i, j)
                if (gameOverAnimate) {
                    greyWhiteBckColor.alpha = 0
                    greenBorderColor.alpha = 0
                    greyBorderColor.alpha = 0

                } else {
                    greyWhiteBckColor.alpha = 255
                    greenBorderColor.alpha = 255
                    greyBorderColor.alpha = 255
                }

                val letter = gridDataAdapter?.getLetter(i, j)
                paint.getTextBounds(letter.toString(), 0, 1, charBounds)

                //highlight or selected cell color
                val selected = gridDataAdapter?.highLight(i, j)!!
                if (selected) {
                    redBckColor.color = Color.RED
                    canvas.drawRoundRect(
                        cornerX.toFloat(),
                        cornerY.toFloat(),
                        cornerX + gridWidth.toFloat() - borderSpace,
                        cornerY + gridHeight.toFloat() - borderSpace,
                        roundCornerSize.toFloat(),
                        roundCornerSize.toFloat(),
                        redBckColor
                    )
                }

                //during bomb explode animation draw background and border again for size change
                if (bombCell[i][j].animate || bombCell[i][j].replaceWordAnimate) {
                    val value = abs(bombCell[i][j].cellYaxis)
                    canvas.drawRoundRect(
                        cornerX.toFloat(),
                        cornerY.toFloat(),
                        cornerX + gridWidth.toFloat() - borderSpace,
                        cornerY + gridHeight.toFloat() - value,
                        roundCornerSize.toFloat(),
                        roundCornerSize.toFloat(),
                        greyWhiteBckColor
                    )

                    val paint = if (gridDataAdapter!!.completedCell(i, j))
                        greenBorderColor else greyBorderColor

                    canvas.drawRoundRect(
                        cornerX.toFloat(),
                        cornerY.toFloat(),
                        cornerX + gridWidth.toFloat() - borderSpace,
                        cornerY + gridHeight.toFloat() - value,
                        roundCornerSize.toFloat(),
                        roundCornerSize.toFloat(),
                        paint
                    )

                } else {
                    //border color since background cell already drawn in the background
                    val paint = if (gridDataAdapter!!.completedCell(
                            i,
                            j
                        )
                    ) greenBorderColor else greyBorderColor

                    canvas.drawRoundRect(
                        cornerX.toFloat(),
                        cornerY.toFloat(),
                        cornerX + gridWidth.toFloat() - borderSpace,
                        cornerY + gridHeight.toFloat() - borderSpace,
                        roundCornerSize.toFloat(),
                        roundCornerSize.toFloat(),
                        paint
                    )
                }

                val fireInfo = gridDataAdapter?.getFireInfo(i, j)!!
                if (fireInfo.hasFire) {
                    val sizeRatio =
                        if (spotFireImage) 1
                        else {
                            when (fireInfo.fireCount) {
                                1 -> START_FIRE_RATIO

                                2 -> FIRE_RATIO_2

                                3 -> FIRE_RATIO_3

                                4 -> FIRE_RATIO_4

                                5 -> SPREAD_FIRE_RATIO

                                else -> 1
                            }
                        }

                    val totalWidth = gridWidth - borderSpace
                    val smallWidth = totalWidth / sizeRatio
                    val halfWi = abs(smallWidth / 2)

                    val totalHeight = gridHeight - borderSpace
                    val smallHeight = abs(totalHeight / sizeRatio)

                    val centerX = ((cornerX + abs((gridWidth) - borderSpace) / 2))
                    val height = cornerY + (totalHeight - abs(smallHeight))

                    rect.left = (centerX - halfWi)
                    rect.top = height

                    val balanceHeightFromFireSize = ((cornerY + gridHeight) - borderSpace) - height

                    rect.right = ((rect.left + abs(smallWidth)))
                    rect.bottom = (rect.top + abs(balanceHeightFromFireSize))

                    if (!gameOverAnimate) {
                        canvas.drawBitmap(fireGif[fireGifIndex % 5], null, rect, null)
                    }

                }

                when {
                    bombCell[i][j].animate -> {//bomb animation
                        canvas.drawText(
                            letter.toString(),
                            bombCell[i][j].xAxix, y - charBounds.exactCenterY(), paint
                        )
                    }
                    bombCell[i][j].replaceWordAnimate -> { // replace new word animation
                        var yAxis = 0f
                        if (bombCell[i][j].cellYaxis == 0f) yAxis =
                            y - charBounds.exactCenterY() - letterSpace
                        canvas.drawText(
                            letter.toString(),
                            (x - charBounds.exactCenterX() - letterSpace), yAxis, paint
                        )
                    }
                    else -> { // no animation
                        if (fireInfo.fireCount >= 5) paint.color = Color.BLACK else paint.color =
                            Color.WHITE

                        if (!gameOverAnimate) {
                            canvas.drawText(
                                letter.toString(),
                                (x - charBounds.exactCenterX() - letterSpace),
                                (y - charBounds.exactCenterY() - letterSpace),
                                paint
                            )
                        }

                    }
                }

                if (gridDataAdapter?.hasWaterDrop(i, j) == true) {
                    val centerX = (waterX - borderSpace) + gridWidth / 2
                    rect.left =
                        centerX - waterDropBitmap!!.width//+ (halfWidth - waterDropBitmap!!.width)
                    rect.top = waterY + borderSpace
                    rect.right = rect.left + (gridWidth - borderSpace)
                    rect.bottom = rect.top + (gridHeight - borderSpace)
                    canvas.drawBitmap(waterGif[waterGifIndex % 5], null, rect, null)
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

        fireGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_massive_frame_01))
        fireGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_massive_frame_2))
        fireGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_massive_frame_3))
        fireGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_massive_frame_4))
        fireGif.add(BitmapFactory.decodeResource(resources, R.drawable.frame_massive_frame_5))

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