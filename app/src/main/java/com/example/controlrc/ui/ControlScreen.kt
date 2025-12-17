package com.example.controlrc.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlrc.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    viewModel: MainViewModel,
    onConnectClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ESP32 Controller") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Columna Izquierda: Controles Adelante/Atrás
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VELOCIDAD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ControlButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        label = "ADELANTE",
                        enabled = uiState.isConnected,
                        onPressChange = { pressed ->
                            Log.d("ControlScreen", "ADELANTE pressed: $pressed")
                            viewModel.sendCommand(if (pressed) "F" else "S")
                        },
                        modifier = Modifier.size(110.dp)
                    )

                    ControlButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        label = "ATRÁS",
                        enabled = uiState.isConnected,
                        onPressChange = { pressed ->
                            Log.d("ControlScreen", "ATRÁS pressed: $pressed")
                            viewModel.sendCommand(if (pressed) "B" else "S")
                        },
                        modifier = Modifier.size(110.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Columna Central: Estado de conexión y Neón
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ConnectionStatus(
                    isConnected = uiState.isConnected,
                    message = uiState.connectionMessage,
                    isConnecting = uiState.isConnecting,
                    onConnectClick = onConnectClick,
                    onDisconnectClick = { viewModel.disconnect() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeonControl(
                    enabled = uiState.isConnected,
                    isOn = uiState.isNeonOn,
                    onToggle = { viewModel.toggleNeon() }
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Columna Derecha: Controles Izquierda/Derecha
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DIRECCIÓN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        label = "IZQ",
                        enabled = uiState.isConnected,
                        onPressChange = { pressed ->
                            Log.d("ControlScreen", "IZQUIERDA pressed: $pressed")
                            viewModel.sendCommand(if (pressed) "L" else "C")
                        },
                        modifier = Modifier.size(110.dp)
                    )

                    ControlButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        label = "DER",
                        enabled = uiState.isConnected,
                        onPressChange = { pressed ->
                            Log.d("ControlScreen", "DERECHA pressed: $pressed")
                            viewModel.sendCommand(if (pressed) "R" else "C")
                        },
                        modifier = Modifier.size(110.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionStatus(
    isConnected: Boolean,
    message: String,
    isConnecting: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFF4CAF50) else Color.LightGray
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) Color.White else Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Button(
                    onClick = if (isConnected) onDisconnectClick else onConnectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color(0xFFD32F2F) else Color(0xFF2196F3)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isConnected) "Desconectar" else "Conectar")
                }
            }
        }
    }
}

@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    !enabled -> Color.Gray
                    isPressed -> Color(0xFF0D47A1)
                    else -> Color(0xFF2196F3)
                }
            )
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            isPressed = true
                            onPressChange(true)
                            tryAwaitRelease()
                            isPressed = false
                            onPressChange(false)
                        }
                    }
                )
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(40.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun NeonControl(
    enabled: Boolean,
    isOn: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOn && enabled) Color(0xFFFF9800) else Color.LightGray
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Neón",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Neón ${if (isOn) "ON" else "OFF"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOn && enabled) Color.White else Color.Black
                )
            }

            Switch(
                checked = isOn,
                onCheckedChange = { if (enabled) onToggle() },
                enabled = enabled
            )
        }
    }
}