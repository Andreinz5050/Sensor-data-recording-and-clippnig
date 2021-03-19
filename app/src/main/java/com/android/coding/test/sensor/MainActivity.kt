package com.android.coding.test.sensor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private var AccelerometerServiceSession: AccelerometerService? = null
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )
    private val REQUEST_CODE_ANDROID = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        // setup service
        AccelerometerServiceSession = AccelerometerService()
        val intent = Intent(this, AccelerometerService::class.java)
        startService(intent)
    }
    override fun onResume() {
        super.onResume()
        if (!hasPermissions(this, *REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_ANDROID)
        }

    }
    private fun hasPermissions(
        context: Context,
        vararg permissions: String
    ): Boolean {

        // check Android hardware permissions
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }
    override fun onSaveInstanceState(outState: Bundle) { // Here You have to save service intent
        super.onSaveInstanceState(outState)
        Log.i("MyTag", "onSaveInstanceState")
        outState.putAll(outState)
        outState.putAll(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) { // Here You have to restore service intent
        super.onRestoreInstanceState(savedInstanceState)
        Log.i("MyTag", "onRestoreInstanceState")

//        savedInstanceState.getBundle(savedInstanceState)
    }
}