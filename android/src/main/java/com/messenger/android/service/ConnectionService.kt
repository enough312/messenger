package com.messenger.android.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ConnectionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
