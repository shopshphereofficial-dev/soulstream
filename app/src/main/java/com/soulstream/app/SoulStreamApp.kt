package com.soulstream.app

import android.app.Application
import android.content.Context
import com.soulstream.app.data.Prefs
import com.soulstream.app.engine.Diag
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

/**
 * Application + engine bootstrap.
 *
 * Everything here is guarded: a failure is recorded (engineError / Diag) and
 * shown in the UI instead of leaving the app stuck with no explanation.
 */
class SoulStreamApp : Application() {

    override fun onCreate() {
        super.onCreate()
        installCrashRecorder()
        Ads.init(this)
        Thread { initEngine(this, force = false) }.start()
    }

    private fun installCrashRecorder() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val where = throwable.stackTrace.firstOrNull()
                val msg = buildString {
                    append(throwable.javaClass.name)
                    append(": ")
                    append(throwable.message ?: "")
                    if (where != null) {
                        append("  @ ")
                        append(where.fileName ?: "?")
                        append(":")
                        append(where.lineNumber)
                    }
                }
                Prefs.setLastCrash(this, msg)
                Diag.log(this, "crash", msg)
            } catch (e: Throwable) {
                // nothing we can do here
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            }
        }
    }

    companion object {

        @Volatile
        var engineReady = false

        /** Why the engine could not start (null when fine). */
        @Volatile
        var engineError: String? = null

        /** Extra note about the yt-dlp refresh. */
        @Volatile
        var engineNote: String? = null

        private val lock = Object()

        /**
         * The library wraps the real failure (e.g. a missing native library or a
         * resource that could not be unpacked) inside a generic "failed to
         * initialize" YoutubeDLException. Walking the cause chain turns the
         * unhelpful top-level message into the actual reason, which is what the
         * UI and the diagnostics report show.
         */
        private fun describe(e: Throwable): String {
            val sb = StringBuilder()
            var t: Throwable? = e
            var depth = 0
            while (t != null && depth < 6) {
                if (depth > 0) sb.append("  <-  ")
                sb.append(t.javaClass.simpleName)
                val m = t.message
                if (!m.isNullOrBlank()) sb.append(": ").append(m)
                t = t.cause
                depth++
            }
            return sb.toString()
        }

        /**
         * Initialises Python + ffmpeg + aria2c and makes sure yt-dlp itself is
         * present (the library does NOT ship yt-dlp - it is fetched once and
         * then refreshed daily). Never throws; records why on failure.
         */
        fun initEngine(ctx: Context, force: Boolean) {
            synchronized(lock) {
                if (engineReady && !force) return

                try {
                    YoutubeDL.getInstance().init(ctx)
                    FFmpeg.getInstance().init(ctx)
                    Aria2c.getInstance().init(ctx)
                    engineReady = true
                    engineError = null
                    Diag.log(ctx, "engine", "init ok")
                } catch (e: Throwable) {
                    engineReady = false
                    engineError = describe(e)
                    Diag.log(ctx, "engine", "init FAILED: $engineError")
                    return
                }

                // yt-dlp must exist for any download to work.
                try {
                    val last = Prefs.engineUpdatedAt(ctx)
                    val now = System.currentTimeMillis()
                    val stale = now - last > 24L * 60 * 60 * 1000
                    if (force || stale) {
                        YoutubeDL.getInstance()
                            .updateYoutubeDL(ctx, YoutubeDL.UpdateChannel.STABLE)
                        Prefs.setEngineUpdatedAt(ctx, now)
                        engineNote = "yt-dlp refreshed"
                        Diag.log(ctx, "engine", "yt-dlp refreshed")
                    } else {
                        Diag.log(ctx, "engine", "yt-dlp already fresh")
                    }
                } catch (e: Throwable) {
                    val m = e.message ?: e.javaClass.simpleName
                    engineNote = "yt-dlp refresh failed: $m"
                    Diag.log(ctx, "engine", "yt-dlp refresh FAILED: $m")
                }
            }
        }

        /**
         * Blocking wait for the downloader. Retries init a few times, then
         * throws with the real reason so the user sees it.
         */
        fun awaitEngine(ctx: Context, timeoutMs: Long) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (!engineReady && System.currentTimeMillis() < deadline) {
                initEngine(ctx, force = false)
                if (!engineReady) Thread.sleep(800)
            }
            if (!engineReady) {
                throw IllegalStateException(
                    "engine not ready - " + (engineError ?: "unknown reason")
                )
            }
        }
    }
}
