package com.privateinvest.aitraderpro

import android.app.Application

class AITraderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        ServiceLocator.notificationManager.createChannel()
    }
}
