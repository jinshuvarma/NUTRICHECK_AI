package com.myapp.nutricheck

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import coil.load

class ProductActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        val btnAskAI = findViewById<android.widget.Button>(R.id.btnAskAI)

        btnAskAI.setOnClickListener {
            val intent = android.content.Intent(
                this, FoodAssistantActivity::class.java
            ).apply {
                putExtra(
                    "product_name", getIntent().getStringExtra("product_name").orEmpty()
                )
                putExtra(
                    "ingredients", getIntent().getStringExtra("ingredients").orEmpty()
                )
                putExtra(
                    "calories", getIntent().getStringExtra("calories").orEmpty()
                )
                putExtra(
                    "sugar", getIntent().getStringExtra("sugar").orEmpty()
                )
                putExtra(
                    "fat", getIntent().getStringExtra("fat").orEmpty()
                )
                putExtra(
                    "protein", getIntent().getStringExtra("protein").orEmpty()
                )
                putExtra(
                    "allergens", getIntent().getStringExtra("allergens").orEmpty()
                )
            }

            startActivity(intent)
        }


        val productName = intent.getStringExtra("product_name")
        val brand = intent.getStringExtra("brand")
        val ingredients = intent.getStringExtra("ingredients")
        val calories = intent.getStringExtra("calories")
        val sugar = intent.getStringExtra("sugar")
        val fat = intent.getStringExtra("fat")
        val protein = intent.getStringExtra("protein")
        val allergens = intent.getStringExtra("allergens")

        findViewById<TextView>(R.id.tvProductName).text = productName ?: "Unknown Product"

        findViewById<TextView>(R.id.tvBrand).text = brand ?: "Brand not available"

        findViewById<TextView>(R.id.tvCalories).text =
            "Calories: ${calories ?: "Not available"} kcal"

        findViewById<TextView>(R.id.tvSugar).text = "Sugar: ${sugar ?: "Not available"} g"

        findViewById<TextView>(R.id.tvFat).text = "Fat: ${fat ?: "Not available"} g"

        findViewById<TextView>(R.id.tvProtein).text = "Protein: ${protein ?: "Not available"} g"

        findViewById<TextView>(R.id.tvIngredients).text =
            ingredients?.takeIf { it.isNotBlank() } ?: "Ingredients not available"

        findViewById<TextView>(R.id.tvAllergens).text =
            allergens?.takeIf { it.isNotBlank() } ?: "No allergen information available"

        findViewById<TextView>(R.id.tvIngredients).setOnClickListener {
            openIngredientsAnalysis()
        }

        findViewById<TextView>(R.id.tvAllergens).setOnClickListener {
            openIngredientsAnalysis()
        }
        val productImage = findViewById<ImageView>(R.id.ivProductImage)

        val imageUrl = intent.getStringExtra("image_url")

        if (!imageUrl.isNullOrBlank()) {
            productImage.load(imageUrl) {
                crossfade(true)
                placeholder(android.R.color.white)
                error(android.R.color.white)
            }
        } else {
            productImage.setImageDrawable(null)
        }

        val selectedAllergens = AllergyPreferences.getSelected(this)

        val result = AllergenChecker.check(
            selectedAllergens = selectedAllergens,
            ingredients = ingredients,
            declaredAllergens = allergens
        )

        val allergyMessage = when {
            selectedAllergens.isEmpty() -> "Select your allergies in My Allergy Preferences to check this product."

            result.matched.isNotEmpty() -> "⚠️ POTENTIAL ALLERGEN MATCH\n" + result.matched.joinToString(
                ", "
            ) + "\nCheck the product label before consuming."

            !result.canCheck -> "⚠️ Cannot verify: ingredient and allergen information is unavailable."

            else -> "No selected allergen match detected in the available information. " + "This does not guarantee the product is allergy-safe."
        }

        findViewById<TextView>(R.id.tvAllergens).text = allergyMessage

    }

    private fun openIngredientsAnalysis() {
        val intent = Intent(this, IngredientsAnalysisActivity::class.java).apply {
            putExtra("product_name", intent.getStringExtra("product_name"))
            putExtra("ingredients", intent.getStringExtra("ingredients"))
            putExtra("allergens", intent.getStringExtra("allergens"))
        }
        startActivity(intent)
    }
}
