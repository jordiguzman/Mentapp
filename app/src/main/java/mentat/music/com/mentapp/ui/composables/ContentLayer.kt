package mentat.music.com.mentapp.ui.screens.home.composables // O tu paquete ui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import mentat.music.com.mentapp.data.model.AppData
import mentat.music.com.mentapp.data.model.CarouselItem
import mentat.music.com.mentapp.ui.composables.AlbumCarouselBox
import mentat.music.com.mentapp.ui.composables.DialItem
import mentat.music.com.mentapp.ui.screens.home.DialConstants
import mentat.music.com.mentapp.ui.screens.home.viewmodel.AppState
import mentat.music.com.mentapp.ui.screens.home.viewmodel.HomeUiState
import mentat.music.com.mentapp.ui.theme.VerdanaFontFamily
import mentat.music.com.mentapp.utils.CarouselMapper

@Composable
fun ContentLayer(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    appState: AppState,
    newsPosts: List<CarouselItem>,
    currentItems: List<DialItem>,
    isPortrait: Boolean,
    navController: NavController,
    onPageChanged: (Int) -> Unit
) {
    // 1. Extracción de Datos Segura
    val appData: AppData? = remember(appState) { (appState as? AppState.Success)?.data }

    // 2. Determinar qué ID se ha pulsado (Web o Icono)
    val clickedItemId = uiState.selectedWebCategory ?:
    if (uiState.clickedIconIndex != -1 && uiState.clickedIconIndex < currentItems.size)
        currentItems[uiState.clickedIconIndex].id
    else null

    // 3. Mapeo de Datos (Usando el Mapper)
    val itemsToShow = remember(clickedItemId, appData, newsPosts) {
        CarouselMapper.mapToCarouselItems(
            itemId = clickedItemId,
            appData = appData,
            newsPosts = newsPosts
        )
    }

    // 4. Lógica de Animación de Entrada (Alpha)
    val carouselLayerTargetAlpha = when {
        uiState.isExpansionFinished && (appState is AppState.Loading || itemsToShow != null) -> 1f
        else -> 0f
    }
    val carouselLayerAnimatedAlpha by animateFloatAsState(
        targetValue = carouselLayerTargetAlpha,
        animationSpec = tween(DialConstants.TRANSITION_DURATION),
        label = "carouselLayerAlpha"
    )

    // 5. Lógica de Diseño (Colores y Tamaños)
    val brandColor = if (uiState.selectedWebCategory != null) {
        Color.Black
    } else {
        if (uiState.clickedIconIndex != -1 && uiState.clickedIconIndex < currentItems.size)
            currentItems[uiState.clickedIconIndex].color
        else Color.Transparent
    }

    val isConceptMode = (clickedItemId in listOf("Audio", "Divulgacion", "Blog"))

    // Modificadores de tamaño según orientación y tipo de contenido
    val carouselBoxModifier = if (isPortrait) {
        if (isConceptMode) Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f)
        else Modifier.fillMaxWidth(0.9f).aspectRatio(1f)
    } else {
        if (isConceptMode) Modifier.fillMaxHeight(0.9f).fillMaxWidth(0.7f)
        else Modifier.fillMaxHeight(0.9f).aspectRatio(1f)
    }

    // 6. RENDERIZADO
    if (uiState.isExpansionFinished) {
        Box(
            modifier = modifier
                .alpha(carouselLayerAnimatedAlpha)
                .then(carouselBoxModifier),
            contentAlignment = Alignment.Center
        ) {
            // Contenedor con borde y gradiente
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(brandColor.copy(0.6f), brandColor.copy(0.3f))
                        )
                    )
                    .border(
                        3.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(0.9f),
                                Color.Gray.copy(0.3f),
                                Color.White.copy(0.9f)
                            )
                        ),
                        RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // A. CONTENIDO CARGADO
                if (itemsToShow != null) {
                    val safeInitialPage = if (itemsToShow.isEmpty()) 0 else uiState.currentPage
                    // Usamos key para forzar recomposición si cambian los datos drásticamente
                    key(safeInitialPage to itemsToShow.size) {
                        AlbumCarouselBox(
                            modifier = Modifier,
                            items = itemsToShow,
                            navController = navController,
                            isConceptMode = isConceptMode,
                            initialPage = safeInitialPage,
                            onPageChanged = onPageChanged
                        )
                    }
                }
                // B. ESTADO DE CARGA
                else if (appState is AppState.Loading) {
                    CircularProgressIndicator(color = Color.White.copy(0.7f))
                }
                // C. ESTADO DE ERROR
                else if (appState is AppState.Error) {
                    Text(
                        text = "Error cargar datos.",
                        color = Color.White,
                        fontFamily = VerdanaFontFamily
                    )
                }
            }
        }
    }
}