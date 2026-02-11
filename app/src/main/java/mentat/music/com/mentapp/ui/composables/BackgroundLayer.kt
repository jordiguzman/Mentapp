package mentat.music.com.mentapp.ui.composables

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import mentat.music.com.mentapp.ui.screens.home.DialConstants

@Composable
fun BackgroundLayer(
    modifier: Modifier = Modifier,
    isFrozen: Boolean,
    frozenTime: Float,
    isBlueMode: Boolean
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AttractorBackground(
            modifier = modifier,
            isFrozen = isFrozen,
            frozenTime = frozenTime,
            isBlueMode = isBlueMode
        )
    } else {
        // ✅ Dos videos con crossfade
        VideoBackgroundDual(
            modifier = modifier,
            isBlueMode = isBlueMode
        )
    }
}
