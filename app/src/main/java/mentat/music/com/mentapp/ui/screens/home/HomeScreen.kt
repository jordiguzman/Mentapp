package mentat.music.com.mentapp.ui.screens.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
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
import mentat.music.com.mentapp.ui.composables.AlbumCarouselBox
import mentat.music.com.mentapp.ui.composables.AttractorBackground
import mentat.music.com.mentapp.ui.composables.CircularDialLayout
import mentat.music.com.mentapp.ui.composables.DialItem
import mentat.music.com.mentapp.ui.composables.HudLayer
import mentat.music.com.mentapp.ui.composables.MiniCircularDialLayout
import mentat.music.com.mentapp.ui.composables.SolarisPlayButton
import mentat.music.com.mentapp.ui.composables.TRANSITION_DURATION
import mentat.music.com.mentapp.ui.composables.VideoBackground
import mentat.music.com.mentapp.ui.rememberVibrator
import mentat.music.com.mentapp.ui.screens.home.viewmodel.AppState
import mentat.music.com.mentapp.ui.screens.home.viewmodel.HomeViewModel
import mentat.music.com.mentapp.ui.theme.VerdanaFontFamily
import mentat.music.com.mentapp.utils.CarouselMapper
import mentat.music.com.mentapp.utils.MentatConstants
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.system.exitProcess

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

    val isWebMenuOpen by homeViewModel.isWebMenuOpen.collectAsState()
    val webMenuRotationAngle by homeViewModel.webMenuRotationAngle.collectAsState()
    val webMenuRotationAnim = remember { Animatable(0f) }

    val dialFlipX = remember { Animatable(1f, Float.VectorConverter) }
    val dialBlur = remember { Animatable(0f) }

    var isMainDial by remember { mutableStateOf(true) }
    val selectedWebCategory by homeViewModel.selectedWebCategory.collectAsState()

    val currentPage by homeViewModel.currentPage
    val rotationAngle = remember { Animatable(savedRotationAngle) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val dialScale = remember { Animatable(1.0f) }
    val vibrator = rememberVibrator()

    val angleStep = (2 * Math.PI.toFloat() / 6)
    val targetAngleRad = (Math.PI.toFloat() / 2.0f)

    LaunchedEffect(webMenuRotationAngle) {
        webMenuRotationAnim.snapTo(webMenuRotationAngle)
    }

    val bounceSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 150f)
    val bounceStartScale = 1.1f
    LaunchedEffect(Unit) {
        scope.launch {
            delay(100)
            dialScale.snapTo(bounceStartScale)
            dialScale.animateTo(targetValue = 1.0f, animationSpec = bounceSpec)
        }
    }

    val currentLanguage by homeViewModel.currentLanguage.collectAsState()

    val view = LocalView.current
    val window = (view.context as Activity).window
    LaunchedEffect(key1 = window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

    val dialSceneAlpha by animateFloatAsState(
        targetValue = if (isExpansionFinished) 0f else 1f,
        animationSpec = tween(TRANSITION_DURATION),
        label = "dialSceneAlpha"
    )
    val arrowsAlpha by animateFloatAsState(
        targetValue = if (!isAnimatingOut) 0.4f else 0.0f,
        animationSpec = tween(300),
        label = "arrowsAlpha"
    )

    val iconPathRadius = 140.dp
    val donutPadding = 8.dp
    val donutThickness = 76.dp + (donutPadding * 2)
    val donutRadius = iconPathRadius
    val radiusPx = with(LocalDensity.current) { donutRadius.toPx() }
    val thicknessPx = with(LocalDensity.current) { donutThickness.toPx() }
    val arrowsYOffset = iconPathRadius

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

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val iconSize = 46.dp
    val edgePadding = 24.dp
    val dynamicRadius = (screenWidth / 2) - (iconSize / 2) - edgePadding

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

    val itemsDial1 = remember {
        listOf(
            DialItem("Bluesky", "Bluesky", R.drawable.ic_menu_social, Color(0xFF0085FF)) { launchUrl(MentatConstants.URL_BLUESKY) },
            DialItem("YouTube", "YouTube", R.drawable.ic_menu_youtube, Color(0xFFFF0000)) { activateExpansion(1) },
            DialItem("Spotify", "Spotify", R.drawable.ic_menu_streams, Color(0xFF1DB954)) { activateExpansion(2) },
            DialItem("Bandcamp", "Bandcamp", R.drawable.ic_menu_bandcamp, Color(0xFF629AA9)) { activateExpansion(3) },
            DialItem("SoundCloud", "SoundCloud", R.drawable.ic_menu_soundcloud, Color(0xFFFF5500)) { activateExpansion(4) },
            DialItem("Web", "Mundo Web", R.drawable.ic_logo_mentat, Color(0xFF893471)) { scope.launch { homeViewModel.setWebMenuOpen(true); vibrator.vibrateClick() } }
        )
    }

    val itemsDial2 = remember {
        listOf(
            DialItem("GUZZ", "GUZZ", R.drawable.ic_menu_guzz, Color.White) { activateExpansion(0) },
            DialItem("DJSessions", "DJ Sessions", R.drawable.ic_sessions, Color(0xFF000000)) { launchUrl(MentatConstants.URL_DJ_SESSIONS) },
            DialItem("Subs", "Suscriptores", Icons.Default.Lock, Color(0xFF000000)) { showComingSoon() },
            DialItem("Archive", "Archivo", R.drawable.ic_menu_concept, Color(0xFF8A2BE2)) { launchUrl(MentatConstants.URL_BLOG_OLD) },
            DialItem("Contact", "Contacto", R.drawable.ic_mail, Color(0xFF000000)) { launchUrl("mailto:info@mentat-music.com") },
            DialItem("Live", "Directo", R.drawable.ic_live_music, Color(0xFF000000)) { showComingSoon() }
        )
    }

    val itemsWebMenu = remember {
        listOf(
            DialItem("Audio", "Tutoriales", R.drawable.ic_audio, Color(0xFF000000)) { homeViewModel.filterByCategory("Audio"); homeViewModel.setSelectedWebCategory("Audio"); homeViewModel.setWebMenuOpen(false); activateExpansion(-1) },
            DialItem("Divulgacion", "Ciencia", R.drawable.ic_divulgacion, Color(0xFF000000)) { homeViewModel.filterByCategory("Divulgacion"); homeViewModel.setSelectedWebCategory("Divulgacion"); homeViewModel.setWebMenuOpen(false); activateExpansion(-1) },
            DialItem("Blog", "Blog", R.drawable.ic_blog, Color(0xFF000000)) { homeViewModel.filterByCategory("Blog"); homeViewModel.setSelectedWebCategory("Blog"); homeViewModel.setWebMenuOpen(false); activateExpansion(-1) }
        )
    }

    val currentItems = if (isMainDial) itemsDial1 else itemsDial2

    BackHandler(enabled = isAnimatingOut || isExpansionFinished) {
        scope.launch {
            val wasWebMode = selectedWebCategory != null
            homeViewModel.updateIsExpansionFinished(false)
            homeViewModel.updateIsAnimatingOut(false)
            homeViewModel.setSelectedWebCategory(null)
            if (wasWebMode) launch { delay(300); homeViewModel.setWebMenuOpen(true) }
            delay(200)
            dialScale.animateTo(1.0f, spring(0.35f, Spring.StiffnessLow))
            homeViewModel.updateClickedIconIndex(-1)
            rotationAngle.snapTo(homeViewModel.rotationAngle.value)
        }
    }
    BackHandler(enabled = isWebMenuOpen) { homeViewModel.setWebMenuOpen(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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

            // 2.A: DIAL QUE SE DESVANECE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(dialSceneAlpha)
                    .pointerInput(clickedIconIndex, isAnimatingOut, isExpansionFinished, isWebMenuOpen) {
                        if (isAnimatingOut || isExpansionFinished || isWebMenuOpen) return@pointerInput
                        var centerX = 0f; var centerY = 0f
                        detectDragGestures(
                            onDragStart = { centerX = size.width / 2f; centerY = size.height / 2f; scope.launch { rotationAngle.stop() } },
                            onDrag = { change, _ -> change.consume(); val startAngle = atan2(change.previousPosition.y - centerY, change.previousPosition.x - centerX); val endAngle = atan2(change.position.y - centerY, change.position.x - centerX); scope.launch { rotationAngle.snapTo(rotationAngle.value + (endAngle - startAngle)) } },
                            onDragEnd = { val currentOffset = rotationAngle.value - targetAngleRad; val nearestIconIndex = -(currentOffset / angleStep).roundToInt(); val targetSnapAngle = targetAngleRad - (angleStep * nearestIconIndex); scope.launch { rotationAngle.animateTo(targetSnapAngle, spring(0.7f, 100f)); homeViewModel.updateRotationAngle(targetSnapAngle) } }
                        )
                    }
            ) {
                Text(text = stringResource(R.string.dial_title), color = Color.White.copy(0.5f), fontSize = 22.sp, fontFamily = VerdanaFontFamily, modifier = Modifier.align(if (isPortrait) Alignment.TopCenter else Alignment.TopStart).padding(32.dp))

                Box(
                    modifier = Modifier.fillMaxSize()
                        .scale(dialScale.value)
                        .blur(if (dialBlur.value > 0f) dialBlur.value.dp else 0.dp)
                        .graphicsLayer { scaleX = dialScale.value * dialFlipX.value; scaleY = dialScale.value },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val brush = Brush.sweepGradient(listOf(Color.White.copy(0.95f), Color.White.copy(0.4f), Color.Gray.copy(0.6f), Color.White.copy(0.4f), Color.White.copy(0.95f)), center = center)
                        drawCircle(brush, radiusPx, style = Stroke(thicknessPx))
                        drawCircle(Color.White.copy(0.8f), radiusPx - (thicknessPx / 2), style = Stroke(1.5.dp.toPx()))
                        drawCircle(Color.White.copy(0.5f), radiusPx + (thicknessPx / 2), style = Stroke(2.dp.toPx()))
                    }
                    Row(Modifier.align(Alignment.Center).offset(y = arrowsYOffset).alpha(arrowsAlpha)) {
                        Image(painterResource(R.drawable.outline_line_start_arrow_notch_24), null, colorFilter = ColorFilter.tint(Color.Black), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(80.dp))
                        Image(painterResource(R.drawable.outline_line_end_arrow_notch_24), null, colorFilter = ColorFilter.tint(Color.Black), modifier = Modifier.size(24.dp))
                    }

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
            }

            // 2.B: DIMMER Y MINI DIAL
            val dimmerAlpha by animateFloatAsState(
                targetValue = if (isWebMenuOpen) 0.6f else 0f,
                label = "dimmerAlpha"
            )
            if (isWebMenuOpen || dimmerAlpha > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(dimmerAlpha)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { homeViewModel.setWebMenuOpen(false) })
            }

            Box(
                modifier = Modifier.fillMaxSize()
                    .scale(dialScale.value)
                    .blur(if (dialBlur.value > 0f) dialBlur.value.dp else 0.dp)
                    .graphicsLayer { scaleX = dialScale.value * dialFlipX.value; scaleY = dialScale.value },
                contentAlignment = Alignment.Center
            ) {
                val miniDialScale by animateFloatAsState(
                    targetValue = if (isWebMenuOpen) 1f else 0f,
                    animationSpec = spring(0.6f, 200f),
                    label = "miniDialScale"
                )

                if (isWebMenuOpen || miniDialScale > 0.1f) {
                    Box(Modifier.fillMaxSize().offset(y = 140.dp).scale(miniDialScale), Alignment.Center) {
                        Canvas(Modifier.size(245.dp)) {
                            val brush = Brush.sweepGradient(listOf(Color.White.copy(0.95f), Color.White.copy(0.2f), Color.Gray.copy(0.5f), Color.White.copy(0.2f), Color.White.copy(0.95f)), center = center)
                            drawCircle(brush, with(drawContext.density) { 95.dp.toPx() }, style = Stroke(with(drawContext.density) { 55.dp.toPx() }))
                        }
                        CompositionLocalProvider(LocalContentColor provides Color(0xFF893471)) {
                            Box(Modifier.fillMaxSize().pointerInput(Unit) {
                                val stepRad = (2 * Math.PI / 3).toFloat()
                                detectDragGestures(
                                    onDragStart = { scope.launch { webMenuRotationAnim.stop() } },
                                    onDrag = { change, dragAmount -> change.consume(); scope.launch { webMenuRotationAnim.snapTo(webMenuRotationAnim.value + ((dragAmount.x / 350) * -1f)) } },
                                    onDragEnd = { val steps = (webMenuRotationAnim.value / stepRad).roundToInt(); scope.launch { webMenuRotationAnim.animateTo(steps * stepRad, spring(0.6f, 300f)); homeViewModel.updateWebMenuRotationAngle(steps * stepRad) } }
                                )
                            }, Alignment.Center) {
                                MiniCircularDialLayout(
                                    modifier = Modifier.fillMaxSize(),
                                    items = itemsWebMenu,
                                    currentRotation = webMenuRotationAnim.value,
                                    radius = 95.dp
                                )
                            }
                        }
                        Box(Modifier.size(60.dp).clip(RoundedCornerShape(50)).clickable { homeViewModel.setWebMenuOpen(false); vibrator.vibrateClick() }, Alignment.Center) {
                            Image(painterResource(R.drawable.ic_web_foreground), "Cerrar", colorFilter = ColorFilter.tint(Color.Black), modifier = Modifier.size(32.dp))
                        }
                    }
                }

                if (!isAnimatingOut && !isExpansionFinished) {
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

                    if (isWebMenuOpen) Box(Modifier.size(80.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}))
                }
            }

            // 2.C: CARRUSEL
            val appData: AppData? = remember(appState) { (appState as? AppState.Success)?.data }
            val clickedItemId = selectedWebCategory ?: if (clickedIconIndex != -1 && clickedIconIndex < currentItems.size) currentItems[clickedIconIndex].id else null

            // ✅ CAMBIO: Usar el CarouselMapper
            val itemsToShow = CarouselMapper.mapToCarouselItems(
                itemId = clickedItemId,
                appData = appData,
                newsPosts = newsPosts
            )

            val carouselLayerTargetAlpha = when {
                isExpansionFinished && (appState is AppState.Loading || itemsToShow != null) -> 1f
                else -> 0f
            }
            val carouselLayerAnimatedAlpha by animateFloatAsState(
                targetValue = carouselLayerTargetAlpha,
                animationSpec = tween(TRANSITION_DURATION),
                label = "carouselLayerAlpha"
            )

            val brandColor = if (selectedWebCategory != null) Color.Black else (if (clickedIconIndex != -1 && clickedIconIndex < currentItems.size) currentItems[clickedIconIndex].color else Color.Transparent)
            val isConceptMode = (clickedItemId in listOf("Audio", "Divulgacion", "Blog"))
            val carouselBoxModifier = if (isPortrait) {
                if (isConceptMode) Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f)
                else Modifier.fillMaxWidth(0.9f).aspectRatio(1f)
            } else {
                if (isConceptMode) Modifier.fillMaxHeight(0.9f).fillMaxWidth(0.7f)
                else Modifier.fillMaxHeight(0.9f).aspectRatio(1f)
            }

            if (isExpansionFinished) {
                Box(Modifier.alpha(carouselLayerAnimatedAlpha).then(carouselBoxModifier), Alignment.Center) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(Brush.linearGradient(listOf(brandColor.copy(0.6f), brandColor.copy(0.3f)))).border(3.dp, Brush.linearGradient(listOf(Color.White.copy(0.9f), Color.Gray.copy(0.3f), Color.White.copy(0.9f))), RoundedCornerShape(32.dp)), Alignment.Center) {
                        val safeInitialPage = if (itemsToShow.isNullOrEmpty()) 0 else currentPage
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
        } // FIN BOX LOGICO

        // =================================================================
        // CAPA 3: HUD (BOTONES ENCIMA DE TODO - zIndex: 100f)
        // =================================================================
        var isVibrationOn by remember { mutableStateOf(mentat.music.com.mentapp.data.UserPreferences.isVibrationEnabled(context)) }
        val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

        HudLayer(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f),
            isVibrationOn = isVibrationOn,
            currentLanguage = currentLanguage,
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

                if (isExpansionFinished || clickedIconIndex != -1 || selectedWebCategory != null) {
                    val wasWebMode = selectedWebCategory != null
                    homeViewModel.updateIsExpansionFinished(false)
                    homeViewModel.updateIsAnimatingOut(false)
                    homeViewModel.setSelectedWebCategory(null)
                    homeViewModel.onIconClicked(-1)
                    if (wasWebMode) {
                        scope.launch { delay(300); homeViewModel.setWebMenuOpen(true) }
                    }
                } else if (isWebMenuOpen) {
                    homeViewModel.setWebMenuOpen(false)
                } else {
                    backDispatcher?.onBackPressed()
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}