package com.conversant.app.wordish.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_status")
open class GameStatus @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,

    @ColumnInfo(name = "letter_column")
    var letterColumn: String = "",

    @ColumnInfo(name = "completed_column")
    var completedColumn: String = "",

    @ColumnInfo(name = "fire_column")
    var fireColumn: String = "",

    @ColumnInfo(name = "fire_count")
    var fireCount: String = ""
)