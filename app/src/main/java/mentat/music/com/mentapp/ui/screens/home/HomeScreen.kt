package mentat.music.com.mentapp.ui.screens.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import mentat.music.com.mentapp.ui.composables.*
import mentat.music.com.mentapp.ui.rememberVibrator
import mentat.music.com.mentapp.ui.screens.home.viewmodel.AppState
import mentat.music.com.mentapp.ui.screens.home.viewmodel.HomeViewModel
import mentat.music.com.mentapp.ui.theme.VerdanaFontFamily
import mentat.music.com.mentapp.utils.CarouselMapper
import mentat.music.com.mentapp.utils.MentatConstants
import kotlin.math.roundToInt
import kotlin.system.exitProcess

@SuppressLint("LocalContextResourcesRead")
@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    // 1. ESTADO UNIFICADO (UI STATE)
    // En lugar de 20 variables, observamos un solo objeto de estado
    val uiState by homeViewModel.uiState.collectAsState()

    // Estados de datos (se mantienen separados por asincronía)
    val appState by homeViewModel.appState.collectAsState()
    val newsPosts by homeViewModel.newsPosts.collectAsState()

    // Animaciones locales (UI efímera)
    val webMenuRotationAnim = remember { Animatable(0f) }
    val dialFlipX = remember { Animatable(1f, Float.VectorConverter) }
    val dialBlur = remember { Animatable(0f) }
    val rotationAngle = remember { Animatable(uiState.rotationAngle) }
    val dialScale = remember { Animatable(1.0f) }

    var isMainDial by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val vibrator = rememberVibrator()
    val context = LocalContext.current

    // Constantes de UI
    val angleStep = (2 * Math.PI.toFloat() / 6)
    val targetAngleRad = (Math.PI.toFloat() / 2.0f)

    // Sincronización de animaciones con el ViewModel
    LaunchedEffect(uiState.webMenuRotationAngle) {
        webMenuRotationAnim.snapTo(uiState.webMenuRotationAngle)
    }

    // Efecto de rebote inicial
    val bounceSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 150f)
    LaunchedEffect(Unit) {
        scope.launch {
            delay(100)
            dialScale.snapTo(1.1f)
            dialScale.animateTo(targetValue = 1.0f, animationSpec = bounceSpec)
        }
    }

    // Configuración de Ventana (Inmersiva)
    val view = LocalView.current
    val window = (view.context as Activity).window
    LaunchedEffect(key1 = window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    // Animaciones de Transición
    val dialSceneAlpha by animateFloatAsState(
        targetValue = if (uiState.isExpansionFinished) 0f else 1f,
        animationSpec = tween(TRANSITION_DURATION),
        label = "dialSceneAlpha"
    )
    val arrowsAlpha by animateFloatAsState(
        targetValue = if (!uiState.isAnimatingOut) 0.4f else 0.0f,
        animationSpec = tween(300),
        label = "arrowsAlpha"
    )

    // Dimensiones
    val iconPathRadius = 140.dp
    val donutPadding = 8.dp
    val donutThickness = 76.dp + (donutPadding * 2)
    val donutRadius = iconPathRadius
    val radiusPx = with(LocalDensity.current) { donutRadius.toPx() }
    val thicknessPx = with(LocalDensity.current) { donutThickness.toPx() }
    val arrowsYOffset = iconPathRadius

    // Shader de tiempo (Fondo)
    val infiniteTransition = rememberInfiniteTransition(label = "shader time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 600000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "time"
    )
    var frozenTime by remember { mutableStateOf(0f) }

    // Helpers de Navegación y Acciones
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

    // --- CONFIGURACIÓN DE MENÚS (Usando HomeMenuConfig) ---
    val itemsDial1 = remember {
        HomeMenuConfig.dial1Options.map { option ->
            DialItem(
                id = option.id,
                label = option.label,
                icon = option.iconRes ?: 0,
                color = option.color,
                onClick = {
                    when (option.id) {
                        "Bluesky" -> launchUrl(MentatConstants.URL_BLUESKY)
                        "YouTube" -> activateExpansion(1)
                        "Spotify" -> activateExpansion(2)
                        "Bandcamp" -> activateExpansion(3)
                        "SoundCloud" -> activateExpansion(4)
                        "Web" -> scope.launch { homeViewModel.setWebMenuOpen(true); vibrator.vibrateClick() }
                    }
                }
            )
        }
    }

    val itemsDial2 = remember {
        HomeMenuConfig.dial2Options.map { option ->
            // Fallback para icono de candado si no hay recurso
            val iconRes = if (option.id == "Subs") R.drawable.ic_menu_concept else (option.iconRes ?: 0)
            DialItem(
                id = option.id,
                label = option.label,
                icon = iconRes,
                color = option.color,
                onClick = {
                    when (option.id) {
                        "GUZZ" -> activateExpansion(0)
                        "DJSessions" -> launchUrl(MentatConstants.URL_DJ_SESSIONS)
                        "Subs" -> showComingSoon()
                        "Archive" -> launchUrl(MentatConstants.URL_BLOG_OLD)
                        "Contact" -> launchUrl("mailto:info@mentat-music.com")
                        "Live" -> showComingSoon()
                    }
                }
            )
        }
    }

    val itemsWebMenu = remember {
        HomeMenuConfig.webMenuOptions.map { option ->
            DialItem(
                id = option.id,
                label = option.label,
                icon = option.iconRes ?: 0,
                color = option.color,
                onClick = {
                    homeViewModel.filterByCategory(option.id)
                    homeViewModel.setSelectedWebCategory(option.id)
                    homeViewModel.setWebMenuOpen(false)
                    activateExpansion(-1)
                }
            )
        }
    }

    val currentItems = if (isMainDial) itemsDial1 else itemsDial2

    // --- MANEJO DE BACK PRESS ---
    BackHandler(enabled = uiState.isAnimatingOut || uiState.isExpansionFinished) {
        scope.launch {
            val wasWebMode = uiState.selectedWebCategory != null
            homeViewModel.updateIsExpansionFinished(false)
            homeViewModel.updateIsAnimatingOut(false)
            homeViewModel.setSelectedWebCategory(null)
            if (wasWebMode) launch { delay(300); homeViewModel.setWebMenuOpen(true) }
            delay(200)
            dialScale.animateTo(1.0f, spring(0.35f, Spring.StiffnessLow))
            homeViewModel.updateClickedIconIndex(-1)
            rotationAngle.snapTo(uiState.rotationAngle)
        }
    }
    BackHandler(enabled = uiState.isWebMenuOpen) { homeViewModel.setWebMenuOpen(false) }

    // --- UI LAYOUT PRINCIPAL ---
    // Optimización: Usamos Configuration en lugar de BoxWithConstraints para evitar warnings
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Fondo Animado
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AttractorBackground(
                modifier = Modifier.fillMaxSize(),
                isFrozen = uiState.isAnimatingOut || uiState.isExpansionFinished,
                frozenTime = frozenTime,
                isBlueMode = !isMainDial
            )
        } else {
            VideoBackground(modifier = Modifier.fillMaxSize())
        }

        // 2. Contenido Principal
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            // 2.A: DIAL PRINCIPAL (Refactorizado a DialLayer)
            DialLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(dialSceneAlpha),
                currentItems = currentItems,
                isPortrait = isPortrait,
                dialTitle = stringResource(R.string.dial_title),
                rotationAngle = rotationAngle,
                dialScale = dialScale,
                dialFlipX = dialFlipX,
                dialBlur = dialBlur,
                isAnimatingOut = uiState.isAnimatingOut,
                clickedIconIndex = uiState.clickedIconIndex,
                isExpansionFinished = uiState.isExpansionFinished,
                isWebMenuOpen = uiState.isWebMenuOpen,
                arrowsAlpha = arrowsAlpha,
                iconPathRadius = iconPathRadius,
                radiusPx = radiusPx,
                thicknessPx = thicknessPx,
                arrowsYOffset = arrowsYOffset,
                angleStep = angleStep,
                targetAngleRad = targetAngleRad,
                onRotationComplete = { targetSnapAngle ->
                    homeViewModel.updateRotationAngle(targetSnapAngle)
                },
                scope = scope
            )

            // 2.B: DIMMER Y MINI DIAL (Refactorizado a WebMenuLayer)
            val dimmerAlpha by animateFloatAsState(
                targetValue = if (uiState.isWebMenuOpen) 0.6f else 0f,
                label = "dimmerAlpha"
            )

            if (uiState.isWebMenuOpen || dimmerAlpha > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(dimmerAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { homeViewModel.setWebMenuOpen(false) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(dialScale.value)
                    .blur(if (dialBlur.value > 0f) dialBlur.value.dp else 0.dp)
                    .graphicsLayer {
                        scaleX = dialScale.value * dialFlipX.value
                        scaleY = dialScale.value
                    },
                contentAlignment = Alignment.Center
            ) {
                // Nuevo Componente: WebMenuLayer
                WebMenuLayer(
                    modifier = Modifier.fillMaxSize(),
                    isVisible = uiState.isWebMenuOpen,
                    rotationAnim = webMenuRotationAnim,
                    items = itemsWebMenu,
                    onClose = { homeViewModel.setWebMenuOpen(false) },
                    onRotationChanged = { angle -> homeViewModel.updateWebMenuRotationAngle(angle) },
                    scope = scope,
                    onVibrate = { vibrator.vibrateClick() }
                )

                // Botón Central (SolarisPlayButton)
                if (!uiState.isAnimatingOut && !uiState.isExpansionFinished) {
                    SolarisPlayButton(
                        size = 80.dp,
                        onClick = {
                            scope.launch {
                                launch { dialFlipX.animateTo(0.0f, tween(250)); dialBlur.animateTo(10f, tween(250)) }
                                delay(250)
                                isMainDial = !isMainDial
                                vibrator.vibrateClick()
                                launch { dialFlipX.animateTo(1.0f, spring(0.7f)); dialBlur.animateTo(0f, tween(300)) }
                                launch { dialScale.snapTo(1.15f); dialScale.animateTo(1.0f, bounceSpec) }
                            }
                        }
                    )

                    if (uiState.isWebMenuOpen) {
                        Box(Modifier.size(80.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}))
                    }
                }
            }

            // 2.C: CARRUSEL DE CONTENIDO
            val appData: AppData? = remember(appState) { (appState as? AppState.Success)?.data }

            // Lógica de selección de item para mostrar
            val clickedItemId = uiState.selectedWebCategory ?:
            if (uiState.clickedIconIndex != -1 && uiState.clickedIconIndex < currentItems.size)
                currentItems[uiState.clickedIconIndex].id
            else null

            // Mapper con el nuevo estado
            val itemsToShow = CarouselMapper.mapToCarouselItems(
                itemId = clickedItemId,
                appData = appData,
                newsPosts = newsPosts
            )

            val carouselLayerTargetAlpha = when {
                uiState.isExpansionFinished && (appState is AppState.Loading || itemsToShow != null) -> 1f
                else -> 0f
            }
            val carouselLayerAnimatedAlpha by animateFloatAsState(
                targetValue = carouselLayerTargetAlpha,
                animationSpec = tween(TRANSITION_DURATION),
                label = "carouselLayerAlpha"
            )

            val brandColor = if (uiState.selectedWebCategory != null) Color.Black else (if (uiState.clickedIconIndex != -1 && uiState.clickedIconIndex < currentItems.size) currentItems[uiState.clickedIconIndex].color else Color.Transparent)
            val isConceptMode = (clickedItemId in listOf("Audio", "Divulgacion", "Blog"))

            val carouselBoxModifier = if (isPortrait) {
                if (isConceptMode) Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f)
                else Modifier.fillMaxWidth(0.9f).aspectRatio(1f)
            } else {
                if (isConceptMode) Modifier.fillMaxHeight(0.9f).fillMaxWidth(0.7f)
                else Modifier.fillMaxHeight(0.9f).aspectRatio(1f)
            }

            if (uiState.isExpansionFinished) {
                Box(Modifier.alpha(carouselLayerAnimatedAlpha).then(carouselBoxModifier), Alignment.Center) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(Brush.linearGradient(listOf(brandColor.copy(0.6f), brandColor.copy(0.3f)))).border(3.dp, Brush.linearGradient(listOf(Color.White.copy(0.9f), Color.Gray.copy(0.3f), Color.White.copy(0.9f))), RoundedCornerShape(32.dp)), Alignment.Center) {

                        val safeInitialPage = if (itemsToShow.isNullOrEmpty()) 0 else uiState.currentPage

                        if (itemsToShow != null) {
                            key(safeInitialPage to itemsToShow.size) {
                                AlbumCarouselBox(
                                    modifier = Modifier,
                                    items = itemsToShow,
                                    navController = navController,
                                    isConceptMode = isConceptMode,
                                    initialPage = safeInitialPage,
                                    onPageChanged = homeViewModel::setCurrentPage
                                )
                            }
                        } else if (appState is AppState.Loading) {
                            CircularProgressIndicator(color = Color.White.copy(0.7f))
                        } else if (appState is AppState.Error) {
                            Text("Error cargar datos.", color = Color.White, fontFamily = VerdanaFontFamily)
                        }
                    }
                }
            }
        }

        // 3. CAPA HUD (Superior)
        var isVibrationOn by remember { mutableStateOf(mentat.music.com.mentapp.data.UserPreferences.isVibrationEnabled(context)) }
        val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

        HudLayer(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f),
            isVibrationOn = isVibrationOn,
            // Convertimos el Enum a String para el HUD
            currentLanguage = uiState.currentLanguage,
            onVibrationToggle = {
                val newState = !isVibrationOn
                isVibrationOn = newState
                mentat.music.com.mentapp.data.UserPreferences.setVibrationEnabled(context, newState)
                if (newState) vibrator.vibrateClick()
            },
            onExitClick = {
                vibrator.vibrateClick()
                val activity = context as? Activity
                activity?.finishAndRemoveTask()
                exitProcess(0)
            },
            onLanguageClick = {
                homeViewModel.toggleLanguage()
                vibrator.vibrateClick()
            },
            onBackClick = {
                vibrator.vibrateClick()

                if (uiState.isExpansionFinished || uiState.clickedIconIndex != -1 || uiState.selectedWebCategory != null) {
                    val wasWebMode = uiState.selectedWebCategory != null
                    homeViewModel.updateIsExpansionFinished(false)
                    homeViewModel.updateIsAnimatingOut(false)
                    homeViewModel.setSelectedWebCategory(null)
                    homeViewModel.onIconClicked(-1)
                    if (wasWebMode) {
                        scope.launch { delay(300); homeViewModel.setWebMenuOpen(true) }
                    }
                } else if (uiState.isWebMenuOpen) {
                    homeViewModel.setWebMenuOpen(false)
                } else {
                    backDispatcher?.onBackPressed()
                }
            }
        )
    }
}