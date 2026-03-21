package io.github.numq.haskcore.feature.bootstrap.presentation.feature

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import io.github.numq.haskcore.feature.bootstrap.core.Bootstrap
import org.jetbrains.skia.Image
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun BootstrapView(
    applicationScope: Scope,
    handleError: (Throwable) -> Unit,
    title: String,
    icon: Painter,
    exitApplication: () -> Unit,
    content: @Composable (Bootstrap) -> Unit
) {
    if (applicationScope.closed) return

    val feature = koinInject<BootstrapFeature>(scope = applicationScope)

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is BootstrapEvent.HandleFailure -> handleError(event.throwable)

                is BootstrapEvent.ExitApplication -> exitApplication()
            }
        }
    }

    when (val currentState = state) {
        is BootstrapState.Active -> {
            val bitmap = remember {
                useResource("drawable/logo.webp") { inputStream ->
                    Image.makeFromEncoded(inputStream.readBytes()).toComposeImageBitmap()
                }
            }

            val windowState = rememberWindowState(
                position = WindowPosition(Alignment.Center), size = DpSize(width = 800.dp, height = 200.dp)
            )

            Window(
                onCloseRequest = exitApplication,
                state = windowState,
                title = title,
                icon = icon,
                undecorated = true,
                transparent = true,
                resizable = false
            ) {
                WindowDraggableArea(modifier = Modifier.fillMaxSize()) {
                    val animatedAlpha by produceState(0f) {
                        animate(0f, 1f, animationSpec = tween(durationMillis = 1_000)) { animatedValue, _ ->
                            value = animatedValue
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize().alpha(animatedAlpha),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        contentColor = Color.Transparent
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                contentScale = ContentScale.None,
                                colorFilter = ColorFilter.tint(color = Color.Black)
                            )
                        }
                    }
                }
            }
        }

        is BootstrapState.Content -> content(currentState.bootstrap)
    }
}