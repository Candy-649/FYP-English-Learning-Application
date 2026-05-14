package com.example.everydayenglish

import android.app.Application
import com.example.everydayenglish.data.AppContainer
import com.example.everydayenglish.data.AppDataContainer

class MyApplication: Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}