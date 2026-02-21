package mentat.music.com.mentapp.ui.screens.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mentat.music.com.mentapp.R
import mentat.music.com.mentapp.data.model.AppData
import mentat.music.com.mentapp.ui.composables.*
import mentat.music.com.mentapp.ui.rememberVibrator
import mentat.music.com.mentapp.ui.screens.home.composables.ContentLayer
import mentat.music.com.mentapp.ui.screens.home.viewmodel.AppState
import mentat.music.com.mentapp.ui.screens.home.viewmodel.HomeViewModel
import mentat.music.com.mentapp.utils.CarouselMapper
import mentat.music.com.mentapp.utils.MentatConstants
import mentat.music.com.mentapp.utils.NavigationUtils
import kotlin.system.exitProcess

@SuppressLint("LocalContextResourcesRead")
@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    // 1. ESTADO UNIFICADO (UI STATE)
    val uiState by homeViewModel.uiState.collectAsState()

    // Estados de datos
    val appState by homeViewModel.appState.collectAsState()
    val newsPosts by homeViewModel.newsPosts.collectAsState()

    // Animaciones locales
    val webMenuRotationAnim = remember { Animatable(0f) }
    val dialFlipX = remember { Animatable(1f, Float.VectorConverter) }
    val dialBlur = remember { Animatable(0f) }
    val rotationAngle = remember { Animatable(uiState.rotationAngle) }
    val dialScale = remember { Animatable(1.0f) }

    var isMainDial by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val vibrator = rememberVibrator()
    val context = LocalContext.current

    // Sincronización de animaciones con el ViewModel
    LaunchedEffect(uiState.webMenuRotationAngle) {
        webMenuRotationAnim.snapTo(uiState.webMenuRotationAngle)
    }

    // Efecto de rebote inicial
    val bounceSpec = spring<Float>(dampingRatio = 0.5f, stiffness = DialConstants.SPRING_STIFFNESS)
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
        animationSpec = tween(DialConstants.TRANSITION_DURATION),
        label = "dialSceneAlpha"
    )
    val arrowsAlpha by animateFloatAsState(
        targetValue = if (!uiState.isAnimatingOut) 0.4f else 0.0f,
        animationSpec = tween(300),
        label = "arrowsAlpha"
    )

    // Valores en Píxeles desde Constantes
    val radiusPx = with(LocalDensity.current) { DialConstants.ICON_PATH_RADIUS.toPx() }
    val thicknessPx = with(LocalDensity.current) { DialConstants.TOTAL_DONUT_THICKNESS.toPx() }

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

    // Función local para activar la expansión (necesita tocar frozenTime local)
    fun activateExpansion(index: Int) {
        scope.launch {
            if (index != -1) homeViewModel.setSelectedWebCategory(null)
            vibrator.vibrateClick()
            frozenTime = time // Capturamos el tiempo para congelar el shader
            homeViewModel.updateIsAnimatingOut(true)
            homeViewModel.updateClickedIconIndex(index)
            homeViewModel.updateIsExpansionFinished(true)
        }
    }

    // --- CONFIGURACIÓN DE MENÚS (Usando Utils y ViewModel) ---
    val itemsDial1 = remember {
        HomeMenuConfig.dial1Options.map { option ->
            DialItem(
                id = option.id,
                label = option.label,
                icon = option.iconRes ?: 0,
                color = option.color,
                onClick = {
                    when (option.id) {
                        "Bluesky" -> NavigationUtils.launchUrl(context, MentatConstants.URL_BLUESKY)
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
            val iconRes = if (option.id == "Subs") R.drawable.ic_menu_concept else (option.iconRes ?: 0)
            DialItem(
                id = option.id,
                label = option.label,
                icon = iconRes,
                color = option.color,
                onClick = {
                    when (option.id) {
                        "GUZZ" -> activateExpansion(0)
                        "DJSessions" -> NavigationUtils.launchUrl(context, MentatConstants.URL_DJ_SESSIONS)
                        "Subs" -> NavigationUtils.showComingSoon(context)
                        "Archive" -> NavigationUtils.launchUrl(context, MentatConstants.URL_BLOG_OLD)
                        "Contact" -> NavigationUtils.launchUrl(context, "mailto:info@mentat-music.com")
                        "Live" -> NavigationUtils.showComingSoon(context)
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

    // --- MANEJO DE BACK PRESS (Delegado al ViewModel) ---
    BackHandler(enabled = uiState.isAnimatingOut || uiState.isExpansionFinished) {
        scope.launch {
            // El ViewModel decide qué cerrar
            homeViewModel.handleBackPress()

            // Animaciones UI de retorno
            delay(200)
            dialScale.animateTo(1.0f, spring(0.35f, Spring.StiffnessLow))
            rotationAngle.snapTo(uiState.rotationAngle)
        }
    }
    BackHandler(enabled = uiState.isWebMenuOpen) { homeViewModel.setWebMenuOpen(false) }

    // --- UI LAYOUT ---
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Fondo Animado (Fondo limpio, sin nada de blur)
        BackgroundLayer(
            modifier = Modifier.fillMaxSize(),
            isFrozen = uiState.isAnimatingOut || uiState.isExpansionFinished,
            frozenTime = frozenTime,
            isBlueMode = !isMainDial
        )

        // 2. Contenido Principal
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 2.A: DIAL PRINCIPAL (Aquí va el Motion Blur Direccional)
            // 2.A: DIAL PRINCIPAL
            DialLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(dialSceneAlpha)
                    .graphicsLayer {
                    // Mantenemos tus escalas si las hay (en DialLayer no las tienes, en el Box sí)
                    // scaleX = dialScale.value * dialFlipX.value
                    // scaleY = dialScale.value

                    // Asignación limpia recomendada por el IDE
                    renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dialBlur.value > 0.5f) {
                        androidx.compose.ui.graphics.BlurEffect(
                            radiusX = dialBlur.value, // SIN multiplicar por density
                            radiusY = 1f,
                            edgeTreatment = androidx.compose.ui.graphics.TileMode.Clamp
                        )
                    } else {
                        null
                    }
                },
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
                radiusPx = radiusPx,
                thicknessPx = thicknessPx,
                onRotationComplete = { targetSnapAngle ->
                    homeViewModel.updateRotationAngle(targetSnapAngle)
                },
                scope = scope
            )

            // 2.B: DIMMER Y MINI DIAL
            val dimmerAlpha by animateFloatAsState(
                targetValue = if (uiState.isWebMenuOpen) 0.6f else 0f,
                label = "dimmerAlpha"
            )

            if (uiState.isWebMenuOpen || dimmerAlpha > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(DialConstants.COLOR_DIMMER.copy(alpha = dimmerAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { homeViewModel.setWebMenuOpen(false) }
                )
            }
            // 1. CAPA DE LOS DIALES (Giran y tienen Blur)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(dialScale.value)
                    .graphicsLayer {
                        // Este giro SOLO afecta a lo que hay dentro de esta Box (el menú web)
                        scaleX = dialFlipX.value
                    },
                contentAlignment = Alignment.Center
            ) {
                WebMenuLayer(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // El Motion Blur solo para el menú
                            renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dialBlur.value > 0.5f) {
                                androidx.compose.ui.graphics.BlurEffect(
                                    radiusX = dialBlur.value,
                                    radiusY = 1f,
                                    edgeTreatment = androidx.compose.ui.graphics.TileMode.Clamp
                                )
                            } else null
                        },
                    isVisible = uiState.isWebMenuOpen,
                    rotationAnim = webMenuRotationAnim,
                    items = itemsWebMenu,
                    onClose = { homeViewModel.setWebMenuOpen(false) },
                    onRotationChanged = { angle -> homeViewModel.updateWebMenuRotationAngle(angle) },
                    scope = scope,
                    onVibrate = { vibrator.vibrateClick() }
                )
            }


            // 2. CAPA DEL BOTÓN (Independiente, nítida y ahora REACTIVA)
            if (!uiState.isAnimatingOut && !uiState.isExpansionFinished) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Necesitamos capturar el estado de pulsación para el efecto visual
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    // Animación de escala para el "hundimiento" (95% del tamaño al pulsar)
                    val buttonPressScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "buttonPressScale"
                    )

                    SolarisPlayButton(
                        modifier = Modifier.graphicsLayer {
                            scaleX = buttonPressScale
                            scaleY = buttonPressScale
                        },
                        size = 80.dp,
                        onClick = {
                            scope.launch {
                                // --- PARTE 1: EL DIAL SE VA ---
                                launch { dialFlipX.animateTo(0.0f, tween(DialConstants.FLIP_DURATION)) }
                                launch { dialBlur.animateTo(60f, tween(DialConstants.FLIP_DURATION, easing = FastOutLinearInEasing)) }

                                delay(DialConstants.FLIP_DURATION.toLong())

                                // --- CAMBIO LÓGICO ---
                                isMainDial = !isMainDial
                                vibrator.vibrateClick()

                                // --- PARTE 2: EL NUEVO DIAL ATERRIZA ---
                                launch { dialFlipX.animateTo(1.0f, spring(DialConstants.SPRING_DAMPING)) }
                                launch { dialBlur.animateTo(0f, tween(DialConstants.FLIP_DURATION, easing = LinearOutSlowInEasing)) }

                                // Rebote de escala general de la escena
                                launch {
                                    dialScale.snapTo(1.15f)
                                    dialScale.animateTo(1.0f, bounceSpec)
                                }
                            }
                        }
                    )

                    // El área invisible de seguridad (también hereda la interacción si es necesario)
                    if (uiState.isWebMenuOpen) {
                        Box(Modifier.size(80.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}))
                    }
                }
            }


            // 2.C: CARRUSEL DE CONTENIDO (Delegado a ContentLayer)
            ContentLayer(
                modifier = Modifier.align(Alignment.Center),
                uiState = uiState,
                appState = appState,
                newsPosts = newsPosts,
                currentItems = currentItems,
                isPortrait = isPortrait,
                navController = navController,
                onPageChanged = { page -> homeViewModel.setCurrentPage(page) }
            )
        }

        // 3. CAPA HUD
        var isVibrationOn by remember { mutableStateOf(mentat.music.com.mentapp.data.UserPreferences.isVibrationEnabled(context)) }
        val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

        HudLayer(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f),
            isVibrationOn = isVibrationOn,
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

                // Comprobamos si hay algún menú abierto consultando el estado
                val isAnyMenuOpen = uiState.isExpansionFinished || uiState.clickedIconIndex != -1 || uiState.selectedWebCategory != null || uiState.isWebMenuOpen

                if (isAnyMenuOpen) {
                    // El ViewModel gestiona el cierre lógico
                    homeViewModel.handleBackPress()

                    // Animación visual de regreso (si no estamos en el menú web)
                    if (!uiState.isWebMenuOpen) {
                        scope.launch {
                            delay(200)
                            dialScale.animateTo(1.0f, spring(0.35f, Spring.StiffnessLow))
                            rotationAngle.snapTo(uiState.rotationAngle)
                        }
                    }
                } else {
                    // Salir de la app
                    backDispatcher?.onBackPressed()
                }
            }
        )
    }
}