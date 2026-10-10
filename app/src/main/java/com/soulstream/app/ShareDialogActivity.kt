package com.soulstream.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soulstream.app.data.Prefs
import com.soulstream.app.engine.Engine
import com.soulstream.app.service.DownloadService
import com.soulstream.app.ui.components.NeonCard
import com.soulstream.app.ui.components.PopIn
import com.soulstream.app.ui.components.QualityRow
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.SoulTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The popup you get when you tap Share -> SoulStream in another app. */
class ShareDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val url = Regex("https?://\\S+").find(text ?: "")?.value
        if (url == null) {
            Toast.makeText(this, "No link found in the shared text", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            SoulTheme(accentIndex = Prefs.accent(this)) {
                val scope = rememberCoroutineScope()
                var launching by remember { mutableStateOf(false) }

                // Preload the rewarded ad while the sheet is open.
                LaunchedEffect(Unit) { Ads.load(this@ShareDialogActivity) }

                val appear = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    appear.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 320f))
                }

                val accentA = MaterialTheme.colorScheme.primary
                val accentB = MaterialTheme.colorScheme.secondary

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.62f * appear.value))
                        .systemBarsPadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .graphicsLayer {
                                val s = if (launching) 0.9f else 0.94f + 0.06f * appear.value
                                scaleX = s
                                scaleY = s
                                alpha = if (launching) 0f else appear.value
                                translationY = (1f - appear.value) * 70f
                            }
                    ) {
                        NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(listOf(accentA, accentB))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = Color(0xFF03060E),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "GRAB IT",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = OnDark
                                    )
                                    Text(
                                        url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = Muted)
                                }
                            }
                            Spacer(Modifier.height(10.dp))

                            val passes = Prefs.adPasses(this@ShareDialogActivity)
                            if (passes > 0) {
                                Text(
                                    if (passes == 1) "1 free pass - no ad this time"
                                    else "$passes free passes - no ad this time",
                                    color = accentA,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(6.dp))
                            }

                            Column(
                                Modifier
                                    .height(330.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Engine.QUALITIES.forEachIndexed { i, q ->
                                    PopIn(delayMillis = 60 * i) {
                                        QualityRow(q) {
                                            if (!launching) {
                                                launching = true
                                                // If the user banked a pass (watched an ad in
                                                // free time), skip the ad this time.
                                                if (Prefs.adPasses(this@ShareDialogActivity) > 0) {
                                                    Prefs.addAdPasses(this@ShareDialogActivity, -1)
                                                    startIt(url, q.index)
                                                    finish()
                                                } else {
                                                    // Otherwise give a still-loading ad a short
                                                    // moment to arrive, then show it.
                                                    scope.launch {
                                                        var waited = 0
                                                        while (!Ads.isReady() && waited < 3500) {
                                                            delay(150)
                                                            waited += 150
                                                        }
                                                        Ads.show(this@ShareDialogActivity) {
                                                            startIt(url, q.index)
                                                            finish()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startIt(url: String, quality: Int) {
        Engine.askNotificationPermission(this)
        DownloadService.start(this, url, quality)
        Toast.makeText(
            this,
            "Download started - " + Engine.qualityLabel(quality),
            Toast.LENGTH_SHORT
        ).show()
    }
}
