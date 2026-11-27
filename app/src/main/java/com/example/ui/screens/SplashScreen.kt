package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.i18n.LocalAppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class SnowParticleSpec(
    val xFraction: Float,
    val yOffset: Float,
    val radiusDp: Float,
    val speedFactor: Float,
    val swayAmplitudeDp: Float,
    val phase: Float,
    val alpha: Float,
    val isCrystal: Boolean
)

private data class OrbitSnowSpec(
    val angleOffsetDeg: Float,
    val radiusMultiplier: Float,
    val sizeDp: Float,
    val phase: Float,
    val isGoldLight: Boolean
)

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isDark = isSystemInDarkTheme()
    val primary = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48)
    val festiveGold = if (isDark) Color(0xFFFCD34D) else Color(0xFFD97706)
    val pineGreen = if (isDark) Color(0xFF34D399) else Color(0xFF15803D)
    val titleColor = if (isDark) Color(0xFFF9FAFB) else Color(0xFF111827)
    val subtitleColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563)

    val logoScale = remember { Animatable(0.78f) }
    val logoAlpha = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }
    val accentScale = remember { Animatable(0.5f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(14f) }
    val progressAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "subtle_holiday_splash")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )
    val logoBreath by infiniteTransition.animateFloat(
        initialValue = 0.988f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_breath"
    )
    val twinklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle_phase"
    )
    val snowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_snow_orbit"
    )

    val logoOrbitParticles = remember {
        listOf(
            OrbitSnowSpec(-62f, 0.435f, 2.6f, 0.0f, true),
            OrbitSnowSpec(-20f, 0.460f, 3.2f, 1.1f, false),
            OrbitSnowSpec(25f, 0.435f, 2.4f, 2.0f, true),
            OrbitSnowSpec(72f, 0.455f, 3.0f, 2.9f, false),
            OrbitSnowSpec(118f, 0.435f, 2.6f, 3.8f, true),
            OrbitSnowSpec(165f, 0.460f, 3.4f, 4.7f, false),
            OrbitSnowSpec(212f, 0.435f, 2.5f, 5.4f, true),
            OrbitSnowSpec(255f, 0.455f, 3.1f, 0.7f, false)
        )
    }

    val loadingProgress by animateFloatAsState(
        targetValue = if (progressAlpha.value > 0f) 1f else 0f,
        animationSpec = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
        label = "loading_progress"
    )

    LaunchedEffect(Unit) {
        launch {
            ringProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 860, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            delay(200)
            accentScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        delay(360)
        launch {
            titleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
            )
        }
        launch {
            titleOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
            )
        }

        delay(260)
        progressAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
        delay(1180)
        onAnimationFinished()
    }

    val backgroundBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color(0xFF070C14), Color(0xFF101827), Color(0xFF1E0B14))
            } else {
                listOf(Color(0xFFFFFFFF), Color(0xFFF6FAFF), Color(0xFFFFF1F2))
            }
        )
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        val isLandscapeCompact = maxWidth > maxHeight && maxHeight < 500.dp
        val haloContainerSize = if (isLandscapeCompact) 188.dp else 254.dp
        val logoCircleSize = if (isLandscapeCompact) 130.dp else 176.dp
        val hollySize = if (isLandscapeCompact) 30.dp else 38.dp
        val hollyOffsetX = if (isLandscapeCompact) (-22).dp else (-30).dp
        val hollyOffsetY = if (isLandscapeCompact) 18.dp else 26.dp
        val starSize = if (isLandscapeCompact) 16.dp else 20.dp
        val starOffsetX = if (isLandscapeCompact) 24.dp else 34.dp
        val starOffsetY = if (isLandscapeCompact) 22.dp else 30.dp

        // Soft ambient holiday aura behind the logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val auraCenter = if (isLandscapeCompact) {
                Offset(size.width * 0.34f, size.height * 0.5f)
            } else {
                Offset(size.width * 0.5f, size.height * 0.35f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = if (isDark) 0.20f else 0.13f),
                        primary.copy(alpha = 0f)
                    ),
                    center = auraCenter,
                    radius = size.minDimension * 0.48f * haloPulse
                ),
                radius = size.minDimension * 0.50f * haloPulse,
                center = auraCenter
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        festiveGold.copy(alpha = if (isDark) 0.10f else 0.08f),
                        festiveGold.copy(alpha = 0f)
                    ),
                    center = auraCenter,
                    radius = size.minDimension * 0.30f
                ),
                radius = size.minDimension * 0.32f,
                center = auraCenter
            )
        }

        val logoBadge: @Composable () -> Unit = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(haloContainerSize)
                    .testTag("splash_logo_container")
            ) {
                // Subtle festive rings, twinkling micro-lights & floating snow crystals around the central logo
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val outerRingRadius = size.minDimension * 0.395f
                    val innerRingRadius = size.minDimension * 0.365f
                    val sweep = 360f * ringProgress.value

                    drawCircle(
                        color = primary.copy(alpha = if (isDark) 0.08f else 0.06f),
                        radius = outerRingRadius * haloPulse,
                        center = center
                    )

                    if (sweep > 1f) {
                        // Primary rose-gold festive arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.85f),
                                    festiveGold.copy(alpha = 0.85f),
                                    pineGreen.copy(alpha = 0.75f),
                                    primary.copy(alpha = 0.85f)
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(center.x - outerRingRadius, center.y - outerRingRadius),
                            size = Size(outerRingRadius * 2f, outerRingRadius * 2f),
                            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Delicate inner evergreen accent arc
                        drawArc(
                            color = pineGreen.copy(alpha = 0.42f),
                            startAngle = 35f,
                            sweepAngle = (150f * ringProgress.value),
                            useCenter = false,
                            topLeft = Offset(center.x - innerRingRadius, center.y - innerRingRadius),
                            size = Size(innerRingRadius * 2f, innerRingRadius * 2f),
                            style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Gentle orbiting micro-lights & crisp snow crystals around the logo perimeter
                    val crystalTint = if (isDark) Color.White else Color(0xFF60A5FA)
                    logoOrbitParticles.forEachIndexed { idx, spec ->
                        val revealThreshold = idx.toFloat() / logoOrbitParticles.size
                        if (ringProgress.value < revealThreshold) return@forEachIndexed

                        val driftAngle = spec.angleOffsetDeg + sin((twinklePhase + spec.phase).toDouble()).toFloat() * 4f
                        val angleRad = Math.toRadians(driftAngle.toDouble())
                        val r = size.minDimension * spec.radiusMultiplier
                        val pos = Offset(
                            x = center.x + (r * cos(angleRad)).toFloat(),
                            y = center.y + (r * sin(angleRad)).toFloat() +
                                cos((snowPhase * 2f * PI + spec.phase).toDouble()).toFloat() * 2.5.dp.toPx()
                        )
                        val twinkle = 0.45f + 0.55f * ((sin(twinklePhase + spec.phase) + 1f) * 0.5f)

                        if (spec.isGoldLight) {
                            val lightColor = if (idx % 4 == 0) festiveGold else primary
                            drawCircle(
                                color = lightColor.copy(alpha = 0.22f * twinkle),
                                radius = 7.dp.toPx(),
                                center = pos
                            )
                            drawCircle(
                                color = lightColor.copy(alpha = 0.90f * twinkle),
                                radius = spec.sizeDp.dp.toPx(),
                                center = pos
                            )
                        } else {
                            drawSnowCrystal(
                                center = pos,
                                radius = spec.sizeDp.dp.toPx() * 1.4f,
                                color = crystalTint.copy(alpha = 0.72f * twinkle)
                            )
                        }
                    }
                }

                // Official FOCOM Logo centered inside soft luminous border
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(logoCircleSize)
                        .scale(logoScale.value * logoBreath)
                        .alpha(logoAlpha.value)
                        .shadow(20.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .border(1.5.dp, festiveGold.copy(alpha = 0.42f), CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.focom_logo_1779471672893),
                        contentDescription = strings.logoDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Subtle festive holly sprig & berries accent on the upper-right rim
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = hollyOffsetX, y = hollyOffsetY)
                        .size(hollySize)
                        .scale(accentScale.value)
                        .alpha(logoAlpha.value)
                ) {
                    drawHollyAccent(
                        pineColor = pineGreen,
                        berryColor = primary,
                        highlightColor = Color.White
                    )
                }

                // Delicate four-point holiday star sparkle on upper-left rim
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = starOffsetX, y = starOffsetY)
                        .size(starSize)
                        .scale(accentScale.value)
                        .alpha(logoAlpha.value * (0.65f + 0.35f * sin(twinklePhase)))
                ) {
                    drawStarSparkle(
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.minDimension * 0.45f,
                        color = festiveGold
                    )
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (isLandscapeCompact) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    modifier = Modifier.testTag("splash_landscape_row")
                ) {
                    logoBadge()

                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "FOCOM UES ILIAD",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = titleColor,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .alpha(titleAlpha.value)
                                .offset(y = titleOffset.value.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = strings.splashSubtitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subtitleColor,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.alpha(titleAlpha.value)
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        LinearProgressIndicator(
                            progress = { loadingProgress },
                            modifier = Modifier
                                .width(172.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .alpha(progressAlpha.value),
                            color = primary,
                            trackColor = if (isDark) Color(0xFF1F2937) else Color(0xFFE5E7EB)
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    logoBadge()

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "FOCOM UES ILIAD",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                        color = titleColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .alpha(titleAlpha.value)
                            .offset(y = titleOffset.value.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = strings.splashSubtitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subtitleColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(titleAlpha.value)
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    LinearProgressIndicator(
                        progress = { loadingProgress },
                        modifier = Modifier
                            .width(172.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .alpha(progressAlpha.value),
                        color = primary,
                        trackColor = if (isDark) Color(0xFF1F2937) else Color(0xFFE5E7EB)
                    )
                }
            }
        }

        // Foreground Snowfall Canvas effect overlaying the startup screen and central logo
        Snowfall(
            isDark = isDark,
            modifier = Modifier
                .fillMaxSize()
                .alpha(logoAlpha.value)
                .testTag("splash_snowfall_overlay")
        )
    }
}

@Composable
fun Snowfall(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "snowfall_overlay")
    val fallProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowfall_progress"
    )

    val particles = remember {
        listOf(
            // Gentle foreground snowflakes passing over and around the logo zone
            SnowParticleSpec(0.38f, 0.14f, 2.5f, 1.0f, 8f, 0.4f, 0.42f, true),
            SnowParticleSpec(0.46f, 0.36f, 1.9f, 1.2f, 6f, 1.8f, 0.34f, false),
            SnowParticleSpec(0.54f, 0.22f, 2.7f, 0.9f, 9f, 2.7f, 0.46f, true),
            SnowParticleSpec(0.61f, 0.44f, 1.7f, 1.1f, 6f, 0.9f, 0.32f, false),
            SnowParticleSpec(0.42f, 0.56f, 2.2f, 1.0f, 7f, 3.5f, 0.36f, false),
            SnowParticleSpec(0.57f, 0.62f, 2.4f, 0.85f, 8f, 4.1f, 0.40f, true),
            // Surrounding screen snowflakes
            SnowParticleSpec(0.08f, 0.10f, 2.2f, 0.95f, 8f, 0.2f, 0.38f, true),
            SnowParticleSpec(0.16f, 0.58f, 1.6f, 0.75f, 6f, 1.1f, 0.28f, false),
            SnowParticleSpec(0.24f, 0.28f, 2.6f, 1.15f, 10f, 2.4f, 0.44f, true),
            SnowParticleSpec(0.31f, 0.79f, 1.5f, 0.85f, 5f, 0.8f, 0.28f, false),
            SnowParticleSpec(0.69f, 0.18f, 2.1f, 1.05f, 7f, 3.1f, 0.36f, false),
            SnowParticleSpec(0.76f, 0.48f, 2.5f, 0.90f, 9f, 1.9f, 0.42f, true),
            SnowParticleSpec(0.84f, 0.15f, 2.7f, 1.20f, 11f, 3.8f, 0.45f, true),
            SnowParticleSpec(0.91f, 0.70f, 1.7f, 0.85f, 7f, 1.5f, 0.30f, false),
            SnowParticleSpec(0.12f, 0.88f, 1.9f, 0.95f, 7f, 5.1f, 0.34f, false),
            SnowParticleSpec(0.29f, 0.45f, 2.0f, 1.05f, 8f, 2.2f, 0.34f, false),
            SnowParticleSpec(0.73f, 0.86f, 2.3f, 0.95f, 8f, 1.3f, 0.38f, true),
            SnowParticleSpec(0.94f, 0.33f, 1.6f, 1.10f, 5f, 3.4f, 0.30f, false),
            SnowParticleSpec(0.05f, 0.41f, 1.9f, 0.88f, 6f, 2.9f, 0.32f, false),
            SnowParticleSpec(0.50f, 0.82f, 1.8f, 1.12f, 6f, 0.6f, 0.30f, false)
        )
    }

    Canvas(modifier = modifier) {
        val snowDotColor = if (isDark) Color(0xFFE0F2FE) else Color(0xFF93C5FD)
        val snowCrystalColor = if (isDark) Color.White else Color(0xFF60A5FA)

        particles.forEach { flake ->
            val yFraction = (flake.yOffset + fallProgress * flake.speedFactor) % 1f
            // Smooth fade near top and bottom bounds so particles enter & exit gracefully
            val edgeFade = when {
                yFraction < 0.08f -> yFraction / 0.08f
                yFraction > 0.90f -> (1f - yFraction) / 0.10f
                else -> 1f
            }.coerceIn(0f, 1f)
            val effectiveAlpha = flake.alpha * edgeFade
            if (effectiveAlpha <= 0.01f) return@forEach

            val swayX = sin((yFraction * 2f * PI + flake.phase).toDouble()).toFloat() * flake.swayAmplitudeDp.dp.toPx()
            val centerFlake = Offset(
                x = size.width * flake.xFraction + swayX,
                y = size.height * yFraction
            )
            val radiusPx = flake.radiusDp.dp.toPx()
            if (flake.isCrystal) {
                drawSnowCrystal(
                    center = centerFlake,
                    radius = radiusPx * 1.4f,
                    color = snowCrystalColor.copy(alpha = effectiveAlpha)
                )
            } else {
                drawCircle(
                    color = snowDotColor.copy(alpha = effectiveAlpha),
                    radius = radiusPx,
                    center = centerFlake
                )
            }
        }
    }
}

