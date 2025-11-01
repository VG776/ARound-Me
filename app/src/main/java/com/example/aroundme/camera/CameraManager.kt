package com.example.aroundme.camera

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraManager(
    private val context: Context
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        imageAnalyzer: ImageAnalysis.Analyzer
    ): Camera = suspendCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Image analysis use case
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, imageAnalyzer)
                }

            this.imageAnalyzer = imageAnalysis

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                continuation.resume(camera!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        cameraExecutor.shutdown()
    }
}