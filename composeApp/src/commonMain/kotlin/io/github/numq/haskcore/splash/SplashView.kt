package io.github.numq.haskcore.splash

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
internal fun SplashView(painter: Painter) {
    var animatedAlpha by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(0f, 1f, animationSpec = tween(1_000)) { value, _ ->
            animatedAlpha = value
        }
    }

    Card(shape = RoundedCornerShape(8.dp)) {
        Box(
            modifier = Modifier.fillMaxSize().alpha(animatedAlpha).background(color = Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painter, contentDescription = null, contentScale = ContentScale.None)
        }
    }
}