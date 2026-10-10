package com.soulstream.app.data

import android.content.Context

object Prefs {

    private const val FILE = "soulstream_settings"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ---- downloads ----
    fun defaultQuality(ctx: Context): Int = sp(ctx).getInt("default_quality", 0)
    fun setDefaultQuality(ctx: Context, q: Int) = sp(ctx).edit().putInt("default_quality", q).apply()

    fun askQuality(ctx: Context): Boolean = sp(ctx).getBoolean("ask_quality", true)
    fun setAskQuality(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("ask_quality", v).apply()

    fun turbo(ctx: Context): Boolean = sp(ctx).getBoolean("turbo", true)
    fun setTurbo(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("turbo", v).apply()

    // ---- browser ----
    fun adBlock(ctx: Context): Boolean = sp(ctx).getBoolean("ad_block", true)
    fun setAdBlock(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("ad_block", v).apply()

    fun keepLogin(ctx: Context): Boolean = sp(ctx).getBoolean("keep_login", true)
    fun setKeepLogin(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("keep_login", v).apply()

    fun autoClipboard(ctx: Context): Boolean = sp(ctx).getBoolean("auto_clipboard", true)
    fun setAutoClipboard(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("auto_clipboard", v).apply()

    // ---- look ----
    fun accent(ctx: Context): Int = sp(ctx).getInt("accent", 0)
    fun setAccent(ctx: Context, i: Int) = sp(ctx).edit().putInt("accent", i).apply()

    // ---- engine ----
    fun engineUpdatedAt(ctx: Context): Long = sp(ctx).getLong("last_engine_update", 0L)
    fun setEngineUpdatedAt(ctx: Context, t: Long) = sp(ctx).edit().putLong("last_engine_update", t).apply()

    // ---- ad-free download passes (earned by watching ads in free time) ----
    fun adPasses(ctx: Context): Int = sp(ctx).getInt("ad_passes", 0)
    fun setAdPasses(ctx: Context, n: Int) = sp(ctx).edit().putInt("ad_passes", n.coerceAtLeast(0)).apply()
    fun addAdPasses(ctx: Context, delta: Int) = setAdPasses(ctx, adPasses(ctx) + delta)

    // ---- diagnostics: the last crash, so the user can report it ----
    fun lastCrash(ctx: Context): String? = sp(ctx).getString("last_crash", null)
    fun setLastCrash(ctx: Context, msg: String) =
        sp(ctx).edit().putString("last_crash", msg.take(600)).apply()
    fun clearLastCrash(ctx: Context) = sp(ctx).edit().remove("last_crash").apply()
}
