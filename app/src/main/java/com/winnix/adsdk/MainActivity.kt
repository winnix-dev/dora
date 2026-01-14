package com.winnix.adsdk

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdSize
import com.winnix.adsdk.databinding.ActivityMainBinding
import com.winnix.dora.Dora
import com.winnix.dora.admob_manager.NativeLayout
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.callback.ShowRewardedCallback
import com.winnix.dora.model.AdType
import com.winnix.dora.model.AdUnit
import com.winnix.dora.model.AdmobBannerSize
import com.winnix.dora.model.DoraAdError

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "Tagg MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    private val inter = AdUnit(
        id = "ca-app-pub-3940256099942544/1033173712",
        name = "",
        adType = AdType.Inters
    )

    private val native = AdUnit(
        id = "ca-app-pub-3940256099942544/2247696110",
//        id = "ca-app-pub-3940256099942544/2247696111",
        name = "ca-app-pub-3940256099942544/2247696110",
        adType = AdType.Native
    )

    private val banner = AdUnit(
        id = "ca-app-pub-3940256099942544/9214589741",
        name = "",
        adType = AdType.Banner
    )

    private val rewarded = AdUnit(
        id = "ca-app-pub-3940256099942544/9214589741",
        name = "",
        adType = AdType.Rewarded
    )

    private val openApp = AdUnit(
        id = "ca-app-pub-3940256099942544/9257395921",
        name = "",
        adType = AdType.OpenApp
    )

    private val interYandex = AdUnit(
        id = "demo-interstitial-yandex",
        name = "",
        adType = AdType.Inters
    )
    private val nativeYandex = AdUnit(
        id = "demo-native-yandex",
        name = "ca-app-pub-3940256099942544/2247696110",
        adType = AdType.Native
    )
    private val bannerYandex = AdUnit(
        id = "demo-banner-yandex",
        name = "",
        adType = AdType.Banner
    )
    private val rewardedYandex = AdUnit(
        id = "demo-interstitial-yandex",
        name = "",
        adType = AdType.Inters
    )

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Dora.initializeAdmob(
            this,
        )

        Dora.setUpYandex(
            intersUnit = interYandex.id,
            nativeUnit = nativeYandex.id,
            bannerUnit = bannerYandex.id,
            rewardedUnit = rewardedYandex.adType.getYandexDebugId()
        )

        Dora.loadRewardedAd(
            context = this,
            id = rewarded.adType.getAdmobDebugId()
        )

        binding.apply {
            btnInters.setOnClickListener {
                Dora.loadInterstitial(
                    this@MainActivity,
                    inter.id,
                    native.id,
                    object : LoadInterstitialCallback {

                    }
                )

                Dora.showInterstitial(
                    activity = this@MainActivity,
                    callback = object : ShowInterstitialCallback {
                        override fun onDismiss() {
                            Log.d(TAG, "On Inters Dismiss")
                        }

                        override fun onShowFailed(doraAdError: DoraAdError) {
                            Log.d(TAG, "On Inters Show Failed")
                        }

                        override fun onShow() {
                            Log.d(TAG, "On Inters Show")
                        }
                    }
                )
            }

            btnNative.setOnClickListener {
                Dora.loadAndShowNative(
                    activity = this@MainActivity,
                    lifecycleOwner = this@MainActivity,
                    viewGroup = binding.flAd,
                    layout = NativeLayout.Native50,
                    id = native.id
                )
            }

            btnBanner.setOnClickListener {
                Dora.loadBanner(
                    activity = this@MainActivity,
                    container = binding.flAd,
                    adSize = AdmobBannerSize.InlineAdaptive(250),
                    lifecycleOwner = this@MainActivity,
                    adUnitId = banner.id
                )
            }

            btnOpenAd.setOnClickListener {
                Dora.showOpenAd(
                    6000L,
                    this@MainActivity,
                ) {
                    Log.d(TAG, "Close Open App")
                }
            }

            btnToggleOpenAd.setOnClickListener {
                AdConfig.canShowOpenApp = !AdConfig.canShowOpenApp
                Log.d(TAG, "canShowOpenApp: ${AdConfig.canShowOpenApp}")
            }

            btnRewarded.setOnClickListener {
                Dora.showRewardedAd(
                    this@MainActivity, 6000L,
                    object : ShowRewardedCallback {
                        override fun showSuccess() {
                            Log.d(TAG, "Rewarded Show Success")
                        }
                    },
                )
            }

        }
    }
}