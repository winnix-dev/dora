package com.winnix.adsdk

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.winnix.adsdk.databinding.ActivityMainBinding
import com.winnix.dora.Dora
import com.winnix.dora.admob_manager.NativeLayout
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.model.AdType
import com.winnix.dora.model.AdUnit
import com.winnix.dora.model.AdmobBannerSize

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
        name = "ca-app-pub-3940256099942544/2247696110",
        adType = AdType.Native
    )

    private val banner = AdUnit(
        id = "ca-app-pub-3940256099942544/9214589741",
        name = "",
        adType = AdType.Banner
    )

    private val openApp = AdUnit(
        id = "ca-app-pub-3940256099942544/9257395921",
        name = "",
        adType = AdType.OpenApp
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)


        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Dora.initialize(
            this,
        )

        Dora.setUpYandex(
            intersUnit = inter.id,
            nativeUnit = native.id,
            bannerUnit = banner.id,
        )

        Dora.loadInterstitial(
            this,
            inter.id,
            object : LoadInterstitialCallback {

            }
        )

        binding.apply {
            btnInters.setOnClickListener {
                
                Dora.showInterstitial(
                    activity = this@MainActivity,
                    callback = object : ShowInterstitialCallback {
                        override fun onDismiss() {
                            Log.d(TAG, "On Inters Dismiss")
                        }

                        override fun onShowFailed() {
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
                    adSize = AdmobBannerSize.Adaptive,
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

        }
    }
}