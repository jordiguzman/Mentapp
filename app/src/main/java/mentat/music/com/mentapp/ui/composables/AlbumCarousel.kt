package mentat.music.com.mentapp.ui.composables

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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

// --- (Definición de la fuente) ---
private val verdanaFontFamily = FontFamily(
    Font(R.font.verdana_regular, FontWeight.Normal),
    Font(R.font.verdana_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.verdana_bold, FontWeight.Bold),
    Font(R.font.verdana_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun AlbumCarousel(
    modifier: Modifier = Modifier,
    items: List<CarouselItem>,
    navController: NavController,
    isConceptMode: Boolean,
    initialPage: Int,
    onPageChanged: (Int) -> Unit
) {
    val sidePadding = if (isConceptMode) 16.dp else 48.dp

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

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = sidePadding),
            pageSpacing = if (isConceptMode) 16.dp else 0.dp
        ) { pageIndex ->
            val item = items[pageIndex]
            AlbumCard(
                item = item,
                navController = navController,
                isConceptMode = isConceptMode
            )
        }

        Spacer(Modifier.height(24.dp))

        HorizontalPagerIndicator(
            pagerState = pagerState
        )
    }
}

/**
 * La tarjeta individual LIMPIA Y CORREGIDA
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AlbumCard(
    item: CarouselItem,
    navController: NavController,
    isConceptMode: Boolean
) {
    // --- LÓGICA LIMPIA (Clean Architecture) ---
    // Si hay contenido en 'content', es una noticia/blog. Si está vacío, es música.
    val isNews = !item.content.isNullOrBlank()

    // ... (El resto de variables: uriHandler, context...)
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val vibrator = rememberVibrator()
    val isClickable = item.targetUrl != null

    // 2. ESTRUCTURA VISUAL
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // --- IMAGEN ---
        val imageAspectRatio = if (isConceptMode) 1.5f else 1f
        if (item.imageUrl != null) {
            GlideImage(
                model = item.imageUrl,
                contentDescription = item.title ?: "Portada",
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(imageAspectRatio)
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(24.dp), clip = false)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop,
                requestBuilderTransform = { it.apply(RequestOptions().override(600).skipMemoryCache(true)) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // --- A) CATEGORÍA (ETIQUETA AMARILLA) ---
        if (isNews) {
            // AQUÍ ESTABA EL ERROR: Usamos directamente item.category
            item.category?.let { categoryName ->
                Text(
                    text = categoryName.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow,
                    textAlign = TextAlign.Center,
                    fontFamily = verdanaFontFamily,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        // --- B) TÍTULO ---
        item.title?.let { title ->
            Text(
                text = title.uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontFamily = verdanaFontFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }

        Spacer(Modifier.height(8.dp))

        // --- C) CONTENIDO ---
        if (isNews) {
            // -- ES NOTICIA --
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // AQUÍ ESTABA EL OTRO ERROR: Usamos directamente item.content
                Text(
                    text = item.content ?: "",
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Start,
                    fontFamily = verdanaFontFamily,
                )

                // OPCIONAL: Si quieres mostrar la fecha (que ahora viene en artist) al final del texto:
                /*
                item.artist?.let { date ->
                    Spacer(Modifier.height(8.dp))
                    Text(text = date, fontSize = 12.sp, color = Color.Gray)
                }
                */

                Spacer(Modifier.height(16.dp))
            }
        } else {
            // -- ES MÚSICA --
            item.artist?.let { artistName ->
                Text(
                    text = artistName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontFamily = verdanaFontFamily,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
        }
    }
}


// --- (HorizontalPagerIndicator) ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalPagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.5f)
) {
    Row(
        modifier = modifier
            .height(30.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) activeColor else inactiveColor
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}