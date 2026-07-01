package com.hom.monitor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hom.monitor.data.model.CoreInfo

@Composable
fun CpuCoreGrid(cores: List<CoreInfo>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (cores.size <= 4) 2 else 4),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.heightIn(max = if (cores.size <= 4) 140.dp else 280.dp)
    ) {
        items(cores) { core ->
            CoreCard(core = core)
        }
    }
}

@Composable
private fun CoreCard(core: CoreInfo) {
    val usageColor = when {
        !core.online -> MaterialTheme.colorScheme.outline
        core.usage > 80f -> MaterialTheme.colorScheme.error
        core.usage > 50f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "CPU${core.index}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (core.online) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "%.0f%%".format(core.usage),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = usageColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            val freqText = if (!core.online) "离线"
            else if (core.frequency > 0) formatFrequency(core.frequency)
            else "-- MHz"

            Text(
                freqText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )

            if (core.online) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { core.usage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = usageColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

private fun formatFrequency(khz: Long): String {
    val mhz = khz / 1000
    return if (mhz >= 1000) {
        "%.1f GHz".format(mhz / 1000f)
    } else {
        "$mhz MHz"
    }
}
