# DORA SDK: Thư viện load Ad của Winnix

## Installation
```gradle
// Nhớ đổi sang bản mới nhất ở phần release
implementation 'com.github.winnix-dev:Dora:1.0.0'
```
### Requirements
Lưu ý, cần phải update các SDK đã cũ thì mới dùng được
* **minSdk:** 22
* **compileSdk:** 36
```kotlin
id("com.android.application") version "8.13.1" apply false
id("com.android.library") version "8.13.1" apply false
id("org.jetbrains.kotlin.android") version "2.1.0" apply false
id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
// Nhớ đổi cả trong Build Gradle của app để cùng phiên bản
id ("com.google.dagger.hilt.android") version "2.57.1" apply false

```
Thêm App ID vào Manifest
```xml
<meta-data  
    android:name="com.google.android.gms.ads.APPLICATION_ID"  
    android:value="//ID App"/>
```

## Quick Start
### 1. Initialize SDK
Gọi ở Màn Splash để tránh ANR
```kotlin
//Tạo các AdUnitID trong app
val YandexNative = AdUnit(
    name = "All_Native",
    adType = AdType.Native,
    id = "",
)
val LanguageNative = AdUnit(
    name = "wp1_Language_Native",
    adType = AdType.Native,
    id = "", // Updated
) //...

// Khởi tạo ở màn Splash
Dora.initialize(
    activity = activity,
    adConfig = AdConfig(
        isDebug = BuildConfig.DEBUG,
        nativeTimeInterval = 5000
    )
)

Dora.setUpAdmob(
    intersList = listOf(
        AdResource.MainInterstitial,
        AdResource.SplashInterstitial,
        AdResource.OnboardingInterstitial,
    ),
    nativeList = listOf(
        AdResource.SplashNative,
        AdResource.LanguageNative,
        AdResource.OnboardingNative,
    ),
    openAppId = AdResource.ReturnAppOpenAd,
)

Dora.setUpYandex(
    intersUnit = AdResource.YandexInters,
    nativeUnit = AdResource.YandexNative,
    bannerUnit = AdResource.YandexBanner,
    openAppId = AdResource.YandexOpenApp
)
```
Các ad tạo ở base fragment để gọi cho dễ
### 2. Interstitial
```kotlin
// Tạo thêm 1 biến trong base fragment và 1 object mới để check màn trước màn sau
// Tạo 1 object AdConfig
object AdConfig {
    var isShowInterBefore = false
}
//Trong BaseFragment
var isShowIntern = false
//Tạo 1 hàm showInters để gọi cho nhanh
fun showInters(
    timeout: Long = 6000L,
    onDismiss: () -> Unit
) {
    if(AdConfig.isShowInterBefore) {
        onDismiss()
        return
    }
    (activity as? MainActivity)?.let { activity ->
        Dora.showInterstitial(
            activity = activity,
            timeout = timeout,
            callback = object : ShowInterstitialCallback {
                override fun onDismiss() {
                    onDismiss()
                }

                override fun onShowFailed() {
                    onDismiss()
                }

                override fun onShow() {
                    isShowIntern = true
                }
            }
        )
    }
}
// Override lại onStop
override fun onStop() {
    super.onStop()

    AdConfig.isShowInterBefore = isShowIntern
}

//Gọi trong Các màn
showInters {
    // Hành động sau khi đóng ad
}

// Trong Splash
lifecycleScope.launch {
                val result = Dora.waitForInterstitialAdmobAndYandex(
                    activity,
                    DEFAULT_TIME_OUT
                )

                Log.d(TAG, "initAd: $result")

                if(result) {
                    lifecycleScope.launch {
                        Dora.showInterstitialInNoTime(
                            activity as MainActivity,
                            object : ShowInterstitialCallback {
                                override fun onDismiss() {
                                    // hành động 
                                }

                                override fun onShow() {
                                    isShowIntern = true
                                }
                            }
                        )
                    }
                } else {
                    // hành động 
                }
            }
```
### 3. Native
```kotlin
// Thêm hàm trong base fragment Đã define sẵn 50, 100, 150 , 250, ... 
fun showNative(
        viewGroup: ViewGroup,
        layout: NativeLayout = NativeLayout.Native250
    ) {
        activity?.let { activity ->
            Dora.loadAndShowNative(
                activity,
                lifecycleOwner = viewLifecycleOwner,
                viewGroup = viewGroup,
                layout = layout
            )
        }
    }

//Gọi trong màn, nếu cần sửa layout ad thì sửa
showNative(binding.flAds)
```
### 4. banner
```kotlin
//Base Fragment
fun showBanner(
        container: ViewGroup,
        adSize: AdmobBannerSize,
        adUnitId: AdUnit
    ) {
        activity?.let { activity ->
            Dora.loadBanner(
                activity = activity,
                container = container,
                adSize = adSize,
                lifecycleOwner = viewLifecycleOwner,
                adUnitId = adUnitId
            )
        }
    }

// Dùng trong màn
showBanner(
    container = binding.flAds,
    adSize = AdmobBannerSize.Adaptive,
    adUnitId = AdResource.PreviewBannerCollapsible
)
```
### 5. open app
```kotlin
// GỌi trong Application 
override fun onCreate() {
    super.onCreate()

    Dora.registerOpenAd(this)

}
```
