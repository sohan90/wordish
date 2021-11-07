package com.conversant.app.wordish.commons.generator


abstract class GridGenerator<InputType, OutputValue> {

    abstract fun setGrid(dataInput: InputType, grid: Array<CharArray>): OutputValue

    protected fun resetGrid(grid: Array<CharArray>) {
        for (i in grid.indices) {
            for (j in grid[i].indices) {
                grid[i][j] = '\u0000'
            }
        }
    }
}