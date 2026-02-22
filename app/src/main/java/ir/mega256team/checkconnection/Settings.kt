package ir.mega256team.checkconnection

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MySettings")
data class MySettings(
    @PrimaryKey var keyy: String,
    @ColumnInfo var value: String
)
