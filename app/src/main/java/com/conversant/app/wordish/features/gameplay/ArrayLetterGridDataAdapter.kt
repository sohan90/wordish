package com.conversant.app.wordish.features.gameplay

import com.conversant.app.wordish.custom.FireInfo
import com.conversant.app.wordish.custom.LetterGridDataAdapter

/**
 * Created by abdularis on 09/07/17.
 */
class ArrayLetterGridDataAdapter internal constructor(
    var backedGrid: Array<CharArray>,
    val fireList: Array<Array<FireInfo>> = emptyArray(),
    private val highlight: Array<BooleanArray> = emptyArray(),
    private val waterDrop: Array<BooleanArray> = emptyArray(),
    var completedCell: Array<BooleanArray> = emptyArray(),
    var gameOverCell: Array<BooleanArray> = emptyArray(),
    var highlightSelectedTilesRange: Array<BooleanArray> = emptyArray()

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
        return fireList[row][col].hasFire
    }

    override fun getFireInfo(row: Int, col: Int): FireInfo {
        return fireList[row][col]
    }

    override fun updateFire(row: Int, col: Int, hasFire: Boolean) {
        val fireInfo = fireList[row][col]
        if (hasFire) {
            fireInfo.hasFire = hasFire
            if (fireInfo.fireCount < 5) {
                fireInfo.fireCount = fireInfo.fireCount + 1
            }
        } else {
            if (fireInfo.fireCount >=3){
                fireInfo.hasFire = true
                fireInfo.fireCount = fireInfo.fireCount - 2
            } else {
                fireInfo.fireCount = 0
                fireInfo.hasFire = false
            }
        }
    }

    override fun hasWaterDrop(row: Int, col: Int): Boolean {
        return waterDrop[row][col]
    }

    override fun updateWaterDrop(row: Int, col: Int, hasWater: Boolean) {
        waterDrop[row][col] = hasWater
    }

    override fun highLight(row: Int, col: Int): Boolean {
        return highlight[row][col]
    }

    override fun updateHighlight(row: Int, col: Int, select: Boolean) {
        highlight[row][col] = select
    }

    override fun initCompletedCell(row: Int, col: Int, select: Boolean) {
        completedCell[row][col] = select
    }

    override fun completedCell(row: Int, col: Int): Boolean {
        return completedCell[row][col]
    }

    override fun gameOverTileAnimation(row: Int, col: Int): Boolean {
       return gameOverCell[row][col]
    }

    override fun highlightSelectedTilesRange(row: Int, col: Int): Boolean {
        return this.highlightSelectedTilesRange[row][col]
    }

    override fun initHiglightSelectedTileRange(row: Int, col: Int, select: Boolean) {
        this.highlightSelectedTilesRange[row][col]= select
    }
}