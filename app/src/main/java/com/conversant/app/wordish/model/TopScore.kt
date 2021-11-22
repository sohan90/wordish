package com.conversant.app.wordish.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "top_score")
open class TopScore @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,

    @ColumnInfo(name = "turns")
    var turns: Int = 0,

    @ColumnInfo(name = "words")
    var words: Int = 0,

    @ColumnInfo(name = "coins")
    var coins: Int = 0,

    @ColumnInfo(name = "won")
    var won: Int = 0
)