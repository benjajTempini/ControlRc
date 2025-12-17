package com.example.controlrc

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val isConnected: Boolean = false,
    val connectionMessage: String = "Desconectado",
    val isNeonOn: Boolean = false,
    val isConnecting: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = BluetoothManager(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "MainViewModel"
    }

    init {
        bluetoothManager.onConnectionStateChanged = { isConnected, message ->
            _uiState.value = _uiState.value.copy(
                isConnected = isConnected,
                connectionMessage = message,
                isConnecting = false
            )
            Log.d(TAG, "Connection state changed: $isConnected - $message")
        }
    }

    fun connect() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isConnecting = true,
                connectionMessage = "Conectando..."
            )
            Log.d(TAG, "Attempting to connect...")
            bluetoothManager.connectToESP32()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            Log.d(TAG, "Disconnecting...")
            withContext(Dispatchers.IO) {
                bluetoothManager.disconnect()
            }
        }
    }

    fun sendCommand(command: String) {
        Log.d(TAG, "sendCommand called with: $command, isConnected: ${_uiState.value.isConnected}")
        if (_uiState.value.isConnected) {
            viewModelScope.launch(Dispatchers.IO) {
                val success = bluetoothManager.sendCommand(command)
                Log.d(TAG, "Sent command: $command - Success: $success")
            }
        } else {
            Log.w(TAG, "Cannot send command $command - Not connected")
        }
    }

    fun toggleNeon() {
        val newState = !_uiState.value.isNeonOn
        _uiState.value = _uiState.value.copy(isNeonOn = newState)

        // Envía 'N' cada vez que se activa/desactiva
        sendCommand("N")
        Log.d(TAG, "Neon toggled: $newState")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared - disconnecting")
        disconnect()
    }
}