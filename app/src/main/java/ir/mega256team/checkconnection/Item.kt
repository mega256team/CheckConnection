package ir.mega256team.checkconnection

import com.google.gson.annotations.SerializedName

data class Item(
    @SerializedName("name")
    var name: String,

    @SerializedName("ipAddresses")
    var ipAddresses: List<IPAddress>
)
