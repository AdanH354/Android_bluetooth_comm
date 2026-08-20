package com.example.bluetooth_comm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import android.bluetooth.BluetoothDevice

class DashboardViewModel : ViewModel() {
    var isConnected by mutableStateOf(false)
    var isEmergency by mutableStateOf(false)
    var isRecording by mutableStateOf(false)
    var isFailure by mutableStateOf(false)
    var motorSpeed by mutableStateOf(0f)
    var selectedDevice by mutableStateOf<BluetoothDevice?>(null)
    
    fun updateFromData(data: String) {
        when {
            data.contains("E:1") -> isEmergency = true
            data.contains("E:0") -> isEmergency = false
            data.contains("R:1") -> isRecording = true
            data.contains("R:0") -> isRecording = false
            data.contains("F:1") -> isFailure = true
            data.contains("F:0") -> isFailure = false
        }
    }
}
