package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class TrocApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("TrocApplication", "FirebaseApp initialized successfully in Application.onCreate")
            }
        } catch (e: Exception) {
            Log.e("TrocApplication", "Failed to initialize FirebaseApp: ${e.message}", e)
        }
    }
}
