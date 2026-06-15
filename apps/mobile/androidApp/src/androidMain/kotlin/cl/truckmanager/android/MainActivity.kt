package cl.truckmanager.android

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import cl.truckmanager.android.ui.home.PantallaInicio
import cl.truckmanager.android.ui.login.PantallaLogin
import cl.truckmanager.android.ui.qrscan.PantallaEscaneoQr
import cl.truckmanager.android.ui.theme.TemaTruckManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

private const val PREFS     = "tm_sesion"
private const val KEY_COND  = "conductorId"
private const val KEY_NOMBRE = "nombreConductor"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Leer sesión persistida (si existe) para restaurar sin login
        val prefs         = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val condIdGuardado = prefs.getString(KEY_COND, "") ?: ""
        val nombreGuardado = prefs.getString(KEY_NOMBRE, "") ?: ""

        setContent {
            TemaTruckManager {
                NavegacionApp(
                    conductorIdInicial  = condIdGuardado,
                    nombreConductorInicial = nombreGuardado,
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NavegacionApp(
    conductorIdInicial:     String = "",
    nombreConductorInicial: String = "",
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current

    // Si hay conductorId guardado, arrancar directamente en inicio
    var pantalla           by remember { mutableStateOf(if (conductorIdInicial.isNotBlank()) "inicio" else "login") }
    var conductorId        by remember { mutableStateOf(conductorIdInicial) }
    var nombreConductor    by remember { mutableStateOf(nombreConductorInicial) }
    var vehiculoEsperadoId by remember { mutableStateOf("") }
    var placaConfirmada    by remember { mutableStateOf("") }

    val permisosGps = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    when (pantalla) {

        "login" -> PantallaLogin(
            alIniciarSesion = { cId, nombre, _ ->
                conductorId     = cId
                nombreConductor = nombre
                // Persistir para restaurar en futuros lanzamientos
                contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_COND, cId)
                    .putString(KEY_NOMBRE, nombre)
                    .apply()
                pantalla = "inicio"
            }
        )

        "inicio" -> {
            LaunchedEffect(Unit) {
                if (!permisosGps.allPermissionsGranted) permisosGps.launchMultiplePermissionRequest()
            }
            PantallaInicio(
                idConductor     = conductorId,
                nombreConductor = nombreConductor,
                placaConfirmada = placaConfirmada,
                alEscanearQr    = { vId ->
                    vehiculoEsperadoId = vId
                    placaConfirmada    = ""
                    pantalla           = "escaneo_qr"
                },
                alCerrarSesion  = {
                    // Limpiar sesión persistida
                    contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
                    conductorId        = ""
                    nombreConductor    = ""
                    vehiculoEsperadoId = ""
                    placaConfirmada    = ""
                    pantalla           = "login"
                },
            )
        }

        "escaneo_qr" -> PantallaEscaneoQr(
            idConductor        = conductorId,
            vehiculoEsperadoId = vehiculoEsperadoId,
            alConfirmar        = { placa ->
                placaConfirmada = placa
                pantalla        = "inicio"
            },
        )
    }
}
