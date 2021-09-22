package com.conversant.app.wordish.features.gameplay

import com.conversant.app.wordish.custom.LetterGridDataAdapter

/**
 * Created by abdularis on 09/07/17.
 */
class ArrayLetterGridDataAdapter internal constructor(
    private var backedGrid: Array<CharArray>,
    val fireList:Array<BooleanArray> = emptyArray(),
    private val waterDrop:Array<BooleanArray> = emptyArray(),

) : LetterGridDataAdapter() {

    var grid: Array<CharArray>?
        get() = backedGrid
        set(grid) {
            if (grid != null && !grid.contentEquals(backedGrid)) {
                backedGrid = grid
                setChanged()
                notifyObservers()
            }
        }

    override fun getColCount(): Int {
        return backedGrid[0].size
    }

    override fun getRowCount(): Int {
        return backedGrid.size
    }

    override fun getLetter(row: Int, col: Int): Char {
        return backedGrid[row][col]
    }

    override fun hasFire(row: Int, col: Int): Boolean {
        return fireList[row][col]
    }

    override fun hasWaterDrop(row: Int, col: Int): Boolean {
       return waterDrop[row][col]
    }

    override fun initWaterDrop(row: Int, col: Int, hasWaterDrop:Boolean){
        waterDrop[row][col] = hasWaterDrop
    }

    override fun initFire(row: Int, col: Int, hasFire:Boolean) {
        fireList[row][col] = hasFire
    }

}