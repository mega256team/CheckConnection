package ir.mega256team.checkconnection

import retrofit2.http.GET
import retrofit2.http.Url

interface APIInterface {
    @GET("matrix2101/CheckConnectionPredefined/refs/heads/master/predefined.json")
    suspend fun getPredefined(): Items?

    @GET("matrix2101/CheckConnectionPredefined/refs/heads/master/ip_info_token.json")
    suspend fun getIPInfoToken(): IPInfoToken?

    @GET()
    suspend fun getIPInfo(@Url url: String): IPInfoLite?
}