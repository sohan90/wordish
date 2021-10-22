package com.conversant.app.wordish.custom

/**
 * Created by abdularis on 28/06/17.
 *
 * Sample data adapter (for preview in android studio visual editor)
 */
internal class SampleLetterGridDataAdapter(
    private val rowCount: Int,
    private val colCount: Int
) : LetterGridDataAdapter() {

    override fun getRowCount(): Int = rowCount

    override fun getColCount(): Int = colCount

    override fun getLetter(row: Int, col: Int): Char {
        return 'A'
    }
    override fun hasFire(row: Int, col: Int): Boolean {
       return false
    }

    override fun hasWaterDrop(row: Int, col: Int): Boolean {
       return false
    }

    override fun initWaterDrop(row: Int, col: Int, hasWater:Boolean) {
    }

    override fun initFire(row: Int, col: Int, hasFire: Boolean) {

    }

    override fun highLight(row: Int, col: Int): Boolean {
       return false
    }

    override fun initHighlight(row: Int, col: Int, select: Boolean) {}

    override fun initCompletedCell(row: Int, col: Int, select: Boolean) {}

    override fun completedCell(row: Int, col: Int): Boolean {return false}


}