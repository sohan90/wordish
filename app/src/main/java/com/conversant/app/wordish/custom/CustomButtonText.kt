package com.conversant.app.wordish.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.conversant.app.wordish.R

class CustomButtonText : View {
    private lateinit var charBounds: Rect
    private lateinit var buttonRect: Rect
    private lateinit var buttonPaint: Paint

    private lateinit var paintText: TextPaint

    constructor(context: Context) : super(context) {
        init()
    }
    constructor(context: Context, attributes: AttributeSet?) : super(context, attributes) {
        init()
    }

    private fun init() {
        paintText = TextPaint()
        paintText.color = Color.YELLOW
        paintText.style = Paint.Style.FILL
        paintText.isAntiAlias = true

        buttonPaint = Paint()
        buttonPaint.color = Color.BLACK
        buttonPaint.style = Paint.Style.FILL
        buttonPaint.isAntiAlias = true

        buttonRect = Rect()
        charBounds = Rect()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val specMode = MeasureSpec.getMode(widthMeasureSpec)
        val specSize = MeasureSpec.getSize(widthMeasureSpec)

        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)

        val desiredWidth = resources.getDimension(R.dimen.bomb_width_height).toInt()
        val width = when(specMode){
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.UNSPECIFIED -> desiredWidth
            else -> Math.min(specSize, desiredWidth)
        }

        val desireHeight = resources.getDimension(R.dimen.bomb_width_height).toInt()
        val height = when(heightSpecMode){
            MeasureSpec.EXACTLY -> heightSpecSize
            MeasureSpec.UNSPECIFIED -> desireHeight
            else -> Math.min(heightSpecSize, desireHeight)
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas?) {
        buttonRect.left = 0
        buttonRect.top = 0
        buttonRect.right = width
        buttonRect.bottom = height

        canvas?.drawRect(buttonRect, buttonPaint)

        paintText.getTextBounds("Sohan", 0, 5, charBounds)
        val textX =  buttonRect.width() / 2  - charBounds.exactCenterX()
        val textY  =  buttonRect.height()/ 2 - charBounds.exactCenterY()

        canvas?.drawText("Sohan", textX, textY, paintText)
    }

}