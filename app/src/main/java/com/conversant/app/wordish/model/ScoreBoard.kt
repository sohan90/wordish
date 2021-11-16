package com.conversant.app.wordish.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_board")
data class ScoreBoard @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,

    @ColumnInfo(name = "turns")
    var turns: Int = 0,

    @ColumnInfo(name = "words")
    var words: Int = 0,


    @ColumnInfo(name = "coins")
    var coins: Int = 0,

    @ColumnInfo(name = "board_fire_count")
    var boardFireCount: Int = 0,


    @ColumnInfo(name = "bomb_count")
    var bombCount: Int = 0,

    @ColumnInfo(name = "bomb_count_progress")
    var bombCountProgress: Int = 0,


    @ColumnInfo(name = "water_count")
    var waterCount: Int = 0,

    @ColumnInfo(name = "water_count_progress")
    var waterCountProgress: Int = 0,

    @ColumnInfo(name = "board_fire_plus_count")
    var boardFirePlusCount: Int = 0,

    @ColumnInfo(name = "board_fire_plus_count_progress")
    var boardFirePlusCountProgress: Int = 0

)