package cl.fleetmanager.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.fleetmanager.android.ui.theme.Azul50
import cl.fleetmanager.android.ui.theme.Azul700
import cl.fleetmanager.android.ui.theme.Azul900
import cl.fleetmanager.android.ui.theme.Blanco
import cl.fleetmanager.android.ui.theme.ColorExito
import cl.fleetmanager.android.ui.theme.FuenteInter
import cl.fleetmanager.android.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PantallaInicio(
    idConductor:   String,
    nombreConductor: String,
    idVehiculo:    String,
    placaVehiculo: String,
    idAsignacion:  String,
    alEscanearQr:  () -> Unit,
    alCerrarSesion: () -> Unit,
    modeloVista:   HomeViewModel = koinViewModel(),
) {
    val estado by modeloVista.estado.collectAsState()
    val contexto = LocalContext.current

    LaunchedEffect(Unit) { modeloVista.configurar(nombreConductor, placaVehiculo, idAsignacion) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Azul50)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Encabezado
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(Azul700, Azul900)),
                            shape = RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🚛", fontSize = 18.sp)
                }
                Text(
                    "FleetManager",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FuenteInter,
                    color = Azul900,
                )
            }
            IconButton(onClick = { modeloVista.cerrarSesion(); alCerrarSesion() }) {
                Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión", tint = Azul700)
            }
        }

        Spacer(Modifier.height(28.dp))

        // Tarjeta conductor
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Blanco),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Conductor", fontSize = 12.sp, fontFamily = FuenteInter, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                Text(nombreConductor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FuenteInter, color = Azul900)
                Spacer(Modifier.height(12.dp))
                if (placaVehiculo.isNotEmpty()) {
                    Text("Vehículo asignado", fontSize = 12.sp, fontFamily = FuenteInter, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                    Text(placaVehiculo, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FuenteInter, color = Azul700)
                } else {
                    Text("Sin vehículo asignado", fontSize = 15.sp, fontFamily = FuenteInter, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Estado GPS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Blanco),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = if (estado.rastreando) ColorExito else MaterialTheme.colorScheme.onSurface.copy(alpha = .35f),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Rastreo GPS",
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FuenteInter,
                        color = Azul900,
                    )
                    Text(
                        if (estado.rastreando) "Enviando posición cada 30s" else "Inactivo",
                        fontSize = 13.sp,
                        fontFamily = FuenteInter,
                        color = if (estado.rastreando) ColorExito else MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Acciones
        if (idVehiculo.isEmpty()) {
            Button(
                onClick = alEscanearQr,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Azul700, contentColor = Blanco),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Escanear QR del vehículo", fontWeight = FontWeight.SemiBold, fontFamily = FuenteInter)
            }
        } else {
            if (!estado.rastreando) {
                Button(
                    onClick = { modeloVista.iniciarRastreo(contexto, idConductor, idVehiculo) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorExito, contentColor = Blanco),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Iniciar rastreo GPS", fontWeight = FontWeight.SemiBold, fontFamily = FuenteInter)
                }
            } else {
                OutlinedButton(
                    onClick = { modeloVista.detenerRastreo(contexto) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Detener rastreo", fontFamily = FuenteInter)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = alEscanearQr,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Azul700),
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cambiar vehículo", fontFamily = FuenteInter)
            }
        }
    }
}
