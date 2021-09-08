package com.conversant.app.wordish.data.room

import androidx.room.TypeConverter
import com.conversant.app.wordish.model.UsedWord.AnswerLine

object AnswerLineConverter {
    @JvmStatic
    @TypeConverter
    fun answerLineToString(answerLine: AnswerLine?): String? {
        return answerLine?.toString()
    }

    @JvmStatic
    @TypeConverter
    fun stringToAnswerLine(answerLineData: String?): AnswerLine? {
        if (answerLineData == null) return null
        val answerLine = AnswerLine()
        answerLine.fromString(answerLineData)
        return answerLine
    }
}