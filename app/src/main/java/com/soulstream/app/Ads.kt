package com.soulstream.app

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Rewarded ad helper for SoulStream.
 *
 * One rewarded ad is preloaded and shown right before a download starts (the
 * user picks a quality in the Share sheet, watches the ad, then the download
 * begins). If no ad is available the caller is released immediately, so a
 * failed or unfilled ad never blocks a download.
 */
object Ads {

    /**
     * AdMob rewarded ad unit for SoulStream (LIVE — real ads).
     *
     * The Google official TEST unit is kept commented below; swap to it only if
     * you need to test again, since tapping your own live ads is an AdMob policy
     * violation and can flag the account.
     */
    // LIVE (real) rewarded ad unit for SoulStream.
    private const val REWARDED_UNIT_ID = "ca-app-pub-7206645274499834/5234358761"

    // Google official TEST rewarded ad unit (only for development/testing):
    // private const val REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    @Volatile private var rewarded: RewardedAd? = null
    @Volatile private var loading = false
    @Volatile private var initialised = false

    /** Initialise the Mobile Ads SDK once (call from Application.onCreate). */
    fun init(context: Context) {
        if (initialised) return
        initialised = true
        try {
            MobileAds.initialize(context) { }
        } catch (t: Throwable) {
            // Never let an ad problem crash the app.
        }
    }

    /** Preload a rewarded ad so it is ready the moment the user taps a quality. */
    fun load(context: Context) {
        if (rewarded != null || loading) return
        loading = true
        try {
            RewardedAd.load(
                context,
                REWARDED_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewarded = ad
                        loading = false
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewarded = null
                        loading = false
                    }
                }
            )
        } catch (t: Throwable) {
            rewarded = null
            loading = false
        }
    }

    /** True when a rewarded ad is loaded and ready to show. */
    fun isReady(): Boolean = rewarded != null

    /**
     * Show the preloaded rewarded ad, then run [onDone].
     *
     * [onDone] is ALWAYS called exactly once — after the ad is dismissed, after
     * a show failure, or immediately if no ad was ready — so the download
     * proceeds either way.
     */
    fun show(activity: Activity, onDone: () -> Unit) {
        val ad = rewarded
        if (ad == null) {
            load(activity)
            onDone()
            return
        }
        rewarded = null
        var done = false
        val finishOnce = {
            if (!done) {
                done = true
                onDone()
                load(activity) // preload the next one
            }
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finishOnce()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finishOnce()
        }
        try {
            ad.show(activity) { /* reward earned — we proceed on dismissal */ }
        } catch (t: Throwable) {
            finishOnce()
        }
    }

    /**
     * Show the rewarded ad for the "watch in free time to earn a pass" flow and
     * report whether the user actually earned the reward (watched to the end).
     * [onResult] is always called once — with false if no ad was available.
     */
    fun showForReward(activity: Activity, onResult: (earned: Boolean) -> Unit) {
        val ad = rewarded
        if (ad == null) {
            load(activity)
            onResult(false)
            return
        }
        rewarded = null
        var earned = false
        var done = false
        val finishOnce = {
            if (!done) {
                done = true
                onResult(earned)
                load(activity)
            }
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finishOnce()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finishOnce()
        }
        try {
            ad.show(activity) { earned = true }
        } catch (t: Throwable) {
            finishOnce()
        }
    }
}
