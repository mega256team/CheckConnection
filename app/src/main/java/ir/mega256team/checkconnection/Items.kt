package ir.mega256team.checkconnection

import com.google.gson.annotations.SerializedName

data class Items(
    @SerializedName("items")
    var items: List<Item>
)