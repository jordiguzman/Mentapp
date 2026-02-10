package mentat.music.com.mentapp.ui.screens.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object DialConstants {
    // --- DIMENSIONES FÍSICAS ---
    val ICON_PATH_RADIUS = 140.dp
    val DONUT_THICKNESS = 76.dp // Grosor base
    val DONUT_PADDING = 8.dp
    val TOTAL_DONUT_THICKNESS = DONUT_THICKNESS + (DONUT_PADDING * 2)

    // Dimensiones del Menú Web
    val WEB_MENU_RADIUS = 95.dp
    val WEB_MENU_STROKE = 55.dp
    val WEB_MENU_OFFSET_Y = 140.dp

    // --- MATEMÁTICAS Y ÁNGULOS ---
    const val ITEMS_COUNT = 6
    // 2 * PI / 6 = 60 grados por ítem
    const val ANGLE_STEP = (2 * Math.PI / ITEMS_COUNT).toFloat()
    // PI / 2 = 90 grados (posición de las 12 en punto)
    const val TARGET_ANGLE_RAD = (Math.PI / 2.0).toFloat()
    // Ángulo inicial (Bandcamp a la derecha/izquierda según se mire)
    const val START_ANGLE = (-Math.PI / 2).toFloat()

    // --- TIEMPOS DE ANIMACIÓN (ms) ---
    const val TRANSITION_DURATION = 600
    const val FLIP_DURATION = 250
    const val SPRING_STIFFNESS = 150f
    const val SPRING_DAMPING = 0.7f

    // --- COLORES ---
    val COLOR_WEB_MENU = Color(0xFF893471) // Púrpura Mentat
    val COLOR_DIMMER = Color.Black.copy(alpha = 0.6f)

    // Gradiente metálico del Dial Principal
    val DIAL_GRADIENT = listOf(
        Color.White.copy(0.95f),
        Color.White.copy(0.4f),
        Color.Gray.copy(0.6f),
        Color.White.copy(0.4f),
        Color.White.copy(0.95f)
    )

    val WEB_MENU_GRADIENT = listOf(
        Color.White.copy(0.95f),
        Color.White.copy(0.2f),
        Color.Gray.copy(0.5f),
        Color.White.copy(0.2f),
        Color.White.copy(0.95f)
    )
}