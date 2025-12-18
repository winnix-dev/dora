# DORA SDK: Thư viện load Ad của Winnix

## Installation

### Requirements
* **minSdk:** 22
* **compileSdk:** 36

## Quick Start
### 1. Initialize SDK
Gọi ở Màn Splash để tránh ANR
```kotlin
// AdmobGuard dùng để check các điều kiện load Ad và Show Ad
val admobGuard = AdmobGuard.apply {
    // Thêm các Rule ở đây
}
// AdConfig dùng để sửa các thông số mặc định của Ad
val config = AdConfig(
    isDebug= true,
    intersTimeout= 6000L,
    maxNativeCache= 2,
    nativeTimeInterval= 3000L,
    admobGuard = null
)
Dora.initialize(activity, adConfig)
```
### 2. AdRule
Đây là nơi định nghĩa các luật cho Ad (Ad có được show hay không)

```kotlin
// Định nghĩa các Rule
class PreConditionRule(
    val condition: (ad: AdmobUnit) -> Boolean
) : AdmobRule {
    override fun checking(ad: AdmobUnit): Boolean {
        return condition(ad)
    }
    override fun onAdShow(ad: AdmobUnit) {
        //Làm gì đó
    }
}

// Sử dụng trong code
val admobGuard = AdmobGuard.apply {
    addRule(
        PreConditionRule()
    )
    addShowRule(
        PreConditionRule2()
    )
}
```
### 3. Interstitial Ad
```kotlin
// Định nghĩa AdUnit
private val inter = AdmobUnit(
    id = "",
    name = "",
    adType = AdmobType.Inters
)
// Load Ad
Dora.loadInterstitial(
    inter
)
// Load Ad với timeout
Dora.showInterstitial(
    activity = this@MainActivity,
    adUnit = inter,
    reloadAdUnit = null,
    timeout = null, // Để Null nếu muốn lấy giá trị mặc định
    callBack = object : ShowInterstitialCallback {
        override fun onDismiss() {
            // Khi Show thành công và người dùng ấn đóng quảng cáo
        }

        override fun onShowFailed() {
            // Khi Show thất bại hoặc ko có ad trả về trong timeout
        }

        override fun onShow() {
            // Khi ad show lên được
        }
    }
)
```
### 4. Native Ad
**Thêm 1 FrameLayout vào View**
```xml
<FrameLayout
   android:id="@+id/flAd"
   android:layout_width="match_parent"
   android:layout_height="wrap_content"
   app:layout_constraintBottom_toBottomOf="parent"
/>
```
**Khởi tạo Native**
```kotlin
Dora.setNativeAds(
     listAds = listOf(native1, native2),
     maxAdCache = 2, // Có thể ko set
     intervalTime= 3000L, // Có thể ko set
)
```
**Gọi trong Fragment**
```kotlin
Dora.loadAndShowNative(
    activity = this@MainActivity,
    lifecycleOwner = viewlifecycleOwner, // Truyền View Lifecycle thay vì chỉ Lifecycle
    viewGroup = binding.flAd,
    // Có sẵn: 50, 150, 250, collapsible, full, full với next btn
    layout = NativeLayout.Native50, 
)
```
### 5. Banner Ad
```kotlin
Dora.loadBanner(
    activity = this@MainActivity,
    container = binding.flAd,
    adSize = AdmobBannerSize.Adaptive,
    lifecycleOwner = this@MainActivity,
    adUnitId = banner
)
```

### 6. Open App
Open App sẽ có 2 kiểu, 1 là Hot Start(hiển thị ở màn plash) hoặc Cold Start(show lúc mở lên)
```kotlin
// Đăng kí 
Dora.registerOpenAd(application, openApp)
// Cold Start
Dora.showOpenAd(
    6000L,
    this@MainActivity,
) {
    Log.d(TAG, "Close Open App")
}
```