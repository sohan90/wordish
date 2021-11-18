package com.conversant.app.wordish.custom

import java.util.*

/**
 * Created by abdularis on 26/06/17.
 */
abstract class LetterGridDataAdapter : Observable() {
    abstract fun getRowCount(): Int
    abstract fun getColCount(): Int
    abstract fun getLetter(row: Int, col: Int): Char
    abstract fun getFireInfo(row: Int, col: Int): FireInfo
    abstract fun hasFire(row: Int, col: Int): Boolean
    abstract fun hasWaterDrop(row: Int, col: Int): Boolean
    abstract fun updateWaterDrop(row: Int, col: Int, hasWater: Boolean)
    abstract fun updateFire(row: Int, col: Int, hasFire:Boolean)
    abstract fun highLight(row: Int, col: Int):Boolean
    abstract fun updateHighlight(row: Int, col: Int, select:Boolean)
    abstract fun initCompletedCell(row: Int, col: Int, select:Boolean)
    abstract fun completedCell(row: Int, col: Int):Boolean
    abstract fun gameOverTileAnimation(row: Int, col: Int):Boolean
}