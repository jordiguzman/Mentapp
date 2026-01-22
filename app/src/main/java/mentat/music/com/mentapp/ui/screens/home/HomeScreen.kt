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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

    // VARIABLE PARA EFECTO FLIP (Moneda 3D)
    val dialFlipX = remember { Animatable(1f, Float.VectorConverter) }
    val dialBlur = remember { Animatable(0f) }
    // ESTADO PARA EL DIAL (True = Info, False = Audio)
    var isMainDial by remember { mutableStateOf(true) }

    val currentPage by homeViewModel.currentPage
    val rotationAngle = remember { Animatable(savedRotationAngle) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val dialScale = remember { Animatable(1.0f) }
    val vibrator = rememberVibrator()

    // CONSTANTES DE ANIMACIÓN (Traídas de tu código original)
    val angleStep = (2 * Math.PI.toFloat() / 7) // Asumimos 7 items por dial
    val targetAngleRad = (Math.PI.toFloat() / 2.0f)

    // --- REBOTE ---
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
        Toast.makeText(context, context.getString(R.string.msg_coming_soon), Toast.LENGTH_SHORT).show()
    }

    // Función para activar la animación de expansión (Carrusel)
    fun activateExpansion(index: Int) {
        scope.launch {
            vibrator.vibrateClick()
            frozenTime = time
            homeViewModel.updateIsAnimatingOut(true)
            homeViewModel.updateClickedIconIndex(index)
            homeViewModel.updateIsExpansionFinished(true)
        }
    }

    // =====================================================================
    // DEFINICIÓN DE LOS DIALES (AQUÍ ESTÁ LA MAGIA)
    // =====================================================================

    // DIAL 1: INFO (Principal)
    val itemsDial1 = remember {
        listOf(
            DialItem("Bluesky", "Bluesky", R.drawable.ic_menu_social, Color(0xFF0085FF)) {
                launchUrl(MentatConstants.URL_BLUESKY)
            },
            DialItem("YouTube", "YouTube", R.drawable.ic_menu_youtube, Color(0xFFFF0000)) {
                launchUrl(MentatConstants.URL_YOUTUBE_CHANNEL)
            },
            DialItem("Spotify", "Spotify", R.drawable.ic_menu_streams, Color(0xFF1DB954)) {
                launchUrl(MentatConstants.URL_SPOTIFY_ARTIST)
            },
            DialItem("Bandcamp", "Bandcamp", R.drawable.ic_menu_bandcamp, Color(0xFF629AA9)) {
                launchUrl(MentatConstants.URL_BANDCAMP_LATEST)
            },
            // NUEVOS (Iconos del Sistema) -> Usan lógica de carrusel (Filtro Audio)
            DialItem("Audio", "Tutoriales", Icons.Default.Call, Color(0xFF000000)) {
                homeViewModel.filterByCategory("Audio")
                activateExpansion(4) // Índice manual en la lista
            },
            DialItem("Divulgacion", "Ciencia", Icons.Default.Star, Color(0xFF000000)) {
                homeViewModel.filterByCategory("Divulgacion")
                activateExpansion(5)
            },
            DialItem("Blog", "Blog", Icons.Default.Create, Color(0xFF000000)) {
                homeViewModel.filterByCategory("Blog")
                activateExpansion(6)
            }
        )
    }

    // DIAL 2: AUDIO (Pro)
    val itemsDial2 = remember {
        listOf(
            DialItem("SoundCloud", "SoundCloud", R.drawable.ic_menu_soundcloud, Color(0xFFFF5500)) {
                launchUrl(MentatConstants.URL_SOUNDCLOUD_LATEST)
            },
            DialItem("GUZZ", "GUZZ", R.drawable.ic_menu_guzz, Color.White) {
                homeViewModel.filterByCategory("GUZZ")
                activateExpansion(1)
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

    // Seleccionamos la lista activa
    val currentItems = if (isMainDial) itemsDial1 else itemsDial2

    // --- BACK HANDLER (Física en 2 Pasos: Hundimiento + Rebote) ---
    BackHandler(enabled = isAnimatingOut || isExpansionFinished) {
        scope.launch {
            homeViewModel.updateIsExpansionFinished(false)
            homeViewModel.updateIsAnimatingOut(false)

            // Sincronización (-150ms)
            // Esto hace que la animación empiece justo antes de que llegue el icono,
            // perfecto para que se vea el hundimiento mientras aterriza.
            val impactTime = (TRANSITION_DURATION - 150).coerceAtLeast(0)
            delay(impactTime.toLong())

            // --- FASE 1: EL IMPACTO (Hundimiento) ---
            // Antes usábamos snapTo (instantáneo). Ahora animamos la bajada.
            dialScale.animateTo(
                targetValue = 0.92f,
                animationSpec = tween(
                    durationMillis = 120, // Tarda un poco en hundirse (puedes subirlo a 150 si quieres más drama)
                    easing = FastOutLinearInEasing // Empieza rápido, frena de golpe al chocar
                )
            )

            // --- FASE 2: LA RECUPERACIÓN (La que te gusta) ---
            dialScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = 0.35f, // Gomoso
                    stiffness = Spring.StiffnessVeryLow // Lento y majestuoso
                )
            )

            // Limpieza final
            homeViewModel.updateClickedIconIndex(-1)
            rotationAngle.snapTo(homeViewModel.rotationAngle.value)
        }
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
            val isPortrait = maxWidth < maxHeight

            // --- CAPA DIAL ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(dialSceneAlpha)
                    .pointerInput(clickedIconIndex) {
                        if (isAnimatingOut || isExpansionFinished) return@pointerInput
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
                // TÍTULO MENTAPP
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

                // BOTONES ESQUINA SUPERIOR DERECHA (Power & Idioma)
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

                // CONTENEDOR DEL DIAL Y EL BOTÓN CENTRAL
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(dialScale.value)
                        .blur(if (dialBlur.value > 0f) dialBlur.value.dp else 0.dp)
                        .graphicsLayer {
                            // AQUÍ ESTÁ EL TRUCO VISUAL:
                            // Multiplicamos la escala X por nuestra variable de "giro de moneda"
                            // Esto lo pusiste tú y ESTÁ PERFECTO
                            scaleX = dialScale.value * dialFlipX.value

                            scaleY = dialScale.value
                            rotationZ = rotationAngle.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 1. Círculos de fondo (Canvas)
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

                    // 3. LA RUEDA (CircularDialLayout) -> Versión Híbrida
                    CircularDialLayout(
                        modifier = Modifier.fillMaxSize(),
                        items = currentItems,
                        currentRotation = rotationAngle.value,
                        iconPathRadius = iconPathRadius,
                        isAnimatingOut = isAnimatingOut,
                        clickedIconIndex = clickedIconIndex,
                        isExpansionFinished = isExpansionFinished
                    )

                    // 4. BOTÓN CENTRAL (Versión FINAL: Blur Progresivo 0 -> MAX -> 0)
                    if (!isAnimatingOut && !isExpansionFinished) {
                        SolarisPlayButton(
                            size = 80.dp,
                            onClick = {
                                scope.launch {
                                    // --- FASE 1: ACELERACIÓN (De 0 a Mitad) ---
                                    // Todo ocurre en 250ms

                                    // A) La moneda se cierra
                                    launch {
                                        dialFlipX.animateTo(
                                            targetValue = 0.0f,
                                            animationSpec = tween(250, easing = FastOutLinearInEasing)
                                        )
                                    }

                                    // B) El Blur SUBE progresivamente de 0 a 10
                                    // Usamos LinearEasing para que la subida sea constante y se note la velocidad
                                    launch {
                                        dialBlur.animateTo(
                                            targetValue = 10f, // Máximo blur justo en el medio
                                            animationSpec = tween(250, easing = LinearEasing)
                                        )
                                    }

                                    // Esperamos a que termine la Fase 1
                                    delay(250)

                                    // --- FASE 2: EL PICO (Cambio de datos) ---
                                    // Aquí estamos en el "ojo del huracán": Blur al máximo, moneda invisible
                                    isMainDial = !isMainDial
                                    vibrator.vibrateClick()

                                    // --- FASE 3: FRENADA (De Mitad a Final) ---

                                    // A) La moneda se abre
                                    launch {
                                        dialFlipX.animateTo(
                                            targetValue = 1.0f,
                                            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
                                        )
                                    }

                                    // B) El Blur BAJA progresivamente de 10 a 0
                                    // Desaparece a medida que la moneda recupera su forma
                                    launch {
                                        dialBlur.animateTo(
                                            targetValue = 0f, // Vuelve a estar nítido
                                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                                        )
                                    }

                                    // C) Golpe de peso (Bounce)
                                    launch {
                                        dialScale.snapTo(1.15f)
                                        dialScale.animateTo(1.0f, animationSpec = bounceSpec)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // --- CAPA CARRUSEL (Lógica actualizada) ---
            val appData: AppData? = remember(appState) {
                when (val state = appState) {
                    is AppState.Success -> state.data
                    is AppState.Loading -> null
                    is AppState.Error -> { Log.e("Home", "Error: ${state.message}"); null }
                }
            }

            val clickedItemId = if (clickedIconIndex != -1 && clickedIconIndex < currentItems.size)
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

                    // BLOQUES QUE USAN NOTICIAS (ROOM)
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

            val brandColor = if (clickedIconIndex != -1 && clickedIconIndex < currentItems.size)
                currentItems[clickedIconIndex].color else Color.Transparent

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