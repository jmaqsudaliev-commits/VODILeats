package com.vodileats.core.network.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vodileats.core.common.constants.ApiConstants
import com.vodileats.core.network.auth.TokenManager
import com.vodileats.core.network.model.CourierLocationUpdate
import com.vodileats.core.network.model.OrderOfferData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Socket.IO protocol bilan ishlaydigan WebSocket client.
 * Barcha real-time eventlarni boshqaradi.
 */
@Singleton
class TrackingWebSocketClient @Inject constructor(
    private val tokenManager: TokenManager
) {
    companion object {
        private const val TAG = "WebSocketClient"
        private const val RECONNECT_DELAY_MS = 5000L
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val PING_INTERVAL_MS = 25000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    private var shouldReconnect = true

    // Connection state
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Event flows
    private val _courierLocationUpdates = MutableSharedFlow<CourierLocationUpdate>(replay = 0)
    val courierLocationUpdates: SharedFlow<CourierLocationUpdate> = _courierLocationUpdates.asSharedFlow()

    private val _orderStatusChanges = MutableSharedFlow<OrderStatusChange>(replay = 0)
    val orderStatusChanges: SharedFlow<OrderStatusChange> = _orderStatusChanges.asSharedFlow()

    private val _newOrderAlerts = MutableSharedFlow<NewOrderAlert>(replay = 0)
    val newOrderAlerts: SharedFlow<NewOrderAlert> = _newOrderAlerts.asSharedFlow()

    private val _orderOffers = MutableSharedFlow<OrderOfferData>(replay = 0)
    val orderOffers: SharedFlow<OrderOfferData> = _orderOffers.asSharedFlow()

    /**
     * WebSocket ulanishni boshlash
     */
    fun connect() {
        shouldReconnect = true
        reconnectAttempts = 0
        establishConnection()
    }

    private fun establishConnection() {
        val token = tokenManager.getAccessTokenSync() ?: run {
            Log.w(TAG, "Token yo'q, ulanish bekor qilindi")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING

        val client = OkHttpClient.Builder()
            .pingInterval(PING_INTERVAL_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        // Socket.IO handshake - avval polling transport, keyin WebSocket upgrade
        // Soddalashtirilgan versiya - to'g'ridan-to'g'ri WebSocket
        val wsUrl = "${ApiConstants.WS_URL}/?token=$token&EIO=4&transport=websocket"

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "✅ WebSocket ulandi")
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempts = 0

                // Socket.IO handshake
                webSocket.send("40/tracking,")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "🔌 WebSocket yopilmoqda: $code - $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "🔌 WebSocket yopildi: $code - $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
                attemptReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "❌ WebSocket xatolik: ${t.message}")
                _connectionState.value = ConnectionState.DISCONNECTED
                attemptReconnect()
            }
        })
    }

    /**
     * Socket.IO xabarlarini parse qilish
     */
    private fun handleMessage(rawMessage: String) {
        try {
            // Socket.IO protocol: "42/tracking,["event_name", data]"
            if (!rawMessage.startsWith("42")) return

            val jsonPart = rawMessage.substringAfter(",", "")
            if (jsonPart.isEmpty()) return

            val jsonArray = JSONArray(jsonPart)
            if (jsonArray.length() < 2) return

            val eventName = jsonArray.getString(0)
            val eventData = jsonArray.getJSONObject(1)

            Log.d(TAG, "📩 Event: $eventName")

            scope.launch {
                when (eventName) {
                    "courier:location-update" -> {
                        val update = CourierLocationUpdate(
                            courierId = eventData.getString("courierId"),
                            latitude = eventData.getDouble("latitude"),
                            longitude = eventData.getDouble("longitude"),
                            bearing = eventData.optDouble("bearing", 0.0).toFloat(),
                            timestamp = eventData.getString("timestamp")
                        )
                        _courierLocationUpdates.emit(update)
                    }

                    "order:status-changed" -> {
                        val change = OrderStatusChange(
                            orderId = eventData.getString("orderId"),
                            orderNumber = eventData.getString("orderNumber"),
                            status = eventData.getString("status")
                        )
                        _orderStatusChanges.emit(change)
                    }

                    "restaurant:new-order" -> {
                        val alert = NewOrderAlert(
                            orderId = eventData.getString("orderId"),
                            orderNumber = eventData.getString("orderNumber"),
                            totalAmount = eventData.getDouble("totalAmount"),
                            deliveryAddress = eventData.getString("deliveryAddress")
                        )
                        _newOrderAlerts.emit(alert)
                    }

                    "courier:new-offer" -> {
                        val offer = OrderOfferData(
                            orderId = eventData.getString("orderId"),
                            orderNumber = eventData.getString("orderNumber"),
                            restaurantName = eventData.getString("restaurantName"),
                            restaurantAddress = eventData.getString("restaurantAddress"),
                            restaurantLatitude = eventData.getDouble("restaurantLatitude"),
                            restaurantLongitude = eventData.getDouble("restaurantLongitude"),
                            deliveryAddress = eventData.getString("deliveryAddress"),
                            deliveryLatitude = eventData.getDouble("deliveryLatitude"),
                            deliveryLongitude = eventData.getDouble("deliveryLongitude"),
                            totalAmount = eventData.getDouble("totalAmount"),
                            distanceToRestaurant = eventData.getDouble("distanceToRestaurant"),
                            estimatedDeliveryDistance = eventData.getDouble("estimatedDeliveryDistance"),
                            timeoutSeconds = eventData.getInt("timeoutSeconds")
                        )
                        _orderOffers.emit(offer)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Xabar parse xatolik: ${e.message}")
        }
    }

    /**
     * Socket.IO event yuborish
     */
    fun emit(event: String, data: JSONObject) {
        val message = "42/tracking,[\"$event\",${data}]"
        webSocket?.send(message)
        Log.d(TAG, "📤 Yuborildi: $event")
    }

    /**
     * Buyurtmani kuzatishni boshlash
     */
    fun trackOrder(orderId: String) {
        emit("customer:track-order", JSONObject().put("orderId", orderId))
    }

    /**
     * Restoran buyurtmalarini kuzatish
     */
    fun listenRestaurant(restaurantId: String) {
        emit("restaurant:listen", JSONObject().put("restaurantId", restaurantId))
    }

    /**
     * Kuryer ro'yxatdan o'tkazish
     */
    fun registerCourier(courierId: String) {
        emit("courier:register", JSONObject().put("courierId", courierId))
    }

    /**
     * Kuryer lokatsiyasini yuborish
     */
    fun sendCourierLocation(latitude: Double, longitude: Double, bearing: Float = 0f) {
        emit("courier:location", JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("bearing", bearing)
        })
    }

    /**
     * Buyurtma taklifiga javob berish
     */
    fun respondToOffer(orderId: String, accepted: Boolean) {
        emit("courier:offer-response", JSONObject().apply {
            put("orderId", orderId)
            put("accepted", accepted)
        })
    }

    /**
     * Qayta ulanish
     */
    private fun attemptReconnect() {
        if (!shouldReconnect || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return

        scope.launch {
            reconnectAttempts++
            val delay = RECONNECT_DELAY_MS * reconnectAttempts
            Log.i(TAG, "🔄 Qayta ulanish: $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS (${delay}ms)")
            delay(delay)
            establishConnection()
        }
    }

    /**
     * Ulanishni yopish
     */
    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

data class OrderStatusChange(
    val orderId: String,
    val orderNumber: String,
    val status: String
)

data class NewOrderAlert(
    val orderId: String,
    val orderNumber: String,
    val totalAmount: Double,
    val deliveryAddress: String
)
