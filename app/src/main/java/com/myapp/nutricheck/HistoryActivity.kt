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
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: ProductHistoryDbHelper
    private lateinit var historyContainer: LinearLayout
    private lateinit var clearButton: Button

    private val forest = Color.rgb(23, 63, 53)
    private val green = Color.rgb(40, 122, 88)
    private val muted = Color.rgb(100, 115, 108)
    private val pageBackground = Color.rgb(246, 248, 245)
    private val border = Color.rgb(224, 232, 225)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = ProductHistoryDbHelper(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBackground)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 22, 22, 16)
            setBackgroundColor(forest)
        }

        header.addView(TextView(this).apply {
            text = "Scan History"
            textSize = 27f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })

        header.addView(TextView(this).apply {
            text = "Manage your scanned food products"
            textSize = 14f
            setTextColor(Color.rgb(221, 244, 231))
            setPadding(0, 5, 0, 0)
        })

        clearButton = Button(this).apply {
            text = "Clear All History"
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.rgb(180, 55, 55)
            )

            setOnClickListener {
                confirmClearHistory()
            }
        }

        header.addView(
            clearButton, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 48.dp()
            ).apply {
                topMargin = 14.dp()
            })

        root.addView(header)

        historyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(this).apply {
            clipToPadding = false
            setPadding(18.dp(), 18.dp(), 18.dp(), 18.dp())
            addView(historyContainer)
        }

        root.addView(
            scrollView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        val backButton = Button(this).apply {
            text = "Back to Home"
            isAllCaps = false
            setTextColor(forest)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)

            setOnClickListener {
                finish()
            }
        }

        root.addView(
            backButton, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 52.dp()
            ).apply {
                setMargins(18.dp(), 4.dp(), 18.dp(), 12.dp())
            })

        setContentView(root)
        loadHistory()
    }

    private fun loadHistory() {
        historyContainer.removeAllViews()

        val products = dbHelper.getRecentProducts()
        clearButton.isEnabled = products.isNotEmpty()
        clearButton.alpha = if (products.isEmpty()) 0.5f else 1f

        if (products.isEmpty()) {
            historyContainer.addView(TextView(this).apply {
                text = "No scan history yet.\n\nScanned products will appear here."
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(muted)
                setPadding(20.dp(), 50.dp(), 20.dp(), 50.dp())
                background = cardBackground()
            })
            return
        }

        products.forEach { product ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16.dp(), 16.dp(), 16.dp(), 14.dp())
                background = cardBackground()
                elevation = 2.dp().toFloat()
            }

            val name = TextView(this).apply {
                text = product.product_name?.takeIf { it.isNotBlank() } ?: "Unknown product"
                textSize = 17f
                setTypeface(null, Typeface.BOLD)
                setTextColor(forest)
            }

            val brand = TextView(this).apply {
                text = product.brand?.takeIf { it.isNotBlank() } ?: "Brand not available"
                textSize = 14f
                setTextColor(muted)
                setPadding(0, 5.dp(), 0, 0)
            }

            val nutrition = TextView(this).apply {
                val calories = product.calories?.let {
                    formatNumber(it)
                } ?: "N/A"

                val sugar = product.sugar?.let {
                    formatNumber(it)
                } ?: "N/A"

                text = "$calories kcal  •  Sugar: $sugar g"
                textSize = 13f
                setTextColor(green)
                setPadding(0, 8.dp(), 0, 0)
            }

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val detailsButton = Button(this).apply {
                text = "View Details"
                isAllCaps = false
                setTextColor(forest)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    Color.rgb(221, 244, 231)
                )

                setOnClickListener {
                    openProductDetails(product)
                }
            }

            val deleteButton = Button(this).apply {
                text = "Remove"
                isAllCaps = false
                setTextColor(Color.rgb(180, 55, 55))
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    Color.rgb(255, 235, 235)
                )

                setOnClickListener {
                    product.barcode?.let { barcode ->
                        confirmDeleteProduct(barcode)
                    } ?: run {
                        android.widget.Toast.makeText(
                            this@HistoryActivity,
                            "Cannot remove product: barcode is missing",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            actions.addView(
                detailsButton, LinearLayout.LayoutParams(
                    0, 46.dp(), 1f
                ).apply {
                    marginEnd = 8.dp()
                })

            actions.addView(
                deleteButton, LinearLayout.LayoutParams(
                    0, 46.dp(), 1f
                )
            )

            card.addView(name)
            card.addView(brand)
            card.addView(nutrition)
            card.addView(actions)

            historyContainer.addView(
                card, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 14.dp()
                })
        }
    }

    private fun confirmDeleteProduct(barcode: String) {
        AlertDialog.Builder(this).setTitle("Remove product?").setMessage(
                "This product will be removed from your scan history."
            ).setNegativeButton("Cancel", null).setPositiveButton("Remove") { _, _ ->
                dbHelper.deleteProduct(barcode)
                loadHistory()
            }.show()
    }

    private fun confirmClearHistory() {
        AlertDialog.Builder(this).setTitle("Clear all history?").setMessage(
                "All saved scan records will be deleted. This cannot be undone."
            ).setNegativeButton("Cancel", null).setPositiveButton("Clear All") { _, _ ->
                dbHelper.clearHistory()
                loadHistory()
            }.show()
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

    private fun cardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = 22.dp().toFloat()
            setStroke(1.dp(), border)
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::dbHelper.isInitialized) {
            dbHelper.close()
        }
    }
}
