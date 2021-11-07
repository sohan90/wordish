package com.conversant.app.wordish.model

import com.conversant.app.wordish.custom.FireInfo


class Grid(rowCount: Int, colCount: Int) {
    var array: Array<CharArray>
        private set

    var fireArray: Array<Array<FireInfo>>
        private set

    var highlight: Array<BooleanArray>
        private set

    var completedCellHighlight: Array<BooleanArray>
        private set

    var waterDrop: Array<BooleanArray>
        private set

    val rowCount: Int
        get() = array.size

    val colCount: Int
        get() = array[0].size

    init {
        require(!(rowCount <= 0 || colCount <= 0)) { "Row and column should be greater than 0" }
        array = Array(rowCount) { CharArray(colCount) }
        fireArray = Array(rowCount) { Array(colCount){ FireInfo(0, false)} }
        waterDrop = Array(rowCount) { BooleanArray(colCount) }
        highlight = Array(rowCount) { BooleanArray(colCount) }
        completedCellHighlight = Array(rowCount) { BooleanArray(colCount) }
        reset()

    }


    private fun at(row: Int, col: Int): Char = array[row][col]

    private fun reset() {
        for (i in array.indices) {
            for (j in array[i].indices) {
                array[i][j] = NULL_CHAR
            }
        }
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        for (i in 0 until rowCount) {
            for (j in 0 until colCount) {
                stringBuilder.append(at(i, j))
            }
            if (i != rowCount - 1) stringBuilder.append(GRID_NEWLINE_SEPARATOR)
        }
        return stringBuilder.toString()
    }

    companion object {
        const val GRID_NEWLINE_SEPARATOR = ','
        const val NULL_CHAR = '\u0000'
    }

}