package mentat.music.com.mentapp.ui.screens.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mentat.music.com.mentapp.R
import mentat.music.com.mentapp.data.model.AppData
import mentat.music.com.mentapp.data.model.CarouselItem
import mentat.music.com.mentapp.ui.composables.AlbumCarousel
import mentat.music.com.mentapp.ui.composables.AttractorBackground
import mentat.music.com.mentapp.ui.composables.CircularDialLayout
import mentat.music.com.mentapp.ui.composables.DialItem
import mentat.music.com.mentapp.ui.composables.SolarisPlayButton
import mentat.music.com.mentapp.ui.composables.TRANSITION_DURATION
import mentat.music.com.mentapp.ui.composables.VideoBackground
import mentat.music.com.mentapp.ui.rememberVibrator
import mentat.music.com.mentapp.ui.screens.home.viewmodel.AppState
import mentat.music.com.mentapp.ui.screens.home.viewmodel.HomeViewModel
import mentat.music.com.mentapp.utils.MentatConstants
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.system.exitProcess

private val verdanaFontFamily = FontFamily(
    Font(R.font.verdana_regular, FontWeight.Normal),
    Font(R.font.verdana_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.verdana_bold, FontWeight.Bold),
    Font(R.font.verdana_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

@SuppressLint("LocalContextResourcesRead")
@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    val savedRotationAngle by homeViewModel.rotationAngle.collectAsState()
    val appState by homeViewModel.appState.collectAsState()
    val isAnimatingOut by homeViewModel.isAnimatingOut.collectAsState()
    val clickedIconIndex by homeViewModel.clickedIconIndex.collectAsState()
    val isExpansionFinished by homeViewModel.isExpansionFinished.collectAsState()
    val newsPosts by homeViewModel.newsPosts.collectAsState()

    // --- ESTADOS MINI DIAL ---
    val isWebMenuOpen by homeViewModel.isWebMenuOpen.collectAsState()
    val webMenuRotationAngle by homeViewModel.webMenuRotationAngle.collectAsState()

    // AJUSTE DE UX: Inicio rotado -30º (PI/6) para que el ítem 2 (Blog) quede abajo (90º)
    val startAngle = (-Math.PI / 6).toFloat()
    val webMenuRotationAnim = remember { Animatable(startAngle) }

    // VARIABLE PARA EFECTO FLIP (Moneda 3D)
    val dialFlipX = remember { Animatable(1f, Float.VectorConverter) }
    val dialBlur = remember { Animatable(0f) }

    // ESTADO PARA EL DIAL (True = Info, False = Audio)
    var isMainDial by remember { mutableStateOf(true) }
    // Ahora leemos la variable DEL VIEWMODEL
    val selectedWebCategory by homeViewModel.selectedWebCategory.collectAsState()

    val currentPage by homeViewModel.currentPage
    val rotationAngle = remember { Animatable(savedRotationAngle) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val dialScale = remember { Animatable(1.0f) }
    val vibrator = rememberVibrator()

    // --- SNAP PARA 6 ICONOS (DIAL GRANDE) ---
    val angleStep = (2 * Math.PI.toFloat() / 6)
    val targetAngleRad = (Math.PI.toFloat() / 2.0f)

    // --- Sincronizar Mini Dial ---
    LaunchedEffect(webMenuRotationAngle) {
        webMenuRotationAnim.snapTo(webMenuRotationAngle)
    }

    // --- REBOTE INICIAL ---
    val bounceSpec = spring<Float>(
        dampingRatio = 0.5f,
        stiffness = 150f
    )
    val bounceStartScale = 1.1f
    LaunchedEffect(Unit) {
        scope.launch {
            delay(100)
            dialScale.snapTo(bounceStartScale)
            dialScale.animateTo(targetValue = 1.0f, animationSpec = bounceSpec)
        }
    }

    // 1. RECOLECCIÓN DEL ESTADO
    val currentLanguage by homeViewModel.currentLanguage.collectAsState()
    val buttonText = if (currentLanguage == HomeViewModel.Language.ES) "EN" else "ES"

    // --- INMERSIVO ---
    val view = LocalView.current
    val window = (view.context as Activity).window
    LaunchedEffect(key1 = window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            view.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    // --- ANIMACIONES VISUALES ---
    val dialSceneAlpha by animateFloatAsState(
        targetValue = if (isExpansionFinished) 0f else 1f,
        animationSpec = tween(durationMillis = TRANSITION_DURATION),
        label = "dialSceneAlpha"
    )
    val arrowsAlpha by animateFloatAsState(
        targetValue = if (!isAnimatingOut) 0.4f else 0.0f,
        animationSpec = tween(300), label = "arrowsAlpha"
    )

    // --- TAMAÑOS ---
    val iconPathRadius = 140.dp
    val donutPadding = 8.dp
    val donutThickness = 76.dp + (donutPadding * 2)
    val donutRadius = iconPathRadius
    val radiusPx = with(LocalDensity.current) { donutRadius.toPx() }
    val thicknessPx = with(LocalDensity.current) { donutThickness.toPx() }
    val arrowsYOffset = iconPathRadius

    // --- SHADER ---
    val infiniteTransition = rememberInfiniteTransition(label = "shader time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 600000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "time"
    )
    var frozenTime by remember { mutableStateOf(0f) }

    // --- FUNCIONES AUXILIARES ---
    val context = LocalContext.current
    fun launchUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error link", Toast.LENGTH_SHORT).show()
        }
    }
    fun showComingSoon() {
        Toast.makeText(context, context.resources.getString(R.string.msg_coming_soon), Toast.LENGTH_SHORT).show()
    }

    fun activateExpansion(index: Int) {
        scope.launch {
            if (index != -1) homeViewModel.setSelectedWebCategory(null)
            vibrator.vibrateClick()
            frozenTime = time
            homeViewModel.updateIsAnimatingOut(true)
            homeViewModel.updateClickedIconIndex(index)
            homeViewModel.updateIsExpansionFinished(true)
        }
    }

    // =====================================================================
    // DEFINICIÓN DE LOS DIALES
    // =====================================================================

    // DIAL 1: REDES Y WEB (6 ÍTEMS)
    val itemsDial1 = remember {
        listOf(
            DialItem("Bluesky", "Bluesky", R.drawable.ic_menu_social, Color(0xFF0085FF)) {
                launchUrl(MentatConstants.URL_BLUESKY)
            },
            DialItem("YouTube", "YouTube", R.drawable.ic_menu_youtube, Color(0xFFFF0000)) {
                activateExpansion(1)
            },
            DialItem("Spotify", "Spotify", R.drawable.ic_menu_streams, Color(0xFF1DB954)) {
                activateExpansion(2)
            },
            DialItem("Bandcamp", "Bandcamp", R.drawable.ic_menu_bandcamp, Color(0xFF629AA9)) {
                activateExpansion(3)
            },
            DialItem("SoundCloud", "SoundCloud", R.drawable.ic_menu_soundcloud, Color(0xFFFF5500)) {
                activateExpansion(4)
            },
            DialItem("Web", "Mundo Web", R.drawable.ic_web_foreground, Color(0xFF893471)) {
                scope.launch {
                    homeViewModel.setWebMenuOpen(true)
                    vibrator.vibrateClick()
                }
            }
        )
    }

    // DIAL 2: CONTENIDO INTERNO (6 ÍTEMS)
    val itemsDial2 = remember {
        listOf(
            DialItem("GUZZ", "GUZZ", R.drawable.ic_menu_guzz, Color.White) {
                activateExpansion(0)
            },
            DialItem("DJSessions", "DJ Sessions", Icons.Default.List, Color(0xFF9C27B0)) {
                launchUrl(MentatConstants.URL_DJ_SESSIONS)
            },
            DialItem("Subs", "Suscriptores", Icons.Default.Lock, Color(0xFFFFD700)) {
                showComingSoon()
            },
            DialItem("Archive", "Archivo", R.drawable.ic_menu_concept, Color(0xFF8A2BE2)) {
                launchUrl(MentatConstants.URL_BLOG_OLD)
            },
            DialItem("Contact", "Contacto", Icons.Default.Email, Color(0xFF4CAF50)) {
                launchUrl("mailto:info@mentat-music.com")
            },
            DialItem("Live", "Directo", Icons.Default.LocationOn, Color(0xFFE91E63)) {
                showComingSoon()
            }
        )
    }

    // DIAL 3: MINI DIAL WEB (3 ÍTEMS)
    val itemsWebMenu = remember {
        listOf(
            DialItem("Audio", "Tutoriales", Icons.Default.Call, Color(0xFF000000)) {
                homeViewModel.filterByCategory("Audio")
                homeViewModel.setSelectedWebCategory("Audio")
                homeViewModel.setWebMenuOpen(false)
                activateExpansion(-1)
            },
            DialItem("Divulgacion", "Ciencia", Icons.Default.Star, Color(0xFF000000)) {
                homeViewModel.filterByCategory("Divulgacion")
                homeViewModel.setSelectedWebCategory("Divulgacion")
                homeViewModel.setWebMenuOpen(false)
                activateExpansion(-1)
            },
            DialItem("Blog", "Blog", Icons.Default.Create, Color(0xFF000000)) {
                homeViewModel.filterByCategory("Blog")
                homeViewModel.setSelectedWebCategory("Blog")
                homeViewModel.setWebMenuOpen(false)
                activateExpansion(-1)
            }
        )
    }

    val currentItems = if (isMainDial) itemsDial1 else itemsDial2

    // --- BACK HANDLER 1: Salir del Carrusel/Contenido ---
    BackHandler(enabled = isAnimatingOut || isExpansionFinished) {
        scope.launch {
            // A) DETECTAMOS SI VENÍAMOS DE LA WEB
            val wasWebMode = selectedWebCategory != null

            // B) RESETEAMOS ESTADOS VISUALES
            homeViewModel.updateIsExpansionFinished(false)
            homeViewModel.updateIsAnimatingOut(false)

            // C) LIMPIAMOS LA SELECCIÓN
            homeViewModel.setSelectedWebCategory(null)

            // --- CORRECCIÓN UX: REAPARICIÓN TEMPRANA ---
            if (wasWebMode) {
                launch {
                    delay(300)
                    homeViewModel.setWebMenuOpen(true)
                }
            }

            // D) ANIMACIÓN DEL DIAL GRANDE
            val impactTime = (TRANSITION_DURATION - 150).coerceAtLeast(0)
            delay(impactTime.toLong())

            dialScale.animateTo(
                targetValue = 0.92f,
                animationSpec = tween(120, easing = FastOutLinearInEasing)
            )

            dialScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessVeryLow)
            )

            homeViewModel.updateClickedIconIndex(-1)
            rotationAngle.snapTo(homeViewModel.rotationAngle.value)
        }
    }

    // --- BACK HANDLER 2: Cerrar menú si no he entrado en nada ---
    BackHandler(enabled = isWebMenuOpen) {
        homeViewModel.setWebMenuOpen(false)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // --- CAPA 1: FONDO ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AttractorBackground(
                modifier = Modifier.fillMaxSize(),
                isFrozen = isAnimatingOut || isExpansionFinished,
                frozenTime = frozenTime,
                isBlueMode = !isMainDial
            )
        } else {
            VideoBackground(modifier = Modifier.fillMaxSize())
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // AQUI ESTA LA DEFINICIÓN QUE FALTABA
            val isPortrait = maxWidth < maxHeight

            // --- CONTENEDOR PRINCIPAL ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(dialSceneAlpha)
                    // AQUÍ ESTÁ EL FIX DEL BLOQUEO: VIGILAMOS isWebMenuOpen
                    .pointerInput(clickedIconIndex, isAnimatingOut, isExpansionFinished, isWebMenuOpen) {
                        if (isAnimatingOut || isExpansionFinished || isWebMenuOpen) return@pointerInput
                        var centerX = 0f
                        var centerY = 0f
                        detectDragGestures(
                            onDragStart = {
                                centerX = size.width / 2f
                                centerY = size.height / 2f
                                scope.launch { rotationAngle.stop() }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val startAngle = atan2(change.previousPosition.y - centerY, change.previousPosition.x - centerX)
                                val endAngle = atan2(change.position.y - centerY, change.position.x - centerX)
                                scope.launch { rotationAngle.snapTo(rotationAngle.value + (endAngle - startAngle)) }
                            },
                            onDragEnd = {
                                val currentOffset = rotationAngle.value - targetAngleRad
                                val nearestIconIndex = -(currentOffset / angleStep).roundToInt()
                                val targetSnapAngle = targetAngleRad - (angleStep * nearestIconIndex)
                                scope.launch {
                                    rotationAngle.animateTo(targetSnapAngle, spring(0.7f, 100f))
                                    homeViewModel.updateRotationAngle(targetSnapAngle)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // TÍTULO
                Text(
                    text = stringResource(R.string.dial_title),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 22.sp,
                    fontFamily = verdanaFontFamily,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .align(if (isPortrait) Alignment.TopCenter else Alignment.TopStart)
                        .padding(32.dp)
                )

                // BOTONES SUPERIORES
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).size(48.dp)
                        .clickable {
                            vibrator.vibrateClick()
                            val activity = context as? Activity
                            if (activity != null) { activity.finishAndRemoveTask(); exitProcess(0) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(painter = painterResource(id = R.drawable.ic_power), contentDescription = "Salir", colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f)), modifier = Modifier.size(28.dp))
                }
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 35.dp, end = 80.dp)
                        .clickable { homeViewModel.toggleLanguage(); vibrator.vibrateClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = buttonText, color = Color.White.copy(alpha = 0.6f), fontSize = 20.sp, fontFamily = verdanaFontFamily, fontWeight = FontWeight.Bold)
                }

                // =========================================================
                // INICIO DEL SÁNDWICH DE CAPAS (LA SOLUCIÓN VISUAL)
                // =========================================================

                // CAPA A: EL DIAL GRANDE (FONDO)
                // Se escala y se desenfoca cuando gira la moneda
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(dialScale.value) // <--- ESTE SE ENCOGE
                        .blur(if (dialBlur.value > 0f) dialBlur.value.dp else 0.dp)
                        .graphicsLayer {
                            scaleX = dialScale.value * dialFlipX.value
                            scaleY = dialScale.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 1. Círculos de fondo
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gradientColors = listOf(
                            Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.4f),
                            Color.Gray.copy(alpha = 0.6f), Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.95f)
                        )
                        val brush = Brush.sweepGradient(colors = gradientColors, center = this.center)
                        drawCircle(brush = brush, radius = radiusPx, style = Stroke(width = thicknessPx))
                        drawCircle(color = Color.White.copy(alpha = 0.8f), radius = radiusPx - (thicknessPx / 2), style = Stroke(width = 1.5.dp.toPx()))
                        drawCircle(color = Color.White.copy(alpha = 0.5f), radius = radiusPx + (thicknessPx / 2), style = Stroke(width = 2.dp.toPx()))
                    }

                    // 2. Flechas indicadoras
                    Row(
                        modifier = Modifier.align(Alignment.Center).offset(y = arrowsYOffset).alpha(arrowsAlpha),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painter = painterResource(id = R.drawable.outline_line_start_arrow_notch_24), contentDescription = null, colorFilter = ColorFilter.tint(Color.Black), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(80.dp))
                        Image(painter = painterResource(id = R.drawable.outline_line_end_arrow_notch_24), contentDescription = null, colorFilter = ColorFilter.tint(Color.Black), modifier = Modifier.size(24.dp))
                    }

                    // 3. LA RUEDA (CircularDialLayout)
                    CircularDialLayout(
                        modifier = Modifier.fillMaxSize(),
                        items = currentItems,
                        currentRotation = rotationAngle.value,
                        iconPathRadius = iconPathRadius,
                        isAnimatingOut = isAnimatingOut,
                        clickedIconIndex = clickedIconIndex,
                        isExpansionFinished = isExpansionFinished
                    )
                }

                // CAPA B: EL DIMMER (OSCURIDAD)
                // ESTE ES EL TRUCO: Está FUERA del scale, así que siempre cubre toda la pantalla.
                val dimmerAlpha by animateFloatAsState(
                    targetValue = if (isWebMenuOpen) 0.6f else 0f,
                    animationSpec = tween(300),
                    label = "dimmerAlpha"
                )

                if (isWebMenuOpen || dimmerAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = dimmerAlpha))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                homeViewModel.setWebMenuOpen(false)
                            }
                    )
                }

                // CAPA C: MINI DIAL Y BOTÓN CENTRAL
                // Estos SÍ se escalan para acompañar al Dial Grande en el rebote
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(dialScale.value) // <--- ESTE SE ENCOGE TAMBIÉN
                        .blur(if (dialBlur.value > 0f) dialBlur.value.dp else 0.dp)
                        .graphicsLayer {
                            scaleX = dialScale.value * dialFlipX.value
                            scaleY = dialScale.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // A) EL MINI DIAL (SATÉLITES)
                    val miniDialScale by animateFloatAsState(
                        targetValue = if (isWebMenuOpen) 1f else 0f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
                        label = "miniDialScale"
                    )

                    val miniRadius = 95.dp
                    val miniThickness = 55.dp
                    val miniRadiusPx = with(LocalDensity.current) { miniRadius.toPx() }
                    val miniThicknessPx = with(LocalDensity.current) { miniThickness.toPx() }

                    if (isWebMenuOpen || miniDialScale > 0.1f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(y = 140.dp)
                                .scale(miniDialScale),
                            contentAlignment = Alignment.Center
                        ) {
                            // 1. EL FONDO DE CRISTAL
                            Canvas(modifier = Modifier.size((miniRadius * 2) + miniThickness)) {
                                val gradientColors = listOf(
                                    Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.2f),
                                    Color.Gray.copy(alpha = 0.5f), Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.95f)
                                )
                                val brush = Brush.sweepGradient(colors = gradientColors, center = this.center)
                                drawCircle(brush = brush, radius = miniRadiusPx, style = Stroke(width = miniThicknessPx))
                                drawCircle(color = Color.White.copy(alpha = 0.6f), radius = miniRadiusPx - (miniThicknessPx / 2), style = Stroke(width = 1.dp.toPx()))
                                drawCircle(color = Color.White.copy(alpha = 0.3f), radius = miniRadiusPx + (miniThicknessPx / 2), style = Stroke(width = 1.dp.toPx()))
                            }

                            // 2. LOS ICONOS Y EL GESTO
                            // TRUCO SIMPLE: Definimos el "ContentColor" local como MORADO.
                            // El Ripple usará este color.
                            CompositionLocalProvider(LocalContentColor provides Color(0xFF893471)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            val stepRad = (2 * Math.PI / 3).toFloat()
                                            val targetBase = (Math.PI / 2).toFloat()

                                            detectDragGestures(
                                                onDragStart = {
                                                    scope.launch { webMenuRotationAnim.stop() }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val rotationChange = (dragAmount.x / 350) * -1f
                                                    scope.launch {
                                                        webMenuRotationAnim.snapTo(webMenuRotationAnim.value + rotationChange)
                                                    }
                                                },
                                                onDragEnd = {
                                                    val currentRot = webMenuRotationAnim.value
                                                    val relativeRot = currentRot - targetBase
                                                    val steps = (relativeRot / stepRad).roundToInt()
                                                    val targetSnapAngle = (steps * stepRad) + targetBase

                                                    scope.launch {
                                                        webMenuRotationAnim.animateTo(
                                                            targetValue = targetSnapAngle,
                                                            animationSpec = spring(dampingRatio = 0.95f, stiffness = 40f)
                                                        )
                                                        homeViewModel.updateWebMenuRotationAngle(targetSnapAngle)
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularDialLayout(
                                        modifier = Modifier.fillMaxSize(),
                                        items = itemsWebMenu,
                                        currentRotation = webMenuRotationAnim.value,
                                        iconPathRadius = miniRadius,
                                        isAnimatingOut = false,
                                        clickedIconIndex = -1,
                                        isExpansionFinished = false
                                    )
                                }
                            }

                            // 3. EL INTERRUPTOR (CON EFECTO RADAR)
                            val infiniteTransition = rememberInfiniteTransition(label = "radar")
                            val radarScale by infiniteTransition.animateFloat(
                                initialValue = 1.0f, targetValue = 1.4f,
                                animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "scale"
                            )
                            val radarAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f, targetValue = 0.0f,
                                animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "alpha"
                            )

                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        homeViewModel.setWebMenuOpen(false)
                                        vibrator.vibrateClick()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = Color.White.copy(alpha = radarAlpha),
                                        radius = (size.minDimension / 2) * 0.6f * radarScale,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                                Image(
                                    painter = painterResource(id = R.drawable.ic_web_foreground),
                                    contentDescription = "Cerrar",
                                    colorFilter = ColorFilter.tint(Color.Black),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // B) EL BOTÓN CENTRAL (Y SU ESCUDO)
                    if (!isAnimatingOut && !isExpansionFinished) {
                        // El botón visual
                        SolarisPlayButton(
                            size = 80.dp,
                            onClick = {
                                scope.launch {
                                    launch { dialFlipX.animateTo(0.0f, tween(250, easing = FastOutLinearInEasing)) }
                                    launch { dialBlur.animateTo(10f, tween(250, easing = LinearEasing)) }
                                    delay(250)
                                    isMainDial = !isMainDial
                                    vibrator.vibrateClick()
                                    launch { dialFlipX.animateTo(1.0f, spring(0.7f, Spring.StiffnessMediumLow)) }
                                    launch { dialBlur.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
                                    launch {
                                        dialScale.snapTo(1.15f)
                                        dialScale.animateTo(1.0f, animationSpec = bounceSpec)
                                    }
                                }
                            }
                        )

                        // Escudo antimagia (si el menú está abierto)
                        if (isWebMenuOpen) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                            )
                        }
                    }
                }
            }

            // --- CAPA CARRUSEL (RESULTADOS) ---
            val appData: AppData? = remember(appState) {
                when (val state = appState) {
                    is AppState.Success -> state.data
                    is AppState.Loading -> null
                    is AppState.Error -> { Log.e("Home", "Error: ${state.message}"); null }
                }
            }

            // 1. Lógica de ID: Si hay selección Web, úsala. Si no, usa el Dial normal.
            val clickedItemId = selectedWebCategory ?: if (clickedIconIndex != -1 && clickedIconIndex < currentItems.size)
                currentItems[clickedIconIndex].id else null

            var carouselData: List<CarouselItem>? = null
            var conceptDataAsCarousel: List<CarouselItem>? = null

            if (appData != null && clickedItemId != null) {
                when (clickedItemId) {
                    "GUZZ" -> carouselData = appData.GUZZ
                    "Spotify" -> carouselData = appData.Spotify
                    "Bandcamp" -> carouselData = appData.Bandcamp
                    "SoundCloud" -> carouselData = appData.Soundcloud
                    "YouTube" -> {
                        carouselData = appData.YouTube?.map { item ->
                            item.copy(imageUrl = "https://img.youtube.com/vi/${item.imageUrl}/0.jpg")
                        }
                    }
                    "Audio", "Divulgacion", "Blog" -> {
                        conceptDataAsCarousel = newsPosts.map { entity ->
                            val plainText = android.text.Html.fromHtml(entity.content ?: "", android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                                .replace("\uFFFC", "").replace("\n", " ").trim()
                            val snippet = if (plainText.length > 200) plainText.take(200).substringBeforeLast(" ") + "..." else plainText

                            CarouselItem(
                                title = entity.title,
                                imageUrl = entity.imageUrl,
                                targetUrl = entity.targetUrl,
                                artist = snippet
                            )
                        }
                    }
                }
            }

            val carouselLayerTargetAlpha = when {
                isExpansionFinished && (appState is AppState.Loading || carouselData != null || conceptDataAsCarousel != null) -> 1f
                else -> 0f
            }
            val carouselLayerAnimatedAlpha by animateFloatAsState(
                targetValue = carouselLayerTargetAlpha,
                animationSpec = tween(durationMillis = TRANSITION_DURATION),
                label = "carouselLayerAlpha"
            )

            val brandColor = if (selectedWebCategory != null) Color.Black else (
                    if (clickedIconIndex != -1 && clickedIconIndex < currentItems.size)
                        currentItems[clickedIconIndex].color else Color.Transparent
                    )

            val isConceptMode = (clickedItemId in listOf("Audio", "Divulgacion", "Blog"))

            val carouselBoxModifier = if (isPortrait) {
                if (isConceptMode) Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f)
                else Modifier.fillMaxWidth(0.9f).aspectRatio(1f)
            } else {
                if (isConceptMode) Modifier.fillMaxHeight(0.9f).fillMaxWidth(0.7f)
                else Modifier.fillMaxHeight(0.9f).aspectRatio(1f)
            }

            if (isExpansionFinished) {
                Box(
                    modifier = Modifier
                        .alpha(carouselLayerAnimatedAlpha)
                        .then(carouselBoxModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                            .background(Brush.linearGradient(listOf(brandColor.copy(alpha = 0.6f), brandColor.copy(alpha = 0.3f))))
                            .border(3.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), Color.Gray.copy(alpha = 0.3f), Color.White.copy(alpha = 0.9f))), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val itemsToShow = carouselData ?: conceptDataAsCarousel
                        val safeInitialPage = if (itemsToShow.isNullOrEmpty()) 0 else currentPage
                        val itemSize = itemsToShow?.size ?: 0

                        if (itemsToShow != null) {
                            key(safeInitialPage to itemSize) {
                                AlbumCarousel(
                                    items = itemsToShow,
                                    navController = navController,
                                    isConceptMode = isConceptMode,
                                    initialPage = safeInitialPage,
                                    onPageChanged = homeViewModel::setCurrentPage
                                )
                            }
                        } else if (appState is AppState.Loading) {
                            CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f), strokeWidth = 3.dp)
                        } else if (appState is AppState.Error) {
                            Text("Error cargar datos.", color = Color.White, textAlign = TextAlign.Center, fontFamily = verdanaFontFamily)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}