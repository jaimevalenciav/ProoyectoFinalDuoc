package cl.truckmanager.android.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Paleta azul/blanco
val Azul900    = Color(0xFF1E3A8A)
val Azul700    = Color(0xFF1D4ED8)
val Azul600    = Color(0xFF2563EB)
val Azul500    = Color(0xFF3B82F6)
val Azul100    = Color(0xFFDBEAFE)
val Azul50     = Color(0xFFEFF6FF)
val ColorExito   = Color(0xFF16A34A)
val ColorPeligro = Color(0xFFDC2626)
val ColorAdvertencia = Color(0xFFF59E0B)
val Blanco     = Color(0xFFFFFFFF)
val FondoApp   = Color(0xFFF0F6FF)

// Usando fuente del sistema — para producción agregar Inter en res/font/
val FuenteInter = FontFamily.Default

private val EsquemaColoresClaro = lightColorScheme(
    primary          = Azul700,
    onPrimary        = Blanco,
    primaryContainer = Azul100,
    onPrimaryContainer = Azul900,
    secondary        = Azul500,
    onSecondary      = Blanco,
    background       = FondoApp,
    surface          = Blanco,
    onBackground     = Azul900,
    onSurface        = Azul900,
    error            = ColorPeligro,
    outline          = Azul500,
)

@Composable
fun TemaTruckManager(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaColoresClaro,
        typography = Typography(
            bodyLarge    = MaterialTheme.typography.bodyLarge.copy(fontFamily = FuenteInter),
            bodyMedium   = MaterialTheme.typography.bodyMedium.copy(fontFamily = FuenteInter),
            bodySmall    = MaterialTheme.typography.bodySmall.copy(fontFamily = FuenteInter),
            titleLarge   = MaterialTheme.typography.titleLarge.copy(fontFamily = FuenteInter),
            titleMedium  = MaterialTheme.typography.titleMedium.copy(fontFamily = FuenteInter),
            labelLarge   = MaterialTheme.typography.labelLarge.copy(fontFamily = FuenteInter),
        ),
        content = contenido,
    )
}
