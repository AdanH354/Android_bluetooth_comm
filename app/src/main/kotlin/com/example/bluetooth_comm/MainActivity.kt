package com.example.bluetooth_comm

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var bluetoothService: BluetoothService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothService = BluetoothService(btManager.adapter)

        checkPermissions()

        setContent {
            MaterialTheme {
                DashboardScreen(
                    isConnected = viewModel.isConnected,
                    isEmergency = viewModel.isEmergency,
                    isRecording = viewModel.isRecording,
                    isFailure = viewModel.isFailure,
                    motorSpeed = viewModel.motorSpeed,
                    selectedDeviceName = viewModel.selectedDevice?.let { "${it.name ?: "Unknown"} (${it.address})" },
                    onConnect = { connectToDevice() },
                    onDisconnect = { disconnectDevice() },
                    onSelectDevice = { showDeviceSelectionDialog() },
                    onCommand = { sendCommand(it) },
                    onSpeedChange = { viewModel.motorSpeed = it },
                    onClose = { finish() }
                )
            }
        }
    }

    private fun showDeviceSelectionDialog() {
        val devices = bluetoothService.getPairedDevices()
        if (devices.isEmpty()) {
            Toast.makeText(this, "No hay dispositivos vinculados", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = devices.map { it.name ?: it.address }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Seleccionar Dispositivo HC-05")
            .setItems(deviceNames) { _, which ->
                viewModel.selectedDevice = devices[which]
            }
            .show()
    }

    private fun connectToDevice() {
        val device = viewModel.selectedDevice
        if (device == null) {
            Toast.makeText(this, "Selecciona un dispositivo primero", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val isConnected = bluetoothService.connect(device.address)
            if (isConnected) {
                viewModel.isConnected = true
                bluetoothService.startListening { data ->
                    viewModel.updateFromData(data)
                }
            } else {
                Toast.makeText(this@MainActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disconnectDevice() {
        bluetoothService.disconnect()
        viewModel.isConnected = false
    }

    private fun sendCommand(cmd: String) {
        lifecycleScope.launch {
            bluetoothService.sendData(cmd + "\n")
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
        }
    }
}

@Composable
fun DashboardScreen(
    isConnected: Boolean,
    isEmergency: Boolean,
    isRecording: Boolean,
    isFailure: Boolean,
    motorSpeed: Float,
    selectedDeviceName: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSelectDevice: () -> Unit,
    onCommand: (String) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Placeholder
        Image(
            painter = painterResource(id = android.R.drawable.ic_menu_info_details), // Using system icon as placeholder
            contentDescription = "Logo",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
        )

        Text(
            text = "Bluetooth Dashboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Button(
            onClick = { if (isConnected) onDisconnect() else onConnect() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) Color.Gray else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isConnected) "Desconectar" else "Conectar")
        }

        OutlinedButton(
            onClick = onSelectDevice,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Seleccionar Dispositivo")
        }

        Text(
            text = selectedDeviceName ?: "Ningún dispositivo seleccionado",
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Indicators
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IndicatorItem("Connected", isConnected)
            IndicatorItem("Emergency", isEmergency)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IndicatorItem("Recording", isRecording)
            IndicatorItem("Failure", isFailure)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Control Buttons
        DashboardButton("Turn on motor") { onCommand("MOTOR_ON") }
        DashboardButton("Start recording") { onCommand("START_REC") }
        DashboardButton("Take a picture") { onCommand("TAKE_PIC") }

        Spacer(modifier = Modifier.height(24.dp))

        // Motor Speed
        Text("Motor Speed: ${motorSpeed.toInt()}%")
        Slider(
            value = motorSpeed,
            onValueChange = { 
                onSpeedChange(it)
                onCommand("SPEED:${it.toInt()}")
            },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Close App", color = Color.White)
        }
    }
}

@Composable
fun IndicatorItem(label: String, isOn: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isOn) Color(0xFF4CAF50) else Color.Gray)
        )
        Text(text = " $label", fontSize = 14.sp)
    }
}

@Composable
fun DashboardButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text)
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    MaterialTheme {
        DashboardScreen(
            isConnected = true,
            isEmergency = false,
            isRecording = true,
            isFailure = false,
            motorSpeed = 45f,
            selectedDeviceName = "HC-05 (00:21:13:00:00:00)",
            onConnect = {},
            onDisconnect = {},
            onSelectDevice = {},
            onCommand = {},
            onSpeedChange = {},
            onClose = {}
        )
    }
}