private fun DrawScope.drawHollyAccent(
    pineColor: Color,
    berryColor: Color,
    highlightColor: Color
) {
    val leafBase = Offset(size.width * 0.42f, size.height * 0.56f)

    // Left holly leaf
    rotate(degrees = -28f, pivot = leafBase) {
        val leafPath = Path().apply {
            moveTo(leafBase.x, leafBase.y)
            quadraticTo(leafBase.x - size.width * 0.24f, leafBase.y - size.height * 0.26f, leafBase.x, leafBase.y - size.height * 0.44f)
            quadraticTo(leafBase.x + size.width * 0.24f, leafBase.y - size.height * 0.26f, leafBase.x, leafBase.y)
            close()
        }
        drawPath(path = leafPath, color = pineColor)
    }

    // Right holly leaf
    rotate(degrees = 42f, pivot = leafBase) {
        val leafPath = Path().apply {
            moveTo(leafBase.x, leafBase.y)
            quadraticTo(leafBase.x - size.width * 0.22f, leafBase.y - size.height * 0.24f, leafBase.x, leafBase.y - size.height * 0.42f)
            quadraticTo(leafBase.x + size.width * 0.22f, leafBase.y - size.height * 0.24f, leafBase.x, leafBase.y)
            close()
        }
        drawPath(path = leafPath, color = pineColor.copy(alpha = 0.88f))
    }

    // Trio of crisp ruby holly berries
    val berryPositions = listOf(
        Offset(leafBase.x - 3.dp.toPx(), leafBase.y + 1.dp.toPx()),
        Offset(leafBase.x + 3.5.dp.toPx(), leafBase.y + 2.dp.toPx()),
        Offset(leafBase.x + 0.5.dp.toPx(), leafBase.y - 3.5.dp.toPx())
    )
    berryPositions.forEach { pos ->
        drawCircle(color = berryColor, radius = 3.2.dp.toPx(), center = pos)
        drawCircle(
            color = highlightColor.copy(alpha = 0.65f),
            radius = 1.0.dp.toPx(),
            center = Offset(pos.x - 0.9.dp.toPx(), pos.y - 0.9.dp.toPx())
        )
    }
}

private fun DrawScope.drawSnowCrystal(
    center: Offset,
    radius: Float,
    color: Color
) {
    for (i in 0 until 3) {
        val angleRad = Math.toRadians((i * 60).toDouble())
        val dx = (radius * cos(angleRad)).toFloat()
        val dy = (radius * sin(angleRad)).toFloat()
        drawLine(
            color = color,
            start = Offset(center.x - dx, center.y - dy),
            end = Offset(center.x + dx, center.y + dy),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawStarSparkle(
    center: Offset,
    radius: Float,
    color: Color
) {
    val inner = radius * 0.28f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
    drawPath(path = path, color = color)
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.35f),
        style = Stroke(width = 0.7.dp.toPx(), join = StrokeJoin.Round)
    )
}
