package mentat.music.com.mentapp.ui.composables

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.request.RequestOptions
import mentat.music.com.mentapp.R
import mentat.music.com.mentapp.data.model.CarouselItem
import mentat.music.com.mentapp.ui.navigation.AppScreens
import mentat.music.com.mentapp.ui.rememberVibrator
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


// --- FUENTES ---
private val verdanaFontFamily = FontFamily(
    Font(R.font.verdana_regular, FontWeight.Normal),
    Font(R.font.verdana_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.verdana_bold, FontWeight.Bold),
    Font(R.font.verdana_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumCarouselBox(
    modifier: Modifier = Modifier,
    items: List<CarouselItem>,
    navController: NavController,
    isConceptMode: Boolean,
    initialPage: Int,
    onPageChanged: (Int) -> Unit
) {
    val sidePadding = 32.dp

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { items.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { pageIndex ->
            onPageChanged(pageIndex)
        }
    }

    LaunchedEffect(initialPage) {
        if (pagerState.currentPage != initialPage) {
            pagerState.scrollToPage(initialPage)
        }
    }

    // --- ARQUITECTURA DE CAPAS (LAYERED ARCHITECTURE) ---
    // Usamos un Box raíz que ocupa todo el espacio.
    // Los hijos se superponen según su alineación, sin empujarse.
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center // Por defecto, todo al centro
    ) {

        // CAPA 1: EL CARRUSEL (FONDO)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center) // Centrado absoluto
                // IMPORTANTE: Margen de seguridad inferior.
                // Esto impide que el contenido de la carta baje tanto que toque los puntos.
                .padding(bottom = 50.dp),
            contentPadding = PaddingValues(horizontal = sidePadding),
            pageSpacing = 16.dp,
            verticalAlignment = Alignment.CenterVertically
        ) { pageIndex ->
            val item = items[pageIndex]
            val esBlog = !item.content.isNullOrBlank()

            if (esBlog || isConceptMode) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    // Y aquí usamos el BIAS (Horizontal, Vertical)
                    // 0f = Centrado horizontalmente
                    // -0.7f = Tirando hacia arriba (Top es -1.0).
                    // Juega con este número: -0.5, -0.7, -0.9...
                    contentAlignment = BiasAlignment(0f, -0.7f)
                ) {
                    BlogCardBox(item = item, navController = navController)
                }
            } else {
                MusicCardBox(item = item, navController = navController)
            }
        }

        // CAPA 2: EL INDICADOR (FRENTE)
        // Está "clavado" abajo. Es imposible que desaparezca o se mueva.
        HorizontalPagerIndicatorBox(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.BottomCenter) // Anclaje al suelo
                .padding(bottom = 24.dp) // Separación del borde de la pantalla
        )
    }
}

/**
 * MUSIC CARD
 * - Diseño cuadrado y centrado.
 */
/**
 * MUSIC CARD (Versión Box - Imagen Reducida)
 * - Hemos aumentado el padding lateral de 8dp a 48dp.
 * - Resultado: La imagen se hace más pequeña y deja sitio al texto.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MusicCardBox(
    item: CarouselItem,
    navController: NavController
) {
    val (context, uriHandler, vibrator) = getHelpersBox()
    val textShadow = getTextShadowBox()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .clickable(enabled = item.targetUrl != null) {
                handleClickBox(item, context, uriHandler, vibrator, navController)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. IMAGEN CUADRADA (REDUCIDA)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // --- CAMBIO AQUÍ ---
                    // Antes era 8.dp. Ahora ponemos 48.dp (o más si quieres).
                    // Al estrecharla, baja su altura y deja sitio al autor.
                    .padding(horizontal = 48.dp)
                    .aspectRatio(1f) // Sigue siendo cuadrada
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl != null) {
                    GlideImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        requestBuilderTransform = { it.apply(RequestOptions().override(800).skipMemoryCache(true)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. TEXTOS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.title?.uppercase() ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontFamily = verdanaFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Ahora este texto debería tener espacio de sobra para respirar
                Text(
                    text = item.artist ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow),
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontFamily = verdanaFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * BLOG CARD
 * - Diseño rectangular y alineado arriba.
 */
/**
 * BLOG CARD
 * - Diseño rectangular y alineado arriba.
 * - AHORA CON SCROLL EN EL TEXTO
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BlogCardBox(
    item: CarouselItem,
    navController: NavController
) {
    val (context, uriHandler, vibrator) = getHelpersBox()
    val textShadow = getTextShadowBox()

    // 1. ESTADO PARA EL SCROLL
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .clickable(enabled = item.targetUrl != null) {
                handleClickBox(item, context, uriHandler, vibrator, navController)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // FOTO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl != null) {
                    GlideImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        requestBuilderTransform = { it.apply(RequestOptions().override(800).skipMemoryCache(true)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TEXTOS
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // TÍTULO
                Text(
                    text = item.title?.uppercase() ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    fontFamily = verdanaFontFamily,
                    lineHeight = 22.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // --- TEXTO PROBLEMÁTICO ---
                Text(
                    text = item.artist ?: "", // Aquí llega el contenido gracias al ViewModel

                    // PRUEBA DEL SEMÁFORO: COLOR AMARILLO
                    // Si sigue saliendo blanco, el código no se ha actualizado.
                    color = Color.White,

                    style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow),
                    textAlign = TextAlign.Start,
                    fontFamily = verdanaFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,

                    // --- LA SOLUCIÓN: SCROLL ---
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp) // Damos más altura
                        .verticalScroll(scrollState) // Permitimos deslizar

                    // IMPORTANTE: NO HAY maxLines NI overflow AQUÍ
                )
            }
        }
    }
}

// --- HELPERS ---

@Composable
fun getHelpersBox(): Triple<android.content.Context, androidx.compose.ui.platform.UriHandler, mentat.music.com.mentapp.ui.VibrationHelper> {
    return Triple(LocalContext.current, LocalUriHandler.current, rememberVibrator())
}

fun getTextShadowBox() = Shadow(
    color = Color.Black.copy(alpha = 0.8f),
    offset = Offset(2f, 2f),
    blurRadius = 4f
)

fun handleClickBox(
    item: CarouselItem,
    context: android.content.Context,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    vibrator: mentat.music.com.mentapp.ui.VibrationHelper,
    navController: NavController
) {
    if (item.targetUrl == null) return
    vibrator.vibrateClick()
    if (item.appPackageName != null) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.targetUrl))
        intent.setPackage(item.appPackageName)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            uriHandler.openUri(item.targetUrl)
        }
    } else {
        navController.navigate(AppScreens.WebViewScreen.createRoute(item.targetUrl))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalPagerIndicatorBox(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.4f)
) {
    Row(
        modifier = modifier
            .height(20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) activeColor else inactiveColor
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
