package ir.mega256team.checkconnection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.TimeUnit


class ConnectivityMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val client = OkHttpClient.Builder()
    }

    private lateinit var addressTestArray: Array<AddressTest>
    private var delay: Long = Constants.DEFAULT_DELAY
    private var timeOut: Int = Constants.DEFAULT_TIMEOUT
    private var notifText: String = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(Constants.NOTIFICATION_ID, buildNotification(resources.getString(R.string.testing)))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
        scope.cancel()
    }

    //==============================================================================================

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        addressTestArray = intent?.getSerializableExtra(Constants.ADDRESS_TEST_KEY) as Array<AddressTest>
        delay = intent.getLongExtra(Constants.DELAY, Constants.DEFAULT_DELAY)
        timeOut = intent.getIntExtra(Constants.TIME_OUT, Constants.DEFAULT_TIMEOUT)

        client.connectTimeout(timeOut.toLong(), TimeUnit.MILLISECONDS)
        client.readTimeout(timeOut.toLong(), TimeUnit.MILLISECONDS)
        client.writeTimeout(timeOut.toLong(), TimeUnit.MILLISECONDS)
        client.build()

        startMonitoring()
        return START_STICKY
    }

    //==============================================================================================

    override fun onBind(intent: Intent?): IBinder? = null

    //==============================================================================================

    private fun startMonitoring() {
        val context = this

        var shouldTestAccessible: Boolean
        var shouldTestViaTcp: Boolean
        var shouldTestViaHttps: Boolean

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        scope.launch {

            while (isActive) {
                notifText = ""

                if (Utils().getSettings(context, Constants.ACCESSIBLE_PING_KEY, "1") == "1") {
                    shouldTestAccessible = true

                } else {
                    shouldTestAccessible = false
                }

                //=================================================

                if (Utils().getSettings(context, Constants.REACHABLE_VIA_HTTPS_KEY, "1") == "1") {
                    shouldTestViaHttps = true

                } else {
                    shouldTestViaHttps = false
                }

                //=================================================

                if (Utils().getSettings(context, Constants.REACHABLE_VIA_TCP_KEY, "1") == "1") {
                    shouldTestViaTcp = true

                } else {
                    shouldTestViaTcp = false
                }

                //=================================================

                var httpOk = false
                if (shouldTestAccessible) {
                    httpOk = hasUsableInternet()
                }

                initAddressTestArray2(0, shouldTestViaTcp, shouldTestViaHttps, shouldTestAccessible, httpOk)
                initAddressTestArray2(1, shouldTestViaTcp, shouldTestViaHttps, shouldTestAccessible, httpOk)
                initAddressTestArray2(2, shouldTestViaTcp, shouldTestViaHttps, shouldTestAccessible, httpOk)

                val updatedNotification = buildNotification(notifText)
                notificationManager.notify(Constants.NOTIFICATION_ID, updatedNotification)

                sendUpdate()
                delay(delay)
            }
        }
    }

    //==============================================================================================

    fun initAddressTestArray2(
        index: Int,
        shouldTestViaTcp: Boolean,
        shouldTestViaHttps: Boolean,
        shouldTestAccessible: Boolean,
        httpOk: Boolean
    ) {
        if (addressTestArray[index].shouldBeTested) {
            if (!shouldTestViaTcp && !shouldTestViaHttps && !shouldTestAccessible) { //000
                notifText += " ${index + 1}❌ "
                addressTestArray[index].reachable = false
                addressTestArray[index].testFailed = addressTestArray[index].testFailed + 1


            } else if (!shouldTestViaTcp && !shouldTestViaHttps && shouldTestAccessible) { //001
                if (httpOk) {
                    notifText += " ${index + 1}✅ "
                    addressTestArray[index].reachable = true
                    addressTestArray[index].testSuccess = addressTestArray[index].testSuccess + 1

                } else {
                    notifText += " ${index + 1}❌ "
                    addressTestArray[index].reachable = false
                    addressTestArray[index].testFailed = addressTestArray[index].testFailed + 1
                }

            } else if (!shouldTestViaTcp && shouldTestViaHttps && !shouldTestAccessible) { //010
                if (isIpReachableViaHttps(addressTestArray[index])) {
                    notifText += " ${index + 1}✅ "
                    addressTestArray[index].reachable = true
                    addressTestArray[index].testSuccess = addressTestArray[index].testSuccess + 1

                } else {
                    notifText += " ${index + 1}❌ "
                    addressTestArray[index].reachable = false
                    addressTestArray[index].testFailed = addressTestArray[index].testFailed + 1
                }

            } else if (!shouldTestViaTcp && shouldTestViaHttps && shouldTestAccessible) { //011
                if (isIpReachableViaHttps(addressTestArray[index]) && httpOk) {
                    notifText += " ${index + 1}✅ "
                    addressTestArray[index].reachable = true
                    addressTestArray[index].testSuccess = addressTestArray[index].testSuccess + 1

                } else {
                    notifText += " ${index + 1}❌ "
                    addressTestArray[index].reachable = false
                    addressTestArray[index].testFailed = addressTestArray[index].testFailed + 1
                }

            } else if (shouldTestViaTcp && !shouldTestViaHttps && !shouldTestAccessible) { //100
                if (isIpReachableViaTcp(addressTestArray[index])) {
                    notifText += " ${index + 1}✅ "
                    addressTestArray[index].reachable = true
                    addressTestArray[index].testSuccess = addressTestArray[index].testSuccess + 1

                } else {
                    notifText += " ${index + 1}❌ "
                    addressTestArray[index].reachable = false
                    addressTestArray[index].testFailed = addressTestArray[index].testFailed + 1
                }

            } else if (shouldTestViaTcp && !shouldTestViaHttps && shouldTestAccessible) { //101
                if (isIpReachableViaTcp(addressTestArray[index]) && httpOk) {
                    notifText += " ${index + 1}✅ "
                    addressTestArray[index].reachable = true
                    addressTestArray[index].testSuccess = addressTestArray[index].testSuccess + 1

                } else {
                    notifText += " ${index + 1}❌ "
                    addressTestArray[index].reachable = false
                    addressTestArray[index].testFailed = addressTestArray[index].testFailed + 1
                }

            } else if (shouldTestViaTcp && shouldTestViaHttps && !shouldTestAccessible) { //110
                if (isIpReachableViaTcp(addressTestArray[index]) && isIpReachableViaHttps(addressTestArray[index])) {
                    notifText += " ${index + 1}✅ "
                    addressTestArray[index].reachable = true
                    addressTestArray[index].testSuccess = addressTestArray[index].testSuccess + 1

                } else {
                    notifText += " ${index + 1}❌ "
                    addressTestArray[index].reachable = false
                    addressTestArray[index].testFailed = addressTestArray[index].testFailed + 1
                }

            } else if (shouldTestViaTcp && shouldTestViaHttps && shouldTestAccessible) { //111
                if (isIpReachableViaTcp(addressTestArray[index]) && isIpReachableViaHttps(addressTestArray[index]) && httpOk) {
                    notifText += " ${index + 1}✅ "
                    addressTestArray[index].reachable = true
                    addressTestArray[index].testSuccess = addressTestArray[index].testSuccess + 1

                } else {
                    notifText += " ${index + 1}❌ "
                    addressTestArray[index].reachable = false
                    addressTestArray[index].testFailed = addressTestArray[index].testFailed + 1
                }
            }
        }
    }

    //==============================================================================================

    private fun buildNotification(status: String): Notification {
        return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    //==============================================================================================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_ID,
                resources.getString(R.string.connectivityStatus),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    //==============================================================================================

    fun isIpReachableViaTcp(addressTest: AddressTest): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(addressTest.ipOrDomain, addressTest.port), timeOut)
                socket.soTimeout = timeOut
                socket.getOutputStream().write(0xFFF)
                true
            }

        } catch (e: IOException) {
            false

        } catch (e: Exception) {
            false
        }
    }

    //==============================================================================================

    fun isIpReachableViaHttps(addressTest: AddressTest): Boolean {
        return try {
            var https = ""
            if (!addressTest.ipOrDomain.contains("http")){
                https = "https://"
            }

            val conn = (URL(https + addressTest.ipOrDomain).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = timeOut
                readTimeout = timeOut
                instanceFollowRedirects = false
            }
            conn.connect()
            val code = conn.responseCode
            conn.disconnect()
            code in 200..399

        } catch (e: Exception) {
            false
        }
    }

    //==============================================================================================

    fun hasUsableInternet(): Boolean {
        return try {
            val request = Request.Builder()
                .url(Constants.clients3Url2)
                .build()

            val response = client.build().newCall(request).execute()
            response.use { it.code == 204 }

        } catch (e: Exception) {
            false
        }
    }

    //==============================================================================================

    private fun sendUpdate() {
        val intent = Intent(Constants.CONNECTIVITY_UPDATE)
        intent.putExtra(Constants.ADDRESS_TEST_KEY, addressTestArray)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}