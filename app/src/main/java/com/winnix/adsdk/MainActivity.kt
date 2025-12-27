package com.winnix.adsdk

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.winnix.adsdk.databinding.ActivityMainBinding
import com.winnix.dora.Dora
import com.winnix.dora.admob_manager.NativeLayout
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.model.AdConfig
import com.winnix.dora.model.AdType
import com.winnix.dora.model.AdUnit
import com.winnix.dora.model.AdmobBannerSize
import com.winnix.dora.rule.AdmobGuard
import com.winnix.dora.rule.AdmobRule
import com.winnix.dora.yandex_manager.YandexNativeLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "Tagg MainActivity"
    }
    private lateinit var binding: ActivityMainBinding

    private val inter = AdUnit(
        id = "",
        name = "",
        adType = AdType.Inters
    )

    private val native1 = AdUnit(
        id = "",
        name = "",
        adType = AdType.Native
    )

    private val native2 = AdUnit(
        id = "",
        name = "",
        adType = AdType.Native
    )

    private val banner = AdUnit(
        id = "",
        name = "",
        adType = AdType.Banner
    )

    private val openApp = AdUnit(
        id = "",
        name = "",
        adType = AdType.OpenApp
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

        Dora.initialize(
            this,
            adConfig = AdConfig(
                isDebug = BuildConfig.DEBUG
            )
        )

        Dora.setUpAdmob(
            intersList = listOf(inter, inter, inter),
            nativeList = listOf(native1, native2),
            openAppId = openApp,
        )

        Dora.setUpYandex(
            intersUnit = inter,
            nativeUnit = native1,
            bannerUnit = banner,
            openAppId =  openApp
        )
        
        Dora.setAdGuard(
            AdmobGuard.Builder()
                .setNativeFullRule(
                    object : AdmobRule {
                        override fun checking(): () -> Boolean {
                            return { false }
                        }
                    }
                )
                .build()
        )

//        Dora.setNativeAds(
//            listAds = listOf(native1, native2),
//            maxAdCache = 2,
//            intervalTime= 3000L,
//        )
//
//        Dora.setUpInterstitial(
//            adsList = listOf(
//                inter,
//                inter,
//                inter,
//            )
//        )

        Dora.loadNative()

        binding.apply {
            btnInters.setOnClickListener {
//                lifecycleScope.launch {
//                    val result = Dora.waitForInterstitialAdmobAndYandex(
//                        16000L
//                    )
//
//                    Log.d(TAG, "onCreate: $result")
//
//                    if(result) {
//                        Dora.showInterstitialInNoTime(
//                            this@MainActivity,
//                            object : ShowInterstitialCallback {
//                                override fun onDismiss() {
//                                    Log.d(TAG, "onDismiss: SHOW INTERS")
//                                }
//
//                            }
//                        )
//                    } else {
//                        Log.d(TAG, "onCreate: SHow FAILD")
//                    }
//                }

                
                Dora.showInterstitial(
                    activity = this@MainActivity,
                    timeout = null,
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
                    yandexNativeLayout = YandexNativeLayout.Native50
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