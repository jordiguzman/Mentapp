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
import androidx.compose.ui.text.TextStyle
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

    // ESTRUCTURA PRINCIPAL DEL CARRUSEL
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
        // Quitamos Arrangement.Center para controlar nosotros la posición manual
    ) {

        // 1. EMPUJÓN HACIA ABAJO (Weight)
        // Esto empuja el carrusel hacia la mitad inferior de la pantalla
        Spacer(Modifier.weight(1f))

        // 2. EL CARRUSEL
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                // No le damos weight para que ocupe solo lo que necesita
                // y se quede pegado abajo gracias al Spacer superior
                .padding(bottom = 16.dp),
            contentPadding = PaddingValues(horizontal = sidePadding),
            pageSpacing = 16.dp
        ) { pageIndex ->
            val item = items[pageIndex]
            AlbumCard(
                item = item,
                navController = navController
            )
        }

        // 3. INDICADOR (PUNTITOS)
        HorizontalPagerIndicator(pagerState = pagerState)

        // 4. MARGEN INFERIOR FINAL (Pequeño aire abajo del todo)
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * LA TARJETA (Alineación corregida: Texto abajo)
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AlbumCard(
    item: CarouselItem,
    navController: NavController
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val vibrator = rememberVibrator()
    val isClickable = item.targetUrl != null

    val isNews = !item.content.isNullOrBlank()
    val displayTitle = item.title?.uppercase() ?: ""
    val displaySubtitle = if (isNews) {
        item.artist ?: item.category ?: ""
    } else {
        item.artist ?: ""
    }

    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f),
        offset = Offset(2f, 2f),
        blurRadius = 4f
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // 0.65f: Hacemos la tarjeta ALTA (Formato móvil vertical)
            // Cuanto menor es el número, más alta es la tarjeta.
            .aspectRatio(1.1f)
            .clickable(enabled = isClickable) {
                if (item.targetUrl == null) return@clickable
                vibrator.vibrateClick()
                if (item.appPackageName != null) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.targetUrl))
                    intent.setPackage(item.appPackageName)
                    try { context.startActivity(intent) } catch (e: ActivityNotFoundException) { uriHandler.openUri(item.targetUrl) }
                } else {
                    navController.navigate(AppScreens.WebViewScreen.createRoute(item.targetUrl))
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // -----------------------------------------------------
            // 1. LA IMAGEN (65% DEL ESPACIO VERTICAL)
            // Usamos 'weight' para que se adapte elásticamente.
            // -----------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f) // <--- OJO: Ocupa el 65% de la altura disponible
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Transparent),
                    contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl != null) {
                    GlideImage(
                        model = item.imageUrl,
                        contentDescription = displayTitle,

                        // --- CAMBIO CLAVE AQUÍ ---
                        // En lugar de fillMaxSize() a secas (100%), ponemos un factor (ej. 0.92f = 92%).
                        // Al sobrar un 8%, y estar alineado al CENTRO, se crea un padding relativo automático
                        // por los 4 lados. Si quieres más aire, baja a 0.85f.
                        modifier = Modifier
                            .fillMaxSize(0.92f)
                            // Opcional: Si quieres que la imagen tenga sus propias esquinas redondas
                            // porque ya no toca los bordes del contenedor:
                            .clip(RoundedCornerShape(12.dp)),

                        // Alignment.Center es vital aquí para que ese "aire" se reparta equitativamente
                        alignment = Alignment.Center,


                        requestBuilderTransform = {
                            it.apply(RequestOptions().override(800).skipMemoryCache(true))
                        }
                    )
                }
            }

            // -----------------------------------------------------
            // 2. LOS TEXTOS (35% DEL ESPACIO VERTICAL)
            // Se quedan con el resto del espacio asegurado.
            // -----------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f) // <--- OJO: Ocupa el 35% restante
                    .padding(horizontal = 4.dp),
                // Arrangement.SpaceEvenly distribuye el espacio disponible entre los textos
                // para que no queden ni pegados arriba ni abajo.
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // CATEGORÍA (Si la hay)
                if (isNews && item.category != null) {
                    Text(
                        text = item.category.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center,
                        fontFamily = verdanaFontFamily,
                        maxLines = 1,
                        style = TextStyle(shadow = textShadow)
                    )
                }

                // TÍTULO
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontFamily = verdanaFontFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    lineHeight = 18.sp
                )

                // AUTOR / FECHA
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow),
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontFamily = verdanaFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )
            }
        }
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
            .height(20.dp) // Altura reducida
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