package com.soulstream.app.ui

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soulstream.app.Ads
import com.soulstream.app.SoulStreamApp
import com.soulstream.app.data.ActiveJob
import com.soulstream.app.data.LiveDownloads
import com.soulstream.app.data.Prefs
import com.soulstream.app.engine.AdBlock
import com.soulstream.app.engine.Engine
import com.soulstream.app.engine.ImageSaver
import com.soulstream.app.ui.components.AnimatedCounter
import com.soulstream.app.ui.components.GhostButton
import com.soulstream.app.ui.components.GlowButton
import com.soulstream.app.ui.components.NeonCard
import com.soulstream.app.ui.components.NeonRing
import com.soulstream.app.ui.components.PopIn
import com.soulstream.app.ui.components.PulseDot
import com.soulstream.app.ui.components.QualitySheet
import com.soulstream.app.ui.components.SectionLabel
import com.soulstream.app.ui.theme.Hairline
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.NeonAmber
import com.soulstream.app.ui.theme.NeonLime
import com.soulstream.app.ui.theme.NeonRed
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.Surface1
import com.soulstream.app.ui.theme.Surface2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Site(val name: String, val glyph: String, val url: String)

private val SITES = listOf(
    Site("YouTube", "\u25B6", "https://m.youtube.com"),
    Site("Instagram", "\u25CE", "https://www.instagram.com"),
    Site("Facebook", "f", "https://m.facebook.com"),
    Site("TikTok", "\u266A", "https://www.tiktok.com"),
    Site("Kwai", "K", "https://www.kwai.com"),
    Site("X", "\u2715", "https://x.com"),
    Site("Threads", "@", "https://www.threads.net"),
    Site("Pinterest", "P", "https://www.pinterest.com"),
    Site("Reddit", "R", "https://www.reddit.com"),
    Site("Snapchat", "\u25D5", "https://www.snapchat.com"),
    Site("SoundCloud", "\u2601", "https://soundcloud.com"),
    Site("DailyMotion", "D", "https://www.dailymotion.com"),
    Site("Twitch", "T", "https://m.twitch.tv"),
    Site("Vimeo", "V", "https://vimeo.com"),
    Site("Bluesky", "\u2609", "https://bsky.app"),
    Site("OK.ru", "OK", "https://m.ok.ru")
)

