package com.conversant.app.wordish.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.view.marginLeft
import androidx.core.view.marginStart

class CustomButtonText : View {
    private lateinit var charBounds: Rect
    private lateinit var buttonRect: Rect
    private lateinit var buttonPaint: Paint

    private lateinit var paintText: TextPaint

    constructor(context: Context) : super(context)
    constructor(context: Context, attributes: AttributeSet?) : super(context, attributes) {
        init()
    }

    private fun init() {
        paintText = TextPaint()
        paintText.color = Color.RED
        paintText.style = Paint.Style.FILL
        paintText.isAntiAlias = true

        buttonPaint = Paint()
        buttonPaint.color = Color.BLACK
        buttonPaint.style = Paint.Style.FILL
        buttonPaint.isAntiAlias = true

        buttonRect = Rect()
        charBounds = Rect()
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