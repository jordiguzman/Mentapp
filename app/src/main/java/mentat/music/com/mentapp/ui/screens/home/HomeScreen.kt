package mentat.music.com.mentapp.ui.screens.home

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
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
import mentat.music.com.mentapp.ui.composables.TRANSITION_DURATION
import mentat.music.com.mentapp.ui.composables.VideoBackground
import mentat.music.com.mentapp.ui.composables.angleStep
import mentat.music.com.mentapp.ui.composables.menuItems
import mentat.music.com.mentapp.ui.composables.targetAngleRad
import mentat.music.com.mentapp.ui.rememberVibrator
import mentat.music.com.mentapp.ui.screens.home.viewmodel.AppState
import mentat.music.com.mentapp.ui.screens.home.viewmodel.HomeViewModel
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



    // --- OBTENIENDO EL ESTADO DEL PAGINADOR ---
    val currentPage by homeViewModel.currentPage // <-- ¡NUEVO ESTADO!

    val rotationAngle = remember { Animatable(savedRotationAngle) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val dialScale = remember { Animatable(1.0f) }
    val vibrator = rememberVibrator()

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

    // --- INMERSIVO (Híbrido Moderno/Legacy) ---
    val view = LocalView.current
    val window = (view.context as Activity).window
    LaunchedEffect(key1 = window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        // FIX PARA API < 30 (Android 10 o menos)
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

    // --- BACK HANDLER ---
    BackHandler(enabled = isAnimatingOut || isExpansionFinished) {
        scope.launch {
            homeViewModel.updateIsExpansionFinished(false)
            homeViewModel.updateIsAnimatingOut(false)
            delay(TRANSITION_DURATION.toLong())
            scope.launch {
                dialScale.snapTo(bounceStartScale)
                dialScale.animateTo(targetValue = 1.0f, animationSpec = bounceSpec)
            }
            homeViewModel.updateClickedIconIndex(-1)
            rotationAngle.snapTo(homeViewModel.rotationAngle.value)
        }
    }

    // --- FILTRO OSCURO ---
    val desaturationFilter: ColorFilter = remember {
        val matrix = ColorMatrix(
            floatArrayOf(
                0.4f, 0f, 0f, 0f, 0f,
                0f, 0.4f, 0f, 0f, 0f,
                0f, 0f, 0.4f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        ColorFilter.colorMatrix(matrix)
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
                frozenTime = frozenTime
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
                // Título
                Text(
                    text = "MENTAPP",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 22.sp,
                    fontFamily = verdanaFontFamily,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .align(if (isPortrait) Alignment.TopCenter else Alignment.TopStart)
                        .padding(32.dp)
                )

                // Botón Salir (Power)
                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .size(48.dp)
                        .clickable {
                            vibrator.vibrateClick()
                            val activity = context as? Activity
                            if (activity != null) {
                                activity.finishAndRemoveTask()
                                exitProcess(0)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_power),
                        contentDescription = "Salir",
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f)),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd) // O TopStart, ajusta según el diseño
                        .padding(top = 35.dp, end = 80.dp)
                        .clickable {
                            // Llama a la función del ViewModel que dispara la recarga de datos
                            homeViewModel.toggleLanguage()
                            vibrator.vibrateClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 20.sp,
                        fontFamily = verdanaFontFamily,
                        fontWeight = FontWeight.Bold // Para destacar
                    )
                }

                // Contenedor del Dial
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(dialScale.value),
                    contentAlignment = Alignment.Center
                ) {
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
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = arrowsYOffset)
                            .alpha(arrowsAlpha),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painter = painterResource(id = R.drawable.outline_line_start_arrow_notch_24), contentDescription = null, colorFilter = ColorFilter.tint(Color.Black), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(80.dp))
                        Image(painter = painterResource(id = R.drawable.outline_line_end_arrow_notch_24), contentDescription = null, colorFilter = ColorFilter.tint(Color.Black), modifier = Modifier.size(24.dp))
                    }
                    CircularDialLayout(
                        modifier = Modifier.fillMaxSize(),
                        currentRotation = rotationAngle.value,
                        iconPathRadius = iconPathRadius,
                        isAnimatingOut = isAnimatingOut,
                        clickedIconIndex = clickedIconIndex,
                        isExpansionFinished = isExpansionFinished,
                        onIconClick = { route, index ->
                            if (!isAnimatingOut) {
                                scope.launch {
                                    val clickedItemName = menuItems[index].name.trim()
                                    vibrator.vibrateClick()
                                    // AJUSTE 1: Cambiamos "Concepto" por "Entradas"
                                    val carruselItems = listOf("GUZZ", "Spotify", "YouTube", "Entradas", "Bandcamp", "Soundcloud")
                                    if (clickedItemName in carruselItems) {
                                        frozenTime = time
                                        homeViewModel.updateIsAnimatingOut(true)
                                        homeViewModel.updateClickedIconIndex(index)
                                        homeViewModel.updateIsExpansionFinished(true)
                                    } else {
                                        frozenTime = time
                                        delay(100)
                                        uriHandler.openUri(route)
                                        delay(500)
                                    }
                                }
                            }
                        },
                        contentFor = { item, isClickedIcon, isExpansionFinished, isActive ->
                            val iconTargetAlpha = when {
                                isClickedIcon && isAnimatingOut -> 0f
                                else -> 1f
                            }
                            val iconAnimatedAlpha by animateFloatAsState(
                                targetValue = iconTargetAlpha,
                                animationSpec = tween(durationMillis = TRANSITION_DURATION),
                                label = "iconContentAlpha"
                            )
                            val myFilter = if (isActive || isAnimatingOut) {
                                null
                            } else {
                                remember {
                                    ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                                        0.35f, 0f, 0f, 0f, 70f, // Rojo mult 0.6 + offset 70
                                        0f, 0.35f, 0f, 0f, 70f, // Verde mult 0.6 + offset 70
                                        0f, 0f, 0.35f, 0f, 70f, // Azul mult 0.6 + offset 70
                                        0f, 0f, 0f, 1f, 0f     // Alpha intacto
                                    )))
                                }
                            }

                            Image(
                                painter = painterResource(id = item.iconResId),
                                contentDescription = item.name,
                                contentScale = ContentScale.Fit,
                                colorFilter = myFilter, // <--- Matriz aplicada aquí
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(iconAnimatedAlpha)
                            )
                        }
                    )
                }
            }


            // --- CAPA CARRUSEL ---
            val appData: AppData? = remember(appState) {
                when (val state = appState) {
                    is AppState.Success -> state.data
                    is AppState.Loading -> null
                    is AppState.Error -> { Log.e("Home", "Error: ${state.message}"); null }
                }
            }
            val clickedItemName = if (clickedIconIndex != -1) menuItems[clickedIconIndex].name else null
            var carouselData: List<CarouselItem>? = null
            var conceptDataAsCarousel: List<CarouselItem>? = null

            if (appData != null && clickedItemName != null) {
                when (clickedItemName) {
                    // 1. GUZZ VUELVE A SU SITIO (JSON)
                    "GUZZ" -> carouselData = appData.GUZZ

                    "Spotify" -> carouselData = appData.Spotify
                    "Bandcamp" -> carouselData = appData.Bandcamp
                    "Soundcloud" -> carouselData = appData.Soundcloud
                    "YouTube" -> {
                        carouselData = appData.YouTube?.map { item ->
                            item.copy(imageUrl = "https://img.youtube.com/vi/${item.imageUrl}/0.jpg")
                        }
                    }

                    // 2. LAS NOTICIAS SE VAN A "ENTRADAS" (ROOM / RSS)
                    // 2. LAS NOTICIAS SE VAN A "ENTRADAS" (ROOM / RSS)
                    "Entradas" -> {
                        conceptDataAsCarousel = newsPosts.map { entity ->
                            // LIMPIEZA DE HTML:
                            // 1. Html.fromHtml quita las etiquetas (<p>, <b>, etc).
                            // 2. toString() lo convierte a texto.
                            // 3. replace(...) quita saltos de línea extra para que quede compacto.
                            // 4. take(150) coge solo los primeros 150 caracteres.
                            val plainText = android.text.Html.fromHtml(entity.content, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                                .replace("\uFFFC", "")
                                .replace("\n", " ")
                                .trim()

                            // Si el texto es muy largo, añadimos "..." al final
                            val maxLength = 200
                            val snippet = if (plainText.length > maxLength) {
                                plainText.take(maxLength).substringBeforeLast(" ") + "..."
                            } else {
                                plainText
                            }

                            CarouselItem(
                                title = entity.title,
                                imageUrl = entity.imageUrl,
                                targetUrl = entity.link,
                                artist = snippet // <--- AQUÍ PONEMOS EL RESUMEN
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
            val brandColor = if (clickedIconIndex != -1) menuItems[clickedIconIndex].brandColor else Color.Transparent
            // AJUSTE 3: Usamos "Entradas" para determinar si estamos en modo concepto/entradas
            val isConceptMode = (clickedItemName == "Entradas")

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
                        val itemSize = itemsToShow?.size ?: 0 // Obtener el tamaño de la lista
                        if (itemsToShow != null) {
                            // Usamos el 'clickedIconIndex' como una clave de nivel superior
                            // para forzar la recreación cada vez que se abre un nuevo Carousel.
                            key(safeInitialPage to itemSize) {
                                AlbumCarousel(
                                    items = itemsToShow,
                                    navController = navController,
                                    isConceptMode = isConceptMode,
                                    initialPage = safeInitialPage,
                                    onPageChanged = homeViewModel::setCurrentPage
                                )
                            }
                            // -------------------------------------------
                        } else if (appState is AppState.Loading) {
                            CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f), strokeWidth = 3.dp)
                        } else if (appState is AppState.Error) {
                            Text("Error al cargar datos.", color = Color.White, textAlign = TextAlign.Center, fontFamily = verdanaFontFamily)
                        }
                    }

                    if (clickedIconIndex != -1) {
                        Image(
                            painter = painterResource(id = menuItems[clickedIconIndex].iconResId),
                            contentDescription = null,
                            modifier = Modifier
                                .align(if (isPortrait) Alignment.TopCenter else Alignment.CenterStart)
                                .fillMaxWidth(0.20f)
                                .aspectRatio(1f)
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    val xOffset = -(placeable.width * 1.4f).roundToInt()
                                    val yOffset = -(placeable.height * 1.0f).roundToInt()
                                    layout(placeable.width, placeable.height) {
                                        if (isPortrait) placeable.placeRelative(0, yOffset)
                                        else placeable.placeRelative(xOffset, 0)
                                    }
                                }
                                .alpha(0.5f)
                        )
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