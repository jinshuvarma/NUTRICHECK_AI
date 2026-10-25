package com.myapp.nutricheck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView

    private val cameraExecutor =
        Executors.newSingleThreadExecutor()

    private val barcodeScanner =
        BarcodeScanning.getClient()

    // Prevent multiple scans at the same time
    private var isProcessingBarcode = false

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val SCAN_COOLDOWN = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.scanner_activity)

        previewView = findViewById(R.id.previewView)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    // =====================================================
    // SEND BARCODE TO FASTAPI
    // =====================================================

    private fun getProductFromBackend(barcode: String) {

        RetrofitClient.api
            .getProduct(barcode)
            .enqueue(object : Callback<ProductResponse> {

                override fun onResponse(
                    call: Call<ProductResponse>,
                    response: Response<ProductResponse>
                ) {

                    if (response.isSuccessful) {

                        val product = response.body()?.product

                        if (product != null) {

                            ProductHistoryDbHelper(this@ScannerActivity)
                                .saveProduct(barcode, product)
                            val intent = Intent(
                                this@ScannerActivity,
                                ProductActivity::class.java
                            )

                            intent.putExtra(
                                "product_name",
                                product.product_name
                            )

                            intent.putExtra(
                                "brand",
                                product.brand
                            )

                            intent.putExtra(
                                "ingredients",
                                product.ingredients
                            )

                            intent.putExtra("barcode", barcode)
                            intent.putExtra("calories", product.calories?.toString())
                            intent.putExtra("sugar", product.sugar?.toString())
                            intent.putExtra("fat", product.fat?.toString())
                            intent.putExtra("protein", product.protein?.toString())
                            intent.putExtra("image_url", product.image_url)

                            intent.putExtra("allergens", product.allergens)

                            Toast.makeText(
                                this@ScannerActivity,
                                "Opening Product",
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(intent)

                        } else {

                            Toast.makeText(
                                this@ScannerActivity,
                                "Product data unavailable",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } else {

                        Toast.makeText(
                            this@ScannerActivity,
                            "Product not found",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    resetScannerAfterDelay()
                }

                override fun onFailure(
                    call: Call<ProductResponse>,
                    t: Throwable
                ) {

                    Toast.makeText(
                        this@ScannerActivity,
                        "Connection failed: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    resetScannerAfterDelay()
                }
            })
    }

    // =====================================================
    // RESET SCANNER
    // =====================================================

    private fun resetScannerAfterDelay() {

        previewView.postDelayed({

            isProcessingBarcode = false

        }, SCAN_COOLDOWN)
    }

    // =====================================================
    // START CAMERA
    // =====================================================

    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            // Camera preview
            val preview =
                Preview.Builder()
                    .build()

            preview.surfaceProvider =
                previewView.surfaceProvider

            // Image analysis
            val imageAnalysis =
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

            imageAnalysis.setAnalyzer(
                cameraExecutor
            ) { imageProxy ->

                analyzeImage(imageProxy)
            }

            val cameraSelector =
                CameraSelector.DEFAULT_BACK_CAMERA

            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

            } catch (e: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Camera failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // =====================================================
    // ANALYZE CAMERA IMAGE
    // =====================================================

    private fun analyzeImage(
        imageProxy: ImageProxy
    ) {

        if (isProcessingBarcode) {
            imageProxy.close()
            return
        }

        val mediaImage =
            imageProxy.image

        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image =
            InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

        barcodeScanner
            .process(image)

            .addOnSuccessListener { barcodes ->

                if (isProcessingBarcode) {
                    return@addOnSuccessListener
                }

                for (barcode in barcodes) {

                    val value =
                        barcode.rawValue
                            ?: continue

                    val boundingBox =
                        barcode.boundingBox
                            ?: continue

                    val centerX =
                        boundingBox.centerX()

                    val centerY =
                        boundingBox.centerY()

                    // Camera image dimensions
                    val imageWidth =
                        image.width

                    val imageHeight =
                        image.height

                    // Scanning box
                    val boxWidth =
                        imageWidth * 0.70f

                    val boxHeight =
                        imageHeight * 0.35f

                    val boxLeft =
                        (imageWidth - boxWidth) / 2

                    val boxRight =
                        boxLeft + boxWidth

                    val boxTop =
                        (imageHeight - boxHeight) / 2

                    val boxBottom =
                        boxTop + boxHeight

                    // Check barcode inside box
                    val isInsideBox =
                        centerX >= boxLeft &&
                                centerX <= boxRight &&
                                centerY >= boxTop &&
                                centerY <= boxBottom

                    if (!isInsideBox) {
                        continue
                    }

                    isProcessingBarcode = true

                    Toast.makeText(
                        this@ScannerActivity,
                        "Sending: $value",
                        Toast.LENGTH_LONG
                    ).show()

                    getProductFromBackend(value)

                    break
                }
            }

            .addOnFailureListener { exception ->

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Barcode error: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            .addOnCompleteListener {

                imageProxy.close()
            }
    }

    // =====================================================
    // CAMERA PERMISSION
    // =====================================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            CAMERA_PERMISSION_REQUEST
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                startCamera()

            } else {

                Toast.makeText(
                    this,
                    "Camera permission is required",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }
    }

    // =====================================================
    // CLEANUP
    // =====================================================

    override fun onDestroy() {

        barcodeScanner.close()

        cameraExecutor.shutdown()

        super.onDestroy()
    }
}