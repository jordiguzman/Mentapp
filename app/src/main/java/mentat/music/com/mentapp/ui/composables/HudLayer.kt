package mentat.music.com.mentapp.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mentat.music.com.mentapp.R
import mentat.music.com.mentapp.ui.screens.home.viewmodel.HomeViewModel
import mentat.music.com.mentapp.ui.theme.VerdanaFontFamily

@Composable
fun HudLayer(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    isVibrationOn: Boolean,
    currentLanguage: HomeViewModel.Language,
    onVibrationToggle: () -> Unit,
    onExitClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onBackClick: () -> Unit
) {
    if (!isVisible) return

    Box(modifier = modifier) {  // ← SIN padding aquí

        // 1. VIBRACIÓN
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)  // ← Este es el único padding necesario
                .size(48.dp)
                .clickable { onVibrationToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (isVibrationOn)
                        R.drawable.ic_vibration_foreground
                    else
                        R.drawable.ic_vibration_no_foreground
                ),
                contentDescription = "Vibración",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
        }

        // 2. SALIR
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .size(48.dp)
                .clickable { onExitClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_power),
                contentDescription = "Salir",
                colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f)),
                modifier = Modifier.size(28.dp)
            )
        }

        // 3. IDIOMA
        val buttonText = if (currentLanguage == HomeViewModel.Language.ES) "EN" else "ES"
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 35.dp, end = 80.dp)  // ← Valores originales de HomeScreen
                .clickable { onLanguageClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 20.sp,
                fontFamily = VerdanaFontFamily,
                fontWeight = FontWeight.Bold
            )
        }

        // 4. BACK
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}