package com.example.catchme

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

val TargetColors = listOf(
    Color(0xFFFFEA00),
    Color(0xFF00E5FF),
    Color(0xFFFF1744),
    Color(0xFFE040FB),
    Color(0xFFFF9100),
    Color(0xFF76FF03),
    Color(0xFFFF4081),
    Color(0xFF40C4FF)
)

@Composable
fun RadarScreen(
    isScanning: Boolean,
    stepCount: Int,
    distanceWalked: Float,
    headingDegrees: Int,
    cardinalHeading: String,
    targets: List<RadarBlip>,
    onToggleScan: () -> Unit,
    onReset: () -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "sweepAnimation")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarSweep"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06090D))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CATCH ME // TACTICAL RADAR",
            color = Color(0xFF00FF66),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard("STEPS", "$stepCount", Modifier.weight(1f))
            MetricCard("WALKED", "%.1fm".format(distanceWalked), Modifier.weight(1f))
            MetricCard("TARGETS", "${targets.size}", Modifier.weight(1f), highlight = targets.isNotEmpty())
        }

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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (targets.isEmpty()) "Scanning for nearby devices..."
                        else "Detected Targets (${targets.size})",
                        color = if (targets.isEmpty()) Color.Gray else Color(0xFF00FF66),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isDropdownExpanded) "▲ Hide" else "▼ Details",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp
                    )
                }

                AnimatedVisibility(visible = isDropdownExpanded && targets.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        targets.forEachIndexed { index, target ->
                            val color = TargetColors[index % TargetColors.size]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF091118), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .background(color, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = target.deviceId.take(22),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "RSSI: ${target.maxRssi} dBm | Peak: ${target.peakHeadingDegrees}°",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Air: %.1fm".format(target.openAirDistance),
                                        color = color,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Wall: ~%.1fm".format(target.wallDistance),
                                        color = color.copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF04070A), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF132330), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val boxWidth = constraints.maxWidth.toFloat()
            val boxHeight = constraints.maxHeight.toFloat()
            val centerX = boxWidth / 2f
            val centerY = boxHeight / 2f
            val maxRadius = minOf(boxWidth, boxHeight) * 0.42f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val rangeRings = listOf(1f, 3f, 5f, 10f)
                rangeRings.forEach { meters ->
                    val radius = (meters / 10f) * maxRadius * 2f
                    if (radius < maxRadius * 1.1f) {
                        drawCircle(Color(0xFF0D2030), radius, Offset(centerX, centerY))
                    }
                }

                drawLine(
                    color = Color(0xFF1E3A4A),
                    start = Offset(centerX, centerY - maxRadius),
                    end = Offset(centerX, centerY - maxRadius * 0.65f),
                    strokeWidth = 2f
                )

                if (isScanning) {
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val sweepX = centerX + (maxRadius * sin(sweepRad)).toFloat()
                    val sweepY = centerY - (maxRadius * cos(sweepRad)).toFloat()
                    drawLine(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF00FF66).copy(alpha = 0.5f), Color.Transparent),
                            Offset(centerX, centerY),
                            Offset(sweepX, sweepY)
                        ),
                        start = Offset(centerX, centerY),
                        end = Offset(sweepX, sweepY),
                        strokeWidth = 2.5f
                    )
                }

                targets.forEachIndexed { index, target ->
                    val color = TargetColors[index % TargetColors.size]
                    val relativeAngleDeg = ((target.peakHeadingDegrees - headingDegrees) + 360) % 360
                    val relativeRad = Math.toRadians(relativeAngleDeg.toDouble())

                    val clampedRssi = target.maxRssi.toFloat().coerceIn(-95f, -30f)
                    val radialFraction = ((clampedRssi - (-30f)) / (-95f - (-30f))).coerceIn(0.15f, 0.9f)
                    val radialDistance = radialFraction * maxRadius

                    val targetX = centerX + (sin(relativeRad) * radialDistance).toFloat()
                    val targetY = centerY - (cos(relativeRad) * radialDistance).toFloat()

                    drawCircle(color.copy(alpha = 0.28f), 24f, Offset(targetX, targetY))
                    drawCircle(color, 11f, Offset(targetX, targetY))

                    drawContext.canvas.nativeCanvas.drawText(
                        "${index + 1}",
                        targetX - 5f,
                        targetY + 6f,
                        Paint().apply {
                            this.color = android.graphics.Color.BLACK
                            textSize = 16f
                            isFakeBoldText = true
                        }
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF00FF66).copy(alpha = 0.2f), CircleShape)
                    )
                    Image(
                        painter = painterResource(R.drawable.prof),
                        contentDescription = "User",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFF00FF66), CircleShape)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0C1721),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF00FF66)))
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "🧭 $cardinalHeading $headingDegrees°",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onToggleScan,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) Color(0xFFFF3D00) else Color(0xFF00FF66)
                )
            ) {
                Text(
                    text = if (isScanning) "STOP RADAR" else "START RADAR",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(0.5f)
            ) {
                Text("RESET", color = Color.White)
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color.Gray, fontSize = 10.sp)
            Text(
                text = value,
                color = if (highlight) Color(0xFFFF1744) else Color(0xFF00E5FF),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}