package cl.truckmanager.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.truckmanager.android.gps.UltimaUbicacion
import cl.truckmanager.android.ui.theme.*
import cl.truckmanager.android.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PantallaInicio(
    idConductor:     String,
    nombreConductor: String,
    placaConfirmada: String = "",
    alEscanearQr:    (vehiculoEsperadoId: String) -> Unit,
    alCerrarSesion:  () -> Unit,
    modeloVista:     HomeViewModel = koinViewModel(),
) {
    val estado   by modeloVista.estado.collectAsState()
    val contexto  = LocalContext.current

    LaunchedEffect(idConductor) {
        modeloVista.cargar(idConductor, nombreConductor, contexto)
    }
    LaunchedEffect(placaConfirmada) {
        if (placaConfirmada.isNotBlank()) modeloVista.vehiculoConfirmado(placaConfirmada)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Azul50)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Encabezado ─────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(Azul700, Azul900)),
                            shape = RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) { Text("🚛", fontSize = 18.sp) }
                Text("TruckManager", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FuenteInter, color = Azul900)
            }
            IconButton(onClick = { modeloVista.cerrarSesion(contexto); alCerrarSesion() }) {
                Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión", tint = Azul700)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Tarjeta conductor ───────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Blanco),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Conductor", fontSize = 12.sp, fontFamily = FuenteInter,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                Text(estado.nombreConductor.ifBlank { nombreConductor },
                    fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FuenteInter, color = Azul900)
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Contenido principal ─────────────────────────────────────────────
        when {
            estado.cargando -> {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(color = Azul700)
                Spacer(Modifier.height(12.dp))
                Text("Buscando servicio asignado...", fontFamily = FuenteInter, color = Azul700)
            }

            estado.error != null -> {
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Error", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error, fontFamily = FuenteInter)
                        Text(estado.error!!, fontSize = 13.sp, fontFamily = FuenteInter)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { modeloVista.cargar(idConductor, nombreConductor, contexto) },
                    colors = ButtonDefaults.buttonColors(containerColor = Azul700, contentColor = Blanco),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Reintentar", fontFamily = FuenteInter) }
            }

            estado.servicio == null -> {
                Spacer(Modifier.height(40.dp))
                Icon(Icons.Default.EventBusy, contentDescription = null,
                    tint = Azul700.copy(alpha = .4f), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Sin servicio asignado", fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold, fontFamily = FuenteInter, color = Azul900)
                Text("No tienes servicios aprobados pendientes.", fontSize = 14.sp,
                    fontFamily = FuenteInter,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { modeloVista.cargar(idConductor, nombreConductor, contexto) },
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Actualizar", fontFamily = FuenteInter) }
            }

            else -> {
                val servicio = estado.servicio!!

                // ── Tarjeta servicio ─────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Blanco),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Servicio asignado", fontSize = 12.sp, fontFamily = FuenteInter,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                                if (!servicio.numServicio.isNullOrBlank())
                                    Text("N° ${servicio.numServicio}", fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold, fontFamily = FuenteInter,
                                        color = Azul900)
                            }
                            Surface(
                                color = if (estado.servicioEnCurso) ColorExito.copy(alpha = .15f)
                                        else Azul700.copy(alpha = .12f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    if (estado.servicioEnCurso) "En curso" else "Aprobado",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    fontFamily = FuenteInter,
                                    color = if (estado.servicioEnCurso) ColorExito else Azul700,
                                )
                            }
                        }
                        // Tipo de transporte
                        servicio.tipoServicio?.takeIf { it.isNotBlank() }?.let { tipo ->
                            FilaIconoTexto(Icons.Default.LocalShipping, "Tipo de transporte",
                                tipo, Azul700)
                        }
                        // Fecha de inicio
                        servicio.fechaServicio?.takeIf { it.isNotBlank() }?.let { fecha ->
                            FilaIconoTexto(Icons.Default.CalendarToday, "Fecha de inicio",
                                fecha, Azul700)
                        }
                        // Cliente
                        servicio.clienteNombre?.takeIf { it.isNotBlank() }?.let { nombre ->
                            HorizontalDivider(color = Azul50)
                            val textoCliente = buildString {
                                append(nombre)
                                servicio.clienteRut?.takeIf { it.isNotBlank() }?.let {
                                    append("\nRUT: $it")
                                }
                            }
                            FilaIconoTexto(Icons.Default.Business, "Cliente", textoCliente, Azul700)
                        }
                        HorizontalDivider(color = Azul50)
                        FilaIconoTexto(Icons.Default.TripOrigin, "Origen", servicio.origen, Azul700)
                        FilaIconoTexto(Icons.Default.Place, "Destino", servicio.destino,
                            MaterialTheme.colorScheme.error)

                        if (estado.placaAsignada.isNotBlank()) {
                            HorizontalDivider(color = Azul50)
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null,
                                    tint = if (estado.vehiculoValidado) ColorExito else Azul700,
                                    modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        if (estado.vehiculoValidado) "Camión verificado ✓"
                                        else "Camión asignado",
                                        fontSize = 11.sp, fontFamily = FuenteInter,
                                        color = if (estado.vehiculoValidado) ColorExito
                                                else MaterialTheme.colorScheme.onSurface.copy(.55f))
                                    Text(estado.placaAsignada, fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FuenteInter, color = Azul900)
                                    if (estado.marcaModeloAsignado.isNotBlank())
                                        Text(estado.marcaModeloAsignado, fontSize = 12.sp,
                                            fontFamily = FuenteInter,
                                            color = MaterialTheme.colorScheme.onSurface.copy(.55f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Panel GPS (siempre visible cuando rastreando) ────────
                if (estado.rastreando) {
                    TarjetaEstadoGps(ultimaUbicacion = estado.ultimaUbicacion)
                    Spacer(Modifier.height(14.dp))
                }

                Spacer(Modifier.height(16.dp))

                // ── Botones ───────────────────────────────────────────────
                when {
                    estado.rastreando -> {
                        OutlinedButton(
                            onClick = { modeloVista.detenerRastreo(contexto) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Detener rastreo", fontFamily = FuenteInter,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                    estado.servicioEnCurso -> {
                        Button(
                            onClick = {
                                val intent = android.content.Intent(
                                    contexto,
                                    cl.truckmanager.android.service.ServicioRastreoGps::class.java
                                ).apply {
                                    putExtra(cl.truckmanager.android.service.ServicioRastreoGps.EXTRA_CONDUCTOR, idConductor)
                                    putExtra(cl.truckmanager.android.service.ServicioRastreoGps.EXTRA_VEHICULO, servicio.vehiculoId ?: "")
                                }
                                contexto.startForegroundService(intent)
                                modeloVista.cargar(idConductor, nombreConductor, contexto)
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorExito, contentColor = Blanco),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reanudar GPS", fontFamily = FuenteInter,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                    estado.vehiculoValidado -> {
                        Button(
                            onClick = { modeloVista.iniciarServicioYRastreo(contexto, idConductor) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorExito, contentColor = Blanco),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Iniciar servicio", fontFamily = FuenteInter,
                                fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    else -> {
                        Button(
                            onClick = { alEscanearQr(servicio.vehiculoId ?: "") },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Azul700, contentColor = Blanco),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Escanear QR del camión asignado", fontFamily = FuenteInter,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ── Tarjeta GPS + panel debug ──────────────────────────────────────────────────
@Composable
private fun TarjetaEstadoGps(ultimaUbicacion: UltimaUbicacion?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = ColorExito.copy(alpha = .08f)),
        shape    = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Título ────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GpsFixed, contentDescription = null,
                    tint = ColorExito, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("GPS activo", fontWeight = FontWeight.SemiBold,
                        fontFamily = FuenteInter, color = ColorExito)
                    Text("Intervalo de envío: 60 s",
                        fontSize = 11.sp, fontFamily = FuenteInter,
                        color = ColorExito.copy(alpha = .7f))
                }
                if (ultimaUbicacion != null) {
                    Surface(color = ColorExito.copy(.15f), shape = RoundedCornerShape(8.dp)) {
                        Text("Envío #${ultimaUbicacion.intentos}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FuenteInter, color = ColorExito)
                    }
                }
            }

            HorizontalDivider(color = ColorExito.copy(alpha = .15f))

            if (ultimaUbicacion == null) {
                // Esperando primera ubicación
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp, color = ColorExito)
                    Text("Esperando primera ubicación…", fontSize = 13.sp,
                        fontFamily = FuenteInter, color = ColorExito.copy(.8f))
                }
            } else {
                // ── Estado recepción BFF ──────────────────────────
                val recibido    = ultimaUbicacion.recibidoEnBff
                val colorEstado = if (recibido) ColorExito else MaterialTheme.colorScheme.error
                val iconoEstado = if (recibido) Icons.Default.CheckCircle else Icons.Default.ErrorOutline

                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(iconoEstado, contentDescription = null,
                        tint = colorEstado, modifier = Modifier.size(18.dp))
                    Text(
                        if (recibido) "Recibido en servidor" else "Error al enviar al servidor",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FuenteInter, color = colorEstado,
                        modifier = Modifier.weight(1f),
                    )
                    Text(ultimaUbicacion.horaEnvio, fontSize = 11.sp,
                        fontFamily = FuenteInter,
                        color = MaterialTheme.colorScheme.onSurface.copy(.45f))
                }

                // ── Respuesta BFF ─────────────────────────────────
                if (ultimaUbicacion.respuestaBff != null) {
                    Surface(
                        color  = if (recibido) ColorExito.copy(.1f)
                                 else MaterialTheme.colorScheme.errorContainer,
                        shape  = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "BFF → ${ultimaUbicacion.respuestaBff}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 11.sp, fontFamily = FuenteInter,
                            color = if (recibido) ColorExito
                                    else MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // ── Datos enviados ────────────────────────────────
                HorizontalDivider(color = ColorExito.copy(alpha = .12f))
                Text("Datos enviados", fontSize = 10.sp, fontFamily = FuenteInter,
                    color = MaterialTheme.colorScheme.onSurface.copy(.45f),
                    fontWeight = FontWeight.SemiBold)

                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipDato(Icons.Default.MyLocation, "Latitud",
                        "%.6f".format(ultimaUbicacion.latitud), Modifier.weight(1f))
                    ChipDato(Icons.Default.MyLocation, "Longitud",
                        "%.6f".format(ultimaUbicacion.longitud), Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipDato(Icons.Default.Speed, "Velocidad",
                        "${"%.1f".format((ultimaUbicacion.velocidad ?: 0.0) * 3.6)} km/h",
                        Modifier.weight(1f))
                    ChipDato(Icons.Default.GpsFixed, "Precisión",
                        "${"%.0f".format(ultimaUbicacion.precision ?: 0.0)} m",
                        Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────
@Composable
private fun FilaIconoTexto(icono: ImageVector, etiqueta: String, valor: String, tintIcono: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icono, contentDescription = null,
            tint = tintIcono, modifier = Modifier.size(18.dp))
        Column {
            Text(etiqueta, fontSize = 11.sp, fontFamily = FuenteInter,
                color = MaterialTheme.colorScheme.onSurface.copy(.5f))
            Text(valor, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                fontFamily = FuenteInter, color = Azul900)
        }
    }
}

@Composable
private fun ChipDato(icono: ImageVector, etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Blanco.copy(.7f), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icono, contentDescription = null,
                tint = Azul700, modifier = Modifier.size(13.dp))
            Column {
                Text(etiqueta, fontSize = 9.sp, fontFamily = FuenteInter,
                    color = MaterialTheme.colorScheme.onSurface.copy(.45f))
                Text(valor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = FuenteInter, color = Azul900)
            }
        }
    }
}
