package com.winnix.adsdk

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.winnix.adsdk.databinding.ActivityMainBinding
import com.winnix.dora.model.AdConfig
import com.winnix.dora.Dora
import com.winnix.dora.callback.LoadNativeCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.helper.NativeLayout
import com.winnix.dora.model.AdmobBannerSize
import com.winnix.dora.model.AdmobType
import com.winnix.dora.model.AdmobUnit
import com.winnix.dora.rule.AdmobGuard
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "Tag MainActivity"
    }
    private lateinit var binding: ActivityMainBinding

    private val inter = AdmobUnit(
        id = "",
        name = "",
        adType = AdmobType.Inters
    )

    private val native1 = AdmobUnit(
        id = "",
        name = "",
        adType = AdmobType.Native
    )

    private val native2 = AdmobUnit(
        id = "",
        name = "",
        adType = AdmobType.Native
    )

    private val banner = AdmobUnit(
        id = "",
        name = "",
        adType = AdmobType.Banner
    )

    private val openApp = AdmobUnit(
        id = "",
        name = "",
        adType = AdmobType.OpenApp
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        splashScreen.setKeepOnScreenCondition {
            Dora.isInitialized
        }

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val adGuard = AdmobGuard().apply {
//            addRule(
//                PreConditionRule()
//            )
        }

        Dora.initialize(
            this,
            adConfig = AdConfig(
                isDebug = BuildConfig.DEBUG
            )
        )

        Dora.registerOpenAd(application, openApp)

        Dora.setNativeAds(
            listAds = listOf(native1, native2),
            maxAdCache = 2,
            intervalTime= 3000L,
        )

        Dora.loadInterstitial(
            inter
        )

        Dora.loadNative()

        binding.apply {
            btnInters.setOnClickListener {
                Dora.showInterstitial(
                    activity = this@MainActivity,
                    adUnit = inter,
                    reloadAdUnit = null,
                    timeout = null,
                    callBack = object : ShowInterstitialCallback {
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
                )
            }

            btnBanner.setOnClickListener {
                Dora.loadBanner(
                    activity = this@MainActivity,
                    container = binding.flAd,
                    adSize = AdmobBannerSize.Adaptive,
                    lifecycleOwner = this@MainActivity,
                    adUnitId = banner
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

//            btnNativeFull.setOnClickListener {
//                Dora.showNativeFull(this@MainActivity) {
//
//                }
//            }
        }
    }
}