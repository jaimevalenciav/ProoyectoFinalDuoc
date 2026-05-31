package cl.fleetmanager.android.ui.qrscan

import android.Manifest
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import cl.fleetmanager.android.ui.theme.Azul700
import cl.fleetmanager.android.ui.theme.Azul900
import cl.fleetmanager.android.ui.theme.Blanco
import cl.fleetmanager.android.ui.theme.FuenteInter
import cl.fleetmanager.android.viewmodel.EstadoEscaneo
import cl.fleetmanager.android.viewmodel.QrScanViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.koin.androidx.compose.koinViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PantallaEscaneoQr(
    idConductor: String,
    alAsignar:   (idAsignacion: String, placaVehiculo: String) -> Unit,
    modeloVista: QrScanViewModel = koinViewModel(),
) {
    val estado by modeloVista.estado.collectAsState()
    val permisoCamara = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) { permisoCamara.launchPermissionRequest() }
    LaunchedEffect(estado) {
        if (estado is EstadoEscaneo.Asignado) {
            alAsignar((estado as EstadoEscaneo.Asignado).idAsignacion, "")
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (permisoCamara.status.isGranted) {
            VistaCamara(alDetectarQr = { modeloVista.alEscanearQr(it) })
        } else {
            Text(
                "Se requiere permiso de cámara",
                color = Blanco,
                fontFamily = FuenteInter,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Panel inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val s = estado) {
                is EstadoEscaneo.Escaneando ->
                    Text(
                        "Apunta al código QR del vehículo",
                        color = Blanco,
                        fontSize = 16.sp,
                        fontFamily = FuenteInter,
                    )

                is EstadoEscaneo.Cargando ->
                    CircularProgressIndicator(color = Azul700, strokeWidth = 2.dp)

                is EstadoEscaneo.Error -> {
                    Text(s.mensaje, color = MaterialTheme.colorScheme.error, fontFamily = FuenteInter)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { modeloVista.reiniciar() },
                        colors = ButtonDefaults.buttonColors(containerColor = Azul700, contentColor = Blanco),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Reintentar", fontFamily = FuenteInter)
                    }
                }

                is EstadoEscaneo.VehiculoDetectado ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Blanco),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "Vehículo detectado",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
                                fontSize = 13.sp,
                                fontFamily = FuenteInter,
                            )
                            Text(
                                "${s.vehiculo.placa} — ${s.vehiculo.marca} ${s.vehiculo.modelo}",
                                color = Azul900,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                fontFamily = FuenteInter,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { modeloVista.confirmarAsignacion(idConductor, s.vehiculo.vehiculoId) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Azul700, contentColor = Blanco),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Confirmar asignación", fontWeight = FontWeight.SemiBold, fontFamily = FuenteInter)
                            }
                            TextButton(onClick = { modeloVista.reiniciar() }) {
                                Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), fontFamily = FuenteInter)
                            }
                        }
                    }

                else -> {}
            }
        }
    }
}

@Composable
private fun VistaCamara(alDetectarQr: (String) -> Unit) {
    val contexto  = LocalContext.current
    val cicloVida = LocalLifecycleOwner.current
    val executor  = remember { Executors.newSingleThreadExecutor() }
    var ultimoEscaneado by remember { mutableStateOf("") }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val vistaPrevia = PreviewView(ctx)
            val futuro = ProcessCameraProvider.getInstance(ctx)
            futuro.addListener({
                val proveedor = futuro.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(vistaPrevia.surfaceProvider) }
                val opciones = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                val escaner  = BarcodeScanning.getClient(opciones)
                val analisis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analisis.setAnalyzer(executor) { proxy ->
                    val imagen = proxy.image
                    if (imagen != null) {
                        val imagenEntrada = InputImage.fromMediaImage(imagen, proxy.imageInfo.rotationDegrees)
                        escaner.process(imagenEntrada)
                            .addOnSuccessListener { codigos ->
                                codigos.firstOrNull()?.rawValue?.let { valor ->
                                    if (valor != ultimoEscaneado) {
                                        ultimoEscaneado = valor
                                        alDetectarQr(valor)
                                    }
                                }
                            }
                            .addOnCompleteListener { proxy.close() }
                    } else proxy.close()
                }
                proveedor.unbindAll()
                proveedor.bindToLifecycle(cicloVida, CameraSelector.DEFAULT_BACK_CAMERA, preview, analisis)
            }, ContextCompat.getMainExecutor(ctx))
            vistaPrevia
        },
    )
}
