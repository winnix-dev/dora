package com.winnix.dora.admob_manager

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.winnix.dora.R

internal object NativeHelper {
    fun getNativeAdView(
        context: Context,
        nativeAd: NativeAd,
        layout: View
    ) : NativeAdView {
        val adView = NativeAdView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        adView.addView(layout)

        inflateView(adView, nativeAd)

        return adView

    }

    fun inflateView(
        adView: NativeAdView,
        nativeAd: NativeAd
    ) {
        val viewGroup = adView.findViewById<ViewGroup>(R.id.ad_media)
        if (viewGroup != null) {
            val mediaView = MediaView(adView.context)
            viewGroup.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            viewGroup.addView(
                mediaView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            adView.mediaView = mediaView
        }

        val viewGroupIcon = adView.findViewById<View>(R.id.ad_app_icon)
        if (viewGroupIcon != null) {
            if (viewGroupIcon is ViewGroup) {
                val nativeAdIcon = ImageView(adView.context)
                viewGroupIcon.addView(
                    nativeAdIcon,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                adView.iconView = nativeAdIcon
            } else {
                adView.iconView = viewGroupIcon
            }

        }

        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
//        adView.priceView = adView.findViewById(R.id.ad_price)
//        adView.starRatingView = adView.findViewById(R.id.ad_stars)
//        adView.storeView = adView.findViewById(R.id.ad_store)
//        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        (adView.headlineView as TextView).text = nativeAd.headline
        if (adView.mediaView != null) {
            adView.mediaView?.mediaContent = nativeAd.mediaContent
        }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }
        if (adView.callToActionView != null) {
            if (adView.callToActionView != null) {
                if (nativeAd.callToAction == null) {
                    adView.callToActionView?.visibility = View.INVISIBLE
                } else {
                    adView.callToActionView?.visibility = View.VISIBLE
                    if (adView.callToActionView is Button) {
                        (adView.callToActionView as Button).text = nativeAd.callToAction
                    } else {
                        (adView.callToActionView as TextView).text = nativeAd.callToAction
                    }
                }
            }
        }
        if (adView.iconView != null) {
            if (nativeAd.icon == null) {
                adView.iconView?.visibility = View.GONE
            } else {
                (adView.iconView as ImageView).setImageDrawable(
                    nativeAd.icon?.drawable
                )
                adView.iconView?.visibility = View.VISIBLE
            }
        }
        if (adView.priceView != null) {
            if (nativeAd.price == null) {
                adView.priceView?.visibility = View.INVISIBLE
            } else {
                adView.priceView?.visibility = View.VISIBLE
                (adView.priceView as TextView).text = nativeAd.price
            }
        }
        if (adView.storeView != null) {
            if (nativeAd.store == null) {
                adView.storeView?.visibility = View.INVISIBLE
            } else {
                adView.storeView?.visibility = View.VISIBLE
                (adView.storeView as TextView).text = nativeAd.store
            }
        }
        if (adView.starRatingView != null) {
            if (nativeAd.starRating == null) {
                adView.starRatingView?.visibility = View.INVISIBLE
            } else {
                (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
                adView.starRatingView?.visibility = View.VISIBLE
            }
        }
        if (adView.advertiserView != null) {
            if (nativeAd.advertiser == null) {
                adView.advertiserView?.visibility = View.INVISIBLE
            } else {
                (adView.advertiserView as TextView).text = nativeAd.advertiser
                adView.advertiserView?.visibility = View.VISIBLE
            }
        }

        adView.findViewById<ImageView>(R.id.ivDrop)?.let { view ->
            view.setOnClickListener {
                viewGroup.visibility = View.GONE
                it.visibility = View.GONE
            }
        }
    }

    fun NativeAd.registerWithLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                this@registerWithLifecycle.destroy()
                lifecycle.removeObserver(this)
            }
        })
    }

}