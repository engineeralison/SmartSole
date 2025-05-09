package com.example.smartsole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartsole.ui.theme.SmartSoleTheme
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


private val permissions = arrayOf(
    android.Manifest.permission.BLUETOOTH,
    android.Manifest.permission.BLUETOOTH_ADMIN,
    android.Manifest.permission.ACCESS_FINE_LOCATION
)



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define the permissions your app needs
        val permissions = arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION // required for Bluetooth scanning in some cases
        )

        // Check if all permissions are granted
        var allPermissionsGranted = true
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false
                Log.i("permissions", "$permission not granted.")
                break
            }
        }

        // Request permissions if not granted
        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }

        enableEdgeToEdge()
        setContent {
            SmartSoleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Navigation()
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun NavigationPreview() {
    SmartSoleTheme {
        Navigation()
    }
}