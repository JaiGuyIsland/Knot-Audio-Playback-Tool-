package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playback.LoopMode
import com.example.ui.theme.KnotAccent
import com.example.ui.theme.KnotBorder
import com.example.ui.theme.KnotSurfaceCard

@Composable
fun LoopBadge(
    loopMode: LoopMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaylist = loopMode == LoopMode.PLAYLIST
    val borderCol by animateColorAsState(
        targetValue = if (isPlaylist) KnotAccent.copy(alpha = 0.5f) else Color(0xFF818CF8).copy(alpha = 0.5f),
        label = "badge_border"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(KnotSurfaceCard)
            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("loop_badge_toggle"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPlaylist) Icons.Rounded.AllInclusive else Icons.Rounded.RepeatOne,
            contentDescription = "Loop Mode",
            tint = if (isPlaylist) KnotAccent else Color(0xFF818CF8),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isPlaylist) "Continuous Playlist Loop" else "Single Track Loop",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
