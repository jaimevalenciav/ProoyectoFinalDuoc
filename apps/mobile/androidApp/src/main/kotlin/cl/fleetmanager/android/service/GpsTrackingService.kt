package cl.fleetmanager.android.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cl.fleetmanager.android.MainActivity
import cl.fleetmanager.shared.usecase.CasoUsoRastreo
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject

class ServicioRastreoGps : Service() {

    private val casoUsoRastreo: CasoUsoRastreo by inject()
    private val alcanceServicio = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var clienteUbicacion: FusedLocationProviderClient
    private lateinit var callbackUbicacion: LocationCallback

    var idConductor: String = ""
    var idVehiculo:  String = ""

    companion object {
        const val ID_CANAL       = "rastreo_gps"
        const val ID_NOTIF       = 1001
        const val EXTRA_CONDUCTOR = "idConductor"
        const val EXTRA_VEHICULO  = "idVehiculo"
        const val INTERVALO_MS    = 30_000L
    }

    override fun onCreate() {
        super.onCreate()
        clienteUbicacion = LocationServices.getFusedLocationProviderClient(this)
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        idConductor = intent?.getStringExtra(EXTRA_CONDUCTOR) ?: ""
        idVehiculo  = intent?.getStringExtra(EXTRA_VEHICULO)  ?: ""
        startForeground(ID_NOTIF, construirNotificacion())
        iniciarActualizacionesUbicacion()
        return START_STICKY
    }

    private fun iniciarActualizacionesUbicacion() {
        val solicitud = LocationRequest.Builder(INTERVALO_MS)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateDistanceMeters(10f)
            .build()

        callbackUbicacion = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                val ubicacion = resultado.lastLocation ?: return
                alcanceServicio.launch {
                    casoUsoRastreo.rastrear(
                        idConductor = idConductor,
                        idVehiculo  = idVehiculo,
                        latitud     = ubicacion.latitude,
                        longitud    = ubicacion.longitude,
                        velocidad   = ubicacion.speed.toDouble(),
                        precision   = ubicacion.accuracy.toDouble(),
                    )
                }
            }
        }

        try {
            clienteUbicacion.requestLocationUpdates(solicitud, callbackUbicacion, mainLooper)
        } catch (_: SecurityException) {}
    }

    override fun onDestroy() {
        clienteUbicacion.removeLocationUpdates(callbackUbicacion)
        alcanceServicio.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun construirNotificacion(): Notification {
        val intentPendiente = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, ID_CANAL)
            .setContentTitle("FleetManager — GPS activo")
            .setContentText("Enviando posición cada 30 segundos")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(intentPendiente)
            .setOngoing(true)
            .build()
    }

    private fun crearCanalNotificacion() {
        val canal = NotificationChannel(ID_CANAL, "Rastreo GPS", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }
}
