package com.myapp.nutricheck

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var historyContainer: LinearLayout
    private lateinit var historyDb: ProductHistoryDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        historyDb = ProductHistoryDbHelper(this)
        historyContainer = findViewById(R.id.historyContainer)

        findViewById<Button>(R.id.btnScanProduct).setOnClickListener {
            openScanner()
        }

        findViewById<Button>(R.id.btnAllergyPreferences).setOnClickListener {
            openAllergies()
        }

        findViewById<View>(R.id.navHome).setOnClickListener {
            // Already on Home.
        }

        findViewById<View>(R.id.navScan).setOnClickListener {
            openScanner()
        }

        findViewById<View>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        findViewById<View>(R.id.navAI).setOnClickListener {
            startActivity(Intent(this, FoodAssistantActivity::class.java))
        }

        findViewById<TextView>(R.id.tvViewAll).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun openScanner() {
        startActivity(Intent(this, ScannerActivity::class.java))
    }

    private fun openAllergies() {
        startActivity(Intent(this, AllergyActivity::class.java))
    }

    override fun onResume() {
        super.onResume()

        if (::historyContainer.isInitialized && ::historyDb.isInitialized) {
            showRecentProducts()
        }
    }

    private fun showRecentProducts() {
        historyContainer.removeAllViews()

        // Show only the latest 3 products on Home.
        val products = historyDb.getRecentProducts().take(3)

        if (products.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No scans yet. Scan your first product!"
                textSize = 14f
                setTextColor(Color.rgb(100, 115, 108))
                gravity = Gravity.CENTER
                setPadding(16, 24, 16, 24)
            }

            historyContainer.addView(empty)
            return
        }

        products.forEach { product ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 16, 18, 16)

                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 24f
                    setStroke(1, Color.rgb(224, 232, 225))
                }

                elevation = 2f
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    openProductDetails(product)
                }
            }

            val name = TextView(this).apply {
                text = product.product_name?.takeIf { it.isNotBlank() } ?: "Unknown product"

                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.rgb(23, 63, 53))
            }

            val brand = TextView(this).apply {
                text = product.brand?.takeIf { it.isNotBlank() } ?: "Brand not available"

                textSize = 13f
                setTextColor(Color.rgb(100, 115, 108))
                setPadding(0, 5, 0, 0)
            }

            val nutrition = TextView(this).apply {
                val calories = product.calories?.let { formatNumber(it) } ?: "N/A"
                val sugar = product.sugar?.let { formatNumber(it) } ?: "N/A"

                text = "$calories kcal  •  Sugar: $sugar g"
                textSize = 13f
                setTextColor(Color.rgb(40, 122, 88))
                setPadding(0, 8, 0, 0)
            }

            card.addView(name)
            card.addView(brand)
            card.addView(nutrition)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }

            historyContainer.addView(card, params)
        }
    }

    private fun openProductDetails(product: Product) {
        val intent = Intent(this, ProductActivity::class.java).apply {
            putExtra("barcode", product.barcode)
            putExtra("product_name", product.product_name)
            putExtra("brand", product.brand)
            putExtra("ingredients", product.ingredients)
            putExtra("calories", product.calories?.toString())
            putExtra("sugar", product.sugar?.toString())
            putExtra("fat", product.fat?.toString())
            putExtra("protein", product.protein?.toString())
            putExtra("allergens", product.allergens)
            putExtra("image_url", product.image_url)
        }

        startActivity(intent)
    }

    private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::historyDb.isInitialized) {
            historyDb.close()
        }
    }
}
