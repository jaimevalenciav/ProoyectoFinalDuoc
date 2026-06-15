package cl.truckmanager.android.ui.login

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.truckmanager.android.ui.theme.Azul50
import cl.truckmanager.android.ui.theme.Azul700
import cl.truckmanager.android.ui.theme.Azul900
import cl.truckmanager.android.ui.theme.Blanco
import cl.truckmanager.android.ui.theme.FuenteInter
import cl.truckmanager.android.viewmodel.EstadoLogin
import cl.truckmanager.android.viewmodel.LoginViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PantallaLogin(
    alIniciarSesion: (conductorId: String, nombreConductor: String, token: String) -> Unit,
    modeloVista: LoginViewModel = koinViewModel(),
) {
    val estado by modeloVista.estado.collectAsState()
    val contexto = LocalContext.current

    LaunchedEffect(estado) {
        if (estado is EstadoLogin.Exitoso) {
            val s = estado as EstadoLogin.Exitoso
            alIniciarSesion(s.conductorId, s.nombreConductor, s.tokenAcceso)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Azul50),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(.88f)
                .background(Blanco, RoundedCornerShape(24.dp))
                .padding(horizontal = 32.dp, vertical = 40.dp),
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(Azul700, Azul900)),
                        shape = RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("🚛", fontSize = 36.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "TruckManager",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FuenteInter,
                color = Azul900,
            )
            Text(
                "App del conductor",
                fontSize = 14.sp,
                fontFamily = FuenteInter,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
            )

            Spacer(Modifier.height(36.dp))

            when (estado) {
                is EstadoLogin.Cargando ->
                    CircularProgressIndicator(color = Azul700, strokeWidth = 2.dp)

                is EstadoLogin.Error ->
                    Text(
                        (estado as EstadoLogin.Error).mensaje,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontFamily = FuenteInter,
                    )

                else -> {}
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { modeloVista.iniciarSesion(contexto as Activity) },
                enabled = estado !is EstadoLogin.Cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Azul700,
                    contentColor = Blanco,
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            ) {
                Text(
                    "Iniciar sesión con Azure B2C",
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FuenteInter,
                    fontSize = 15.sp,
                )
            }
        }
    }
}
