package com.conversant.app.wordish.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.decodeBitmap
import androidx.core.graphics.drawable.toBitmap
import com.conversant.app.wordish.R
import com.conversant.app.wordish.commons.orZero
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by abdularis on 22/06/17.
 *
 * Render grid of letters
 */
class LetterGrid @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GridBehavior(context, attrs), Observer {

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val fireBitmap = AppCompatResources.getDrawable(
        context,
        R.drawable.ic_baseline_local_fire_department_24
    )?.toBitmap()


    private val fireGif = mutableListOf<Bitmap>()

    private val waterGif = mutableListOf<Bitmap>()

    var fireGifIndex = 0

    var waterGifIndex = 0


    private val waterDropBitmap = AppCompatResources.getDrawable(
        context,
        R.drawable.ic_baseline_water_drop_24
    )?.toBitmap()

    val rect = Rect(0, 0, 0, 0)

    private val charBounds: Rect = Rect()

    private var gridDataAdapter: LetterGridDataAdapter? = SampleLetterGridDataAdapter(
        DEFAULT_LETTER_GRID_SAMPLE_SIZE,
        DEFAULT_LETTER_GRID_SAMPLE_SIZE
    )

    init {
        init(context, attrs)
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
            requireNotNull(newDataAdapter) { "Data Adapater can't be null" }
            if (newDataAdapter !== gridDataAdapter) {
                gridDataAdapter?.deleteObserver(this)
                gridDataAdapter = newDataAdapter
                gridDataAdapter?.addObserver(this)
                invalidate()
                requestLayout()
            }
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
        var waterX = 0

        // iterate and render all letters found in grid data adapter
        for (i in 0 until gridRowCount) {
            x = halfWidth + paddingLeft
            waterX = 0
            for (j in 0 until gridColCount) {
                val letter = gridDataAdapter?.getLetter(i, j)
                paint.getTextBounds(letter.toString(), 0, 1, charBounds)

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
                    rect.bottom = rect.top + 150
                    canvas.drawBitmap(waterGif[waterGifIndex % 5], null, rect, paint)
                }


                canvas.drawText(
                    letter.toString(),
                    x - charBounds.exactCenterX(), y - charBounds.exactCenterY(), paint
                )
                x += gridWidth
                waterX += gridWidth
            }
            y += gridHeight
            waterY += gridHeight

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

        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.water_02_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.water_03_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.water_04_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.water_05_delay))
        waterGif.add(BitmapFactory.decodeResource(resources, R.drawable.water_06_delay))

    }

    companion object {
        private const val DEFAULT_LETTER_GRID_SAMPLE_SIZE = 8
        private const val DEFAULT_TEXT_SIZE = 32f
    }
}