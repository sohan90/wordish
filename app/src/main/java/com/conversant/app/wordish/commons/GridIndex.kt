package com.conversant.app.wordish.commons

/**
 * Created by abdularis on 29/06/17.
 */
data class GridIndex @JvmOverloads constructor(
    @JvmField var row: Int = 0,
    @JvmField var col: Int = 0
) {
    fun set(row: Int, col: Int) {
        this.row = row
        this.col = col
    }
}