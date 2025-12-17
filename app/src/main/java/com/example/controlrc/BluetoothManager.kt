package com.example.controlrc

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val ESP32_NAME = "ESP32-AUTO"
    private val UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    var onConnectionStateChanged: ((Boolean, String) -> Unit)? = null

    companion object {
        private const val TAG = "BluetoothManager"
    }

    val isBluetoothAvailable: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    val isConnected: Boolean
        get() = bluetoothSocket?.isConnected == true
@SuppressLint("MissingPermission")
    
    suspend fun connectToESP32(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting connection process...")

            if (!hasBluetoothPermissions()) {
                Log.e(TAG, "Missing Bluetooth permissions")
                return@withContext Result.failure(Exception("Permisos de Bluetooth no concedidos"))
            }

            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth adapter not available")
                return@withContext Result.failure(Exception("Bluetooth no disponible"))
            }

            if (!bluetoothAdapter.isEnabled) {
                Log.e(TAG, "Bluetooth is disabled")
                return@withContext Result.failure(Exception("Bluetooth desactivado"))
            }

            val pairedDevices: Set<BluetoothDevice>? = if (hasBluetoothConnectPermission()) {
                bluetoothAdapter.bondedDevices
            } else {
                emptySet()
            }
            Log.d(TAG, "Found ${pairedDevices?.size ?: 0} paired devices")

            val esp32Device = pairedDevices?.find { device ->
                if (hasBluetoothConnectPermission()) {
                    val deviceName = device.name
                    Log.d(TAG, "Checking device: $deviceName")
                    deviceName == ESP32_NAME
                } else {
                    false
                }
            }

            if (esp32Device == null) {
                Log.e(TAG, "ESP32-AUTO not found in paired devices")
                return@withContext Result.failure(
                    Exception("ESP32-AUTO no encontrado. Empareja el dispositivo primero.")
                )
            }

            Log.d(TAG, "ESP32-AUTO found, connecting...")
            bluetoothAdapter.cancelDiscovery()

            bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(UUID_SPP)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream

            Log.d(TAG, "Connected successfully!")

            withContext(Dispatchers.Main) {
                onConnectionStateChanged?.invoke(true, "Conectado a ESP32-AUTO")
            }

            Result.success("Conectado exitosamente")

        } catch (e: IOException) {
            Log.e(TAG, "IO Exception during connection", e)
            disconnect()
            withContext(Dispatchers.Main) {
                onConnectionStateChanged?.invoke(false, "Error de conexión: ${e.message}")
            }
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during connection", e)
            disconnect()
            withContext(Dispatchers.Main) {
                onConnectionStateChanged?.invoke(false, "Error: ${e.message}")
            }
            Result.failure(e)
        }
    }

    fun sendCommand(command: String): Boolean {
        return try {
            Log.d(TAG, "sendCommand: attempting to send '$command', isConnected=$isConnected, outputStream=$outputStream")
            if (isConnected && outputStream != null) {
                val bytes = command.toByteArray()
                outputStream?.write(bytes)
                outputStream?.flush()
                Log.d(TAG, "✓ Command sent successfully: '$command' (${bytes.size} bytes)")
                true
            } else {
                Log.w(TAG, "✗ Cannot send command '$command' - not connected (socket=${bluetoothSocket?.isConnected}, stream=$outputStream)")
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "✗ Error sending command: '$command'", e)
            disconnect()
            onConnectionStateChanged?.invoke(false, "Conexión perdida")
            false
        }
    }

    fun disconnect() {
        try {
            Log.d(TAG, "Disconnecting...")
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            outputStream = null
            bluetoothSocket = null
            onConnectionStateChanged?.invoke(false, "Desconectado")
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasBluetoothConnectPermission() && hasBluetoothScanPermission()
        } else {
            hasLocationPermission()
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasBluetoothScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}