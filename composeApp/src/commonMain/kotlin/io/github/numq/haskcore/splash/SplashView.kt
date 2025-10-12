package io.github.numq.haskcore.splash

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
internal fun SplashView(mainImage: Painter, outlineImage: Painter) {
    val thickness = 4.dp

    val mainImageSize = with(LocalDensity.current) {
        mainImage.intrinsicSize.toDpSize() - DpSize(thickness, thickness)
    }

    var animatedAlpha by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(0f, 1f, animationSpec = tween(1_000)) { value, _ ->
            animatedAlpha = value
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(color = Color.White)
            .alpha(animatedAlpha),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = outlineImage,
            contentDescription = null,
            contentScale = ContentScale.None
        )
        Image(
            painter = mainImage,
            contentDescription = null,
            modifier = Modifier.size(mainImageSize),
            contentScale = ContentScale.None
        )
    }
}