package com.winnix.dora.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.google.android.gms.ads.nativead.NativeAd
import com.winnix.dora.databinding.DoraLayoutAdsNativeFullBinding
import com.winnix.dora.helper.NativeHelper
import com.winnix.dora.helper.NativeHelper.registerWithLifecycle

class NativeFullDialog private constructor(): DialogFragment() {
    companion object {
        const val TAG = "NativeFullDialog"

        fun newInstance(
            nativeAd: NativeAd,
            onDismiss: () -> Unit
        ) : NativeFullDialog {
            val instance = NativeFullDialog().apply {
                mNativeAd = nativeAd
                this.onDismiss = onDismiss
            }

            return instance
        }
    }

    private var mNativeAd: NativeAd? = null

    private var onDismiss : () -> Unit = {}

    private var _binding: DoraLayoutAdsNativeFullBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Material_NoActionBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DoraLayoutAdsNativeFullBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

         binding.ivClose.setOnClickListener {
             onDismiss()
             dismiss()
         }

        mNativeAd?.let { nativeAd ->
            NativeHelper.inflateView(
                adView = binding.root,
                nativeAd = nativeAd
            )

            nativeAd.registerWithLifecycle(viewLifecycleOwner.lifecycle)
        }

    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}