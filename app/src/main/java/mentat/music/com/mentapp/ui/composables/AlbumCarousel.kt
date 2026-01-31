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

// --- FUENTES ---
private val verdanaFontFamily = FontFamily(
    Font(R.font.verdana_regular, FontWeight.Normal),
    Font(R.font.verdana_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.verdana_bold, FontWeight.Bold),
    Font(R.font.verdana_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumCarousel(
    modifier: Modifier = Modifier,
    items: List<CarouselItem>,
    navController: NavController,
    isConceptMode: Boolean, // <--- ESTA ES LA CLAVE PARA ELEGIR DISEÑO
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

    // ESTRUCTURA PRINCIPAL DEL CARRUSEL
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 1. EMPUJÓN HACIA ABAJO
        Spacer(Modifier.weight(1f))

        // 2. EL CARRUSEL
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentPadding = PaddingValues(horizontal = sidePadding),
            pageSpacing = 16.dp
        ) { pageIndex ->
            val item = items[pageIndex]

            // --- CORRECCIÓN: EL "CEREBRO" DE LA SELECCIÓN ---
            // No nos fiamos solo de 'isConceptMode'. Miramos el item.
            // Si el item tiene "content" (texto largo), ES UN BLOG/NOTICIA por narices.
            val esBlog = !item.content.isNullOrBlank()

            if (esBlog || isConceptMode) {
                // Ahora sí: Esto forzará que entre aquí tu tarjeta de Blog
                BlogCard(item = item, navController = navController)
            } else {
                MusicCard(item = item, navController = navController)
            }
        }

        // 3. INDICADOR (PUNTITOS)
        HorizontalPagerIndicator(pagerState = pagerState)

        // 4. MARGEN INFERIOR FINAL
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * DISEÑO 1: CARD DE MÚSICA
 * - Prioridad: Imagen muy grande (75% altura).
 * - Texto: Centrado y corto abajo.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MusicCard(
    item: CarouselItem,
    navController: NavController
) {
    // Helpers de navegación y vibración
    val (context, uriHandler, vibrator) = getHelpers()
    val textShadow = getTextShadow()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .clickable(enabled = item.targetUrl != null) {
                handleClick(item, context, uriHandler, vibrator, navController)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // IMAGEN GIGANTE (75%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl != null) {
                    GlideImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxSize(0.95f) // Casi llena el hueco
                            .clip(RoundedCornerShape(12.dp)),
                        alignment = Alignment.Center,
                        requestBuilderTransform = { it.apply(RequestOptions().override(800).skipMemoryCache(true)) }
                    )
                }
            }

            // TEXTO COMPACTO (25%)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.Center, // Centrado verticalmente en su hueco
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
 * DISEÑO 2: CARD DE BLOG (ConceptMode)
 * - Prioridad: Legibilidad del texto.
 * - Estructura: Imagen más pequeña arriba, texto alineado arriba-izquierda.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BlogCard(
    item: CarouselItem,
    navController: NavController
) {
    val (context, uriHandler, vibrator) = getHelpers()
    val textShadow = getTextShadow()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // 1. VOLVEMOS A FIJAR EL TAMAÑO (Para que no flote en el limbo)
            // 0.85f = Una tarjeta alta (estilo naipe).
            .widthIn(max = 400.dp)
            .clickable(enabled = item.targetUrl != null) {
                handleClick(item, context, uriHandler, vibrator, navController)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 2. LA ORDEN SUPREMA: ANCLAR ARRIBA
                // "Arrangement.Top" obliga a todo el contenido a pegarse al techo.
                // Se acabó el flotar en medio.
                .padding(12.dp), // Un poco de margen general para que no toque los bordes del card
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- LA IMAGEN ---
            // Tamaño fijo. Mentalidad MSX: "Mide 190 píxeles y punto".
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp) // Altura Fija
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.TopCenter // Alineación interna Arriba
            ) {
                if (item.imageUrl != null) {
                    GlideImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(), // Llena los 190dp
                        //contentScale = ContentScale.Crop,  // Recorta para llenar sin deformar
                        requestBuilderTransform = { it.apply(RequestOptions().override(800).skipMemoryCache(true)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- EL TEXTO ---
            // Se pintará justo debajo del Spacer.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top, // Aseguramos que el texto también empiece arriba
                horizontalAlignment = Alignment.Start  // Alineado a la izquierda
            ) {
                // TÍTULO
                Text(
                    text = item.title?.uppercase() ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    fontFamily = verdanaFontFamily,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // DESCRIPCIÓN
                Text(
                    text = item.artist ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow),
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start,
                    fontFamily = verdanaFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- HELPERS PARA NO REPETIR CÓDIGO ---

@Composable
fun getHelpers(): Triple<android.content.Context, androidx.compose.ui.platform.UriHandler, mentat.music.com.mentapp.ui.VibrationHelper> {
    return Triple(LocalContext.current, LocalUriHandler.current, rememberVibrator())
}

fun getTextShadow() = Shadow(
    color = Color.Black.copy(alpha = 0.8f),
    offset = Offset(2f, 2f),
    blurRadius = 4f
)

fun handleClick(
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
fun HorizontalPagerIndicator(
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