package ir.mega256team.checkconnection

import com.google.gson.annotations.SerializedName

data class IPInfoToken(
    @SerializedName("token")
    val token: String
)