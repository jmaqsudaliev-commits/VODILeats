package com.vodileats.core.common.constants

object ApiConstants {
    const val BASE_URL = "http://10.0.2.2:3000/api/v1/"
    const val WS_URL = "ws://10.0.2.2:3000/tracking"
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}

object LocationConstants {
    const val LOCATION_UPDATE_INTERVAL = 5000L // 5 soniya
    const val FASTEST_LOCATION_INTERVAL = 3000L
    const val LOCATION_DISPLACEMENT = 10f // 10 metr
}

object NotificationConstants {
    const val COURIER_TRACKING_CHANNEL_ID = "courier_tracking"
    const val COURIER_TRACKING_CHANNEL_NAME = "Kuryer Tracking"
    const val NEW_ORDER_CHANNEL_ID = "new_order"
    const val NEW_ORDER_CHANNEL_NAME = "Yangi Buyurtmalar"
    const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1001
    const val NEW_ORDER_NOTIFICATION_ID = 2001
}
