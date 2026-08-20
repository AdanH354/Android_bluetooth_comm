package com.example.bluetooth_comm

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothService(private val adapter: BluetoothAdapter?) {

    // Standard UUID for the HC-05's SPP (Serial Port Profile)
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<android.bluetooth.BluetoothDevice> {
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(macAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val device = adapter?.getRemoteDevice(macAddress) ?: return@withContext false

            // Cancelar escaneo previó para no ralentizar la conexión
            adapter.cancelDiscovery()

            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket?.connect()

            inputStream = socket?.inputStream
            outputStream = socket?.outputStream
            true
        } catch (e: IOException) {
            e.printStackTrace()
            disconnect()
            false
        }
    }

    // Continuous loop for receiving data from the STM32 via the HC-05
    suspend fun startListening(onDataReceived: (String) -> Unit) = withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024)
        var bytes: Int

        while (socket?.isConnected == true) {
            try {
                bytes = inputStream?.read(buffer) ?: -1
                if (bytes > 0) {
                    val receivedMessage = String(buffer, 0, bytes)
                    withContext(Dispatchers.Main) {
                        onDataReceived(receivedMessage)
                    }
                } else if (bytes == -1) {
                    break // Connection closed
                }
            } catch (e: IOException) {
                break
            }
        }
    }

    // Send data to the STM32
    suspend fun sendData(data: String) = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(data.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            socket = null
        }
    }
}