@Composable
fun HomeScreen(
    onOpenBrowser: (String) -> Unit,
    onStartDownload: (String, Int) -> Unit
) {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var engineReady by remember { mutableStateOf(SoulStreamApp.engineReady) }
    var engineErr by remember { mutableStateOf(SoulStreamApp.engineError) }
    var engineNote by remember { mutableStateOf(SoulStreamApp.engineNote) }
    var fixing by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var pendingUrl by remember { mutableStateOf("") }
    var jobs by remember { mutableStateOf<List<ActiveJob>>(emptyList()) }
    var clipLink by remember { mutableStateOf<String?>(null) }
    var showAdFree by remember { mutableStateOf(false) }
    var adPasses by remember { mutableStateOf(Prefs.adPasses(ctx)) }
    var watchingAd by remember { mutableStateOf(false) }
    val activity = ctx as? android.app.Activity
    val appear = remember { Animatable(0f) }

    val accentA = MaterialTheme.colorScheme.primary
    val accentB = MaterialTheme.colorScheme.secondary

    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        var ticks = 0
        while (ticks < 240) {
            engineReady = SoulStreamApp.engineReady
            engineErr = SoulStreamApp.engineError
            engineNote = SoulStreamApp.engineNote
            if (engineReady && engineErr == null) break
            ticks++
            delay(500)
        }
    }
    LaunchedEffect(Unit) {
        LiveDownloads.jobs.collectLatest { jobs = it }
    }
    LaunchedEffect(Unit) {
        if (Prefs.autoClipboard(ctx)) {
            val clip = readClipboard(ctx)
            if (clip != null && clip.startsWith("http") && clip.length in 10..400) {
                clipLink = clip
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 14.dp, bottom = 26.dp)
                .graphicsLayer {
                    alpha = appear.value
                    translationY = (1f - appear.value) * 30f
                }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeroLogo(accentA, accentB)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "SOULSTREAM",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            brush = Brush.horizontalGradient(listOf(accentA, accentB))
                        )
                    )
                    Text(
                        "Stream it. Grab it. Own it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            EngineChip(
                ready = engineReady,
                error = engineErr,
                fixing = fixing,
                accent = accentA
            ) {
                if (fixing) return@EngineChip
                fixing = true
                scope.launch {
                    val res = withContext(Dispatchers.IO) {
                        try {
                            SoulStreamApp.initEngine(ctx, force = true)
                            if (SoulStreamApp.engineReady) {
                                "Engine ready" + (SoulStreamApp.engineNote?.let { " - $it" } ?: "")
                            } else {
                                "Failed: " + (SoulStreamApp.engineError ?: "unknown")
                            }
                        } catch (e: Throwable) {
                            "Failed: ${e.message}"
                        }
                    }
                    engineReady = SoulStreamApp.engineReady
                    engineErr = SoulStreamApp.engineError
                    engineNote = SoulStreamApp.engineNote
                    fixing = false
                    Toast.makeText(ctx, res, Toast.LENGTH_LONG).show()
                }
            }

            if (!engineReady && engineErr != null) {
                Spacer(Modifier.height(10.dp))
                PopIn {
                    NeonCard(Modifier.fillMaxWidth(), accent = NeonRed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = NeonRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Downloads can't start",
                                style = MaterialTheme.typography.titleMedium,
                                color = OnDark
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(engineErr ?: "", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap REPAIR above, or open Settings - Diagnostics and send the report.",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            clipLink?.let { link ->
                Spacer(Modifier.height(12.dp))
                PopIn {
                    NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = accentA, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Link on clipboard",
                                style = MaterialTheme.typography.labelSmall,
                                color = Muted
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { clipLink = null }) {
                                Icon(Icons.Rounded.Close, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            link,
                            color = OnDark,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        GlowButton(
                            text = "Use this link",
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.Bolt,
                            accentA = accentA,
                            accentB = accentB
                        ) {
                            url = link
                            clipLink = null
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            PopIn {
                NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                    SectionLabel("Drop a link", accentA)
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://...  or type to search", color = Muted) },
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentA,
                            unfocusedBorderColor = Hairline,
                            focusedContainerColor = Surface2.copy(alpha = 0.45f),
                            unfocusedContainerColor = Surface2.copy(alpha = 0.45f),
                            focusedTextColor = OnDark,
                            unfocusedTextColor = OnDark,
                            cursorColor = accentA
                        ),
                        trailingIcon = {
                            IconButton(onClick = { pasteClipboard(ctx) { url = it; clipLink = null } }) {
                                Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste", tint = accentA)
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostButton(
                            text = "Paste",
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.ContentPaste,
                            accent = accentA
                        ) { pasteClipboard(ctx) { url = it; clipLink = null } }
                        GhostButton(
                            text = "Browser",
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Language,
                            accent = accentB
                        ) { onOpenBrowser("https://www.google.com") }
                    }
                    Spacer(Modifier.height(10.dp))
                    GlowButton(
                        text = "DOWNLOAD NOW",
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.Download,
                        accentA = accentA,
                        accentB = accentB
                    ) {
                        val input = url.trim()
                        when {
                            input.isEmpty() -> toast(ctx, "Paste a link first")
                            input.startsWith("http") -> scope.launch {
                                val isImg = withContext(Dispatchers.IO) {
                                    Engine.looksLikeImageUrl(input) ||
                                        Engine.probeIsImage(
                                            input,
                                            CookieManager.getInstance().getCookie(input)
                                        )
                                }
                                if (isImg) {
                                    val msg = withContext(Dispatchers.IO) {
                                        ImageSaver.save(ctx, input)
                                    }
                                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                                } else if (Prefs.askQuality(ctx)) {
                                    pendingUrl = input
                                    showSheet = true
                                } else {
                                    onStartDownload(input, Prefs.defaultQuality(ctx))
                                }
                            }
                            else -> onOpenBrowser(
                                "https://www.youtube.com/results?search_query=" + Uri.encode(input)
                            )
                        }
                    }
                    if (!engineReady) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "The engine is still starting - your download waits for it automatically.",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Ad-free passes", accentA)
            NeonCard(Modifier.fillMaxWidth(), accent = accentB) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(accentB, accentA))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFF03060E), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (adPasses == 1) "1 free pass" else "$adPasses free passes",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnDark
                        )
                        Text(
                            "Watch an ad now, then your next downloads skip the ad.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                GlowButton(
                    text = if (watchingAd) "LOADING AD..." else "WATCH AN AD  (+1 PASS)",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.PlayArrow,
                    accentA = accentA,
                    accentB = accentB
                ) {
                    val act = activity
                    if (!watchingAd && act != null) {
                        watchingAd = true
                        Ads.showForReward(act) { earned ->
                            watchingAd = false
                            if (earned) {
                                Prefs.addAdPasses(ctx, 1)
                                adPasses = Prefs.adPasses(ctx)
                                Toast.makeText(ctx, "Pass added - next download skips the ad", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(ctx, "Ad not completed - no pass added", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Quick grab", accentA)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Engine.QUALITIES.forEach { q ->
                    QuickChip(q.title, q.isAudio, accentA, accentB) {
                        val input = url.trim()
                        if (input.startsWith("http")) {
                            scope.launch {
                                val isImg = withContext(Dispatchers.IO) {
                                    Engine.looksLikeImageUrl(input) ||
                                        Engine.probeIsImage(
                                            input,
                                            CookieManager.getInstance().getCookie(input)
                                        )
                                }
                                if (isImg) {
                                    val msg = withContext(Dispatchers.IO) {
                                        ImageSaver.save(ctx, input)
                                    }
                                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    onStartDownload(input, q.index)
                                }
                            }
                        } else {
                            toast(ctx, "Paste a link first")
                        }
                    }
                }
            }

            val running = jobs.filter { it.isRunning }
            if (running.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("In the pipeline", accentA)
                running.forEach { job -> LiveJobCard(job, accentA, accentB) }
            }
            val finished = jobs.filter { !it.isRunning }
            if (finished.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                finished.forEach { job -> FinishedRow(job, accentA) }
                Spacer(Modifier.height(6.dp))
                GhostButton(
                    text = "Clear finished",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.ClearAll,
                    accent = accentA
                ) { LiveDownloads.clearFinished() }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Ad-free zone", accentA)
            NeonCard(Modifier.fillMaxWidth(), accent = accentB) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(accentB, accentA))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Public, contentDescription = null, tint = Color(0xFF03060E), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("YouTube without ads", style = MaterialTheme.typography.titleMedium, color = OnDark)
                        Text(
                            "Opens a Piped / Invidious mirror - no ad breaks",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                    }
                    GhostButton(text = "Open", icon = Icons.Rounded.PlayArrow, accent = accentB) {
                        showAdFree = true
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("All apps", accentA)
            SITES.chunked(4).forEachIndexed { rowIndex, row ->
                PopIn(delayMillis = rowIndex * 30) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        row.forEach { site ->
                            SiteTile(site, Modifier.weight(1f), accentA) { onOpenBrowser(site.url) }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
        }

        if (showSheet) {
            QualitySheet(
                onDismiss = { showSheet = false },
                onPick = { q ->
                    showSheet = false
                    onStartDownload(pendingUrl, q)
                }
            )
        }

        if (showAdFree) {
            AdFreeSheet(
                onDismiss = { showAdFree = false },
                onPick = { u ->
                    showAdFree = false
                    onOpenBrowser(u)
                }
            )
        }
    }
}

@Composable
private fun HeroLogo(accentA: Color, accentB: Color) {
    val t = rememberInfiniteTransition(label = "logo")
    val glow by t.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoGlow"
    )
    Box(
        Modifier
            .size(50.dp)
            .scale(glow)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(accentA, accentB))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color(0xFF03060E),
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun EngineChip(
    ready: Boolean,
    error: String?,
    fixing: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val tint = when {
        fixing -> NeonAmber
        ready && error == null -> NeonLime
        error != null -> NeonRed
        else -> NeonAmber
    }
    val label = when {
        fixing -> "REPAIRING ENGINE..."
        ready && error == null -> "ENGINE ONLINE - TURBO MODE"
        error != null -> "ENGINE ERROR - TAP TO REPAIR"
        else -> "WARMING UP THE ENGINE..."
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Surface1.copy(alpha = 0.8f))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulseDot(tint)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = if (ready && error == null) OnDark else tint,
            style = MaterialTheme.typography.labelSmall
        )
        if (error != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.SystemUpdateAlt,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun QuickChip(
    text: String,
    audio: Boolean,
    accentA: Color,
    accentB: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 850f),
        label = "chip"
    )
    val tint = if (audio) accentB else accentA
    Row(
        Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(50))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (audio) Icons.Rounded.MusicNote else Icons.Rounded.HighQuality,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = OnDark, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LiveJobCard(job: ActiveJob, accentA: Color, accentB: Color) {
    NeonCard(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        accent = if (job.status == ActiveJob.Status.FAILED) NeonRed else accentA
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (job.status == ActiveJob.Status.FAILED) {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = NeonRed,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(14.dp))
            } else {
                NeonRing(progress = job.progress / 100f, accentA = accentA, accentB = accentB)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    job.title,
                    color = OnDark,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                if (job.status == ActiveJob.Status.FAILED) {
                    Text("Download failed - check Settings > Diagnostics", color = NeonRed, style = MaterialTheme.typography.bodySmall)
                } else if (job.status == ActiveJob.Status.PREPARING) {
                    Text("Warming up the engine...", color = accentA, style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedCounter(value = job.progress, color = accentA)
                        Spacer(Modifier.width(8.dp))
                        Text("streaming to disk", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun FinishedRow(job: ActiveJob, accent: Color) {
    val ok = job.status == ActiveJob.Status.DONE
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = if (ok) NeonLime else NeonRed,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            job.title,
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { LiveDownloads.remove(job.id) }) {
            Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SiteTile(site: Site, modifier: Modifier = Modifier, accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 850f),
        label = "tile"
    )
    Column(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1.copy(alpha = 0.8f))
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(site.glyph, fontSize = 19.sp, color = accent, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(5.dp))
        Text(
            site.name,
            color = OnDark,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AdFreeSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accentA = MaterialTheme.colorScheme.primary
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 26.dp)
        ) {
            Text("Ad-free YouTube mirrors", style = MaterialTheme.typography.titleLarge, color = OnDark)
            Spacer(Modifier.height(4.dp))
            Text(
                "These front-ends serve YouTube without ad breaks. If one is slow, try the next.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            Spacer(Modifier.height(12.dp))
            AdBlock.AD_FREE_YOUTUBE.forEachIndexed { i, u ->
                PopIn(delayMillis = i * 45) {
                    NeonCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        accent = accentA
                    ) {
                        Row(
                            Modifier.clickable { onPick(u) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Public, contentDescription = null, tint = accentA, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(u, color = OnDark, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = accentA, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun readClipboard(ctx: Context): String? = try {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
} catch (e: Exception) {
    null
}

private fun pasteClipboard(ctx: Context, onText: (String) -> Unit) {
    val text = readClipboard(ctx)
    if (!text.isNullOrBlank()) onText(text) else toast(ctx, "Clipboard is empty")
}

private fun toast(ctx: Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}
