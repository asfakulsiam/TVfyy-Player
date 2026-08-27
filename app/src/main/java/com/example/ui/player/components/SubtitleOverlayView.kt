package com.example.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SubtitlePosition
import com.example.domain.model.SubtitleStyleConfig
import kotlin.math.roundToInt

@Composable
fun SubtitleOverlayView(
    activeCuesText: String?,
    subtitleStyle: SubtitleStyleConfig,
    onAdjustVerticalOffset: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeCuesText.isNullOrBlank()) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("subtitle_overlay_layer")
    ) {
        val containerHeight = maxHeight.value
        val baseVerticalFraction = when (subtitleStyle.position) {
            SubtitlePosition.TOP -> 0.10f
            SubtitlePosition.CENTER -> 0.48f
            SubtitlePosition.BOTTOM -> 0.86f
        }

        val effectivePercent = (baseVerticalFraction + subtitleStyle.customVerticalOffsetPercent).coerceIn(0.06f, 0.94f)
        val yOffsetPx = (containerHeight * effectivePercent).dp

        val textColor = Color(subtitleStyle.textColorHex)
        val backgroundColor = Color(subtitleStyle.backgroundColorHex)
        val outlineColor = Color(subtitleStyle.outlineColorHex)

        val textShadow = if (subtitleStyle.hasShadow) {
            Shadow(
                color = outlineColor.copy(alpha = 0.85f),
                offset = Offset(subtitleStyle.outlineWidth, subtitleStyle.outlineWidth),
                blurRadius = 4f
            )
        } else null

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = (containerHeight * effectivePercent * density).roundToInt()) }
                    .widthIn(max = 680.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaPercent = dragAmount.y / (size.height * 10f)
                            onAdjustVerticalOffset((subtitleStyle.customVerticalOffsetPercent + deltaPercent).coerceIn(-0.35f, 0.15f))
                        }
                    }
                    .testTag("subtitle_text_box")
            ) {
                Text(
                    text = activeCuesText,
                    color = textColor,
                    fontSize = subtitleStyle.effectiveFontSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    lineHeight = (subtitleStyle.effectiveFontSizeSp * 1.25f).sp,
                    style = TextStyle(shadow = textShadow),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
