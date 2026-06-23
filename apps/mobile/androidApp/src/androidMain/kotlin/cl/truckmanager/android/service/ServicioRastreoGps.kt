package cl.truckmanager.android.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import cl.truckmanager.android.MainActivity
import cl.truckmanager.android.gps.GpsEstadoRepositorio
import cl.truckmanager.android.gps.UltimaUbicacion
import cl.truckmanager.shared.usecase.TrackingUseCase
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServicioRastreoGps : Service() {

    private val casoUsoRastreo:      TrackingUseCase       by inject()
    private val gpsEstadoRepositorio: GpsEstadoRepositorio by inject()
    private val alcanceServicio = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var clienteUbicacion: FusedLocationProviderClient
    private lateinit var callbackUbicacion: LocationCallback

    var idConductor: String = ""
    var idVehiculo:  String = ""

    companion object {
        private const val TAG           = "ServicioRastreoGps"
        const val ID_CANAL              = "rastreo_gps"
        const val ID_NOTIF              = 1001
        const val EXTRA_CONDUCTOR       = "idConductor"
        const val EXTRA_VEHICULO        = "idVehiculo"
        const val INTERVALO_MS          = 60_000L   // 60 s
        private val FMT_HORA = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }

    override fun onCreate() {
        super.onCreate()
        clienteUbicacion = LocationServices.getFusedLocationProviderClient(this)
        crearCanalNotificacion()
        Log.d(TAG, "Servicio creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        idConductor = intent?.getStringExtra(EXTRA_CONDUCTOR) ?: ""
        idVehiculo  = intent?.getStringExtra(EXTRA_VEHICULO)  ?: ""
        Log.d(TAG, "onStartCommand — conductor=$idConductor vehiculo=$idVehiculo")

        startForeground(ID_NOTIF, construirNotificacion("Obteniendo ubicación…"))

        // Enviar ubicación conocida de inmediato (no esperar primer tick)
        enviarUltimaUbicacionConocida()
        // Registrar callback periódico
        iniciarActualizacionesUbicacion()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun enviarUltimaUbicacionConocida() {
        Log.d(TAG, "Solicitando lastLocation…")
        clienteUbicacion.lastLocation
            .addOnSuccessListener { ubicacion ->
                if (ubicacion != null) {
                    Log.d(TAG, "lastLocation OK: ${ubicacion.latitude}, ${ubicacion.longitude}")
                    alcanceServicio.launch { enviarUbicacion(ubicacion) }
                } else {
                    Log.w(TAG, "lastLocation = null (sin caché de ubicación aún)")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "lastLocation falló: ${e.message}")
            }
    }

    @SuppressLint("MissingPermission")
    private fun iniciarActualizacionesUbicacion() {
        val solicitud = LocationRequest.Builder(INTERVALO_MS)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateDistanceMeters(0f)   // 0 = siempre reportar aunque no se mueva
            .build()

        callbackUbicacion = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                val ubicacion = resultado.lastLocation ?: return
                Log.d(TAG, "onLocationResult: ${ubicacion.latitude}, ${ubicacion.longitude}")
                alcanceServicio.launch { enviarUbicacion(ubicacion) }
            }
        }

        try {
            clienteUbicacion.requestLocationUpdates(solicitud, callbackUbicacion, mainLooper)
            Log.d(TAG, "requestLocationUpdates registrado (intervalo=${INTERVALO_MS}ms)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException en requestLocationUpdates: ${e.message}")
        }
    }

    private suspend fun enviarUbicacion(ubicacion: android.location.Location) {
        val horaEnvio  = FMT_HORA.format(Date())
        val latStr     = "%.6f".format(ubicacion.latitude)
        val lonStr     = "%.6f".format(ubicacion.longitude)
        Log.d(TAG, "Enviando al BFF — lat=$latStr lon=$lonStr conductor=$idConductor vehiculo=$idVehiculo")

        val resultado = casoUsoRastreo.track(
            conductorId = idConductor,
            vehiculoId  = idVehiculo,
            latitud     = ubicacion.latitude,
            longitud    = ubicacion.longitude,
            velocidad   = ubicacion.speed.toDouble(),
            precision   = ubicacion.accuracy.toDouble(),
        )

        val recibido    = resultado.isSuccess
        val error       = resultado.exceptionOrNull()?.message
        val respuesta   = resultado.getOrNull()?.let { "id=${it.id}" } ?: "Error: $error"

        Log.d(TAG, "Respuesta BFF — recibido=$recibido respuesta=$respuesta")

        gpsEstadoRepositorio.actualizar(
            UltimaUbicacion(
                latitud       = ubicacion.latitude,
                longitud      = ubicacion.longitude,
                velocidad     = ubicacion.speed.toDouble(),
                precision     = ubicacion.accuracy.toDouble(),
                horaEnvio     = horaEnvio,
                recibidoEnBff = recibido,
                errorMensaje  = error,
                respuestaBff  = respuesta,
            )
        )

        val texto = if (recibido) "✓ $latStr, $lonStr — $horaEnvio"
                    else          "⚠ Error al enviar — $horaEnvio"
        actualizarNotificacion(texto)
    }

    override fun onDestroy() {
        Log.d(TAG, "Servicio destruido")
        if (::callbackUbicacion.isInitialized)
            clienteUbicacion.removeLocationUpdates(callbackUbicacion)
        alcanceServicio.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun construirNotificacion(texto: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, ID_CANAL)
            .setContentTitle("TruckManager — GPS activo")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun actualizarNotificacion(texto: String) {
        getSystemService(NotificationManager::class.java)
            .notify(ID_NOTIF, construirNotificacion(texto))
    }

    private fun crearCanalNotificacion() {
        val canal = NotificationChannel(ID_CANAL, "Rastreo GPS", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }
}
