package com.example.catchme

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

val TargetColors = listOf(
    Color(0xFFFFEA00), // Yellow
    Color(0xFF00E5FF), // Cyan
    Color(0xFFFF1744), // Red
    Color(0xFFE040FB), // Purple
    Color(0xFFFF9100), // Orange
    Color(0xFF76FF03), // Lime
    Color(0xFFFF4081), // Pink
    Color(0xFF40C4FF)  // Light Blue
)

@Composable
fun RadarScreen(
    isScanning: Boolean,
    stepCount: Int,
    distanceWalked: Float,
    userX: Float,
    userY: Float,
    headingDegrees: Int,
    cardinalHeading: String,
    corridorWidthMeters: Float,
    onCorridorWidthChanged: (Float) -> Unit,
    walkPath: List<Offset>,
    targets: List<RadarBlip>,
    onToggleScan: () -> Unit,
    onReset: () -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06090D))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Title ───────────────────────────────────────────────────────
        Text(
            "⚡ CATCH ME // TACTICAL RADAR ⚡",
            color = Color(0xFF00FF66),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )

        // ── Metrics ─────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard("STEPS",    "$stepCount",                    Modifier.weight(1f))
            MetricCard("WALKED",   "%.1fm".format(distanceWalked),  Modifier.weight(1f))
            MetricCard("TARGETS",  "${targets.size}",               Modifier.weight(1f),
                highlight = targets.isNotEmpty())
        }

        // ── Target List Dropdown ─────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .clickable { isDropdownExpanded = !isDropdownExpanded },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A24)),
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF00FF66)))
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (targets.isEmpty()) "📡 Scanning — Walk Around to Detect"
                        else                   "🎯 Targets In Range (${targets.size})",
                        color = if (targets.isEmpty()) Color.Gray else Color(0xFF00FF66),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (isDropdownExpanded) "▲ Hide" else "▼ Details",
                        color = Color(0xFF00E5FF), fontSize = 11.sp
                    )
                }

                AnimatedVisibility(visible = isDropdownExpanded && targets.isNotEmpty()) {
                    Column(
                        Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        targets.forEachIndexed { i, target ->
                            val col = TargetColors[i % TargetColors.size]
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF091118), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Numbered dot
                                    Box(
                                        Modifier.size(18.dp).background(col, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${i + 1}", color = Color.Black,
                                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            target.deviceId.take(22),
                                            color = Color.White,
                                            fontSize = 12.sp, fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "RSSI: ${target.maxRssi} dBm  |  Peak dir: ${target.peakHeadingDegrees}°",
                                            color = Color.Gray, fontSize = 10.sp
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Air: %.1fm".format(target.openAirDistance),
                                        color = col, fontSize = 11.sp, fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Wall: ~%.1fm".format(target.wallDistance),
                                        color = col.copy(alpha = 0.7f), fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Radar Canvas ─────────────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF04070A), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF132330), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val bw = constraints.maxWidth.toFloat()
            val bh = constraints.maxHeight.toFloat()
            val cx = bw / 2f
            val cy = bh / 2f

            // Max visual radius on screen = 42% of shortest dimension
            val maxRadius = minOf(bw, bh) * 0.42f

            Canvas(Modifier.fillMaxSize()) {

                // ── Range rings ────────────────────────────────────────
                val ringLabels = listOf(1f, 3f, 5f, 10f)   // meters
                ringLabels.forEach { meters ->
                    val r = (meters / 10f) * maxRadius * 2f
                    if (r < maxRadius * 1.1f) {
                        drawCircle(Color(0xFF0D2030), r, Offset(cx, cy))
                    }
                }

                // ── "N" direction tick (always up = North) ─────────────
                drawLine(
                    color = Color(0xFF1E3A4A),
                    start = Offset(cx, cy - maxRadius),
                    end   = Offset(cx, cy - maxRadius * 0.65f),
                    strokeWidth = 2f
                )

                // ── Sweep beam ─────────────────────────────────────────
                if (isScanning) {
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val sx = cx + (maxRadius * sin(sweepRad)).toFloat()
                    val sy = cy - (maxRadius * cos(sweepRad)).toFloat()
                    drawLine(
                        Brush.linearGradient(
                            listOf(Color(0xFF00FF66).copy(0.5f), Color.Transparent),
                            Offset(cx, cy), Offset(sx, sy)
                        ),
                        Offset(cx, cy), Offset(sx, sy), strokeWidth = 2.5f
                    )
                }

                // ── Target blips (heading-relative positioning) ─────────
                // Each target appears at the compass angle where signal peaked,
                // relative to where the user is currently facing.
                // → If you turn to face the watch, it moves to the TOP of the screen.
                targets.forEachIndexed { i, target ->
                    val col = TargetColors[i % TargetColors.size]

                    // Angular difference: how far is the peak direction from current heading
                    val relAngleDeg = ((target.peakHeadingDegrees - headingDegrees) + 360) % 360
                    val relRad      = Math.toRadians(relAngleDeg.toDouble())

                    // Radial distance: scale RSSI to 10% – 90% of maxRadius
                    // Stronger signal (higher dBm value, less negative) → closer to center
                    val rssiClamped = target.maxRssi.toFloat().coerceIn(-95f, -30f)
                    // Map -30 dBm → 15% of radius, -95 dBm → 90% of radius
                    val radialFraction = ((rssiClamped - (-30f)) / (-95f - (-30f)))
                        .coerceIn(0.15f, 0.9f)
                    val radialPx = radialFraction * maxRadius

                    val tx = cx + (sin(relRad) * radialPx).toFloat()
                    val ty = cy - (cos(relRad) * radialPx).toFloat()

                    // Pulse ring
                    drawCircle(col.copy(alpha = 0.28f), 24f, Offset(tx, ty))
                    // Solid blip
                    drawCircle(col, 11f, Offset(tx, ty))

                    // Numbered label inside blip (clean, no external text)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${i + 1}",
                        tx - 5f, ty + 6f,
                        Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 16f
                            isFakeBoldText = true
                        }
                    )
                }
            }

            // ── Avatar pinned at center ─────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(48.dp).background(Color(0xFF00FF66).copy(0.2f), CircleShape))
                    Image(
                        painterResource(R.drawable.prof),
                        contentDescription = "You",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFF00FF66), CircleShape)
                    )
                }
                // Compass badge under avatar
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0C1721),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF00E5FF), Color(0xFF00FF66))
                        )
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        "🧭 $cardinalHeading $headingDegrees°",
                        color = Color.White,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Buttons ──────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onToggleScan,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) Color(0xFFFF3D00) else Color(0xFF00FF66)
                )
            ) {
                Text(
                    if (isScanning) "STOP RADAR" else "START RADAR",
                    color = Color.Black, fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(0.5f)) {
                Text("RESET", color = Color.White)
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String, value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C141C)),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(Color(0xFF1B2C3B), Color(0xFF0C141C)))
        )
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color.Gray, fontSize = 10.sp)
            Text(
                value,
                color = if (highlight) Color(0xFFFF1744) else Color(0xFF00E5FF),
                fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}