package com.myapp.nutricheck

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class IngredientsAnalysisActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingredients_analysis)

        val name = intent.getStringExtra("product_name")
        val ingredients = intent.getStringExtra("ingredients")
        val allergens = intent.getStringExtra("allergens")

        findViewById<TextView>(R.id.tvAnalysisProductName).text =
            name ?: "Unknown product"

        findViewById<TextView>(R.id.tvAnalysisIngredients).text =
            ingredients?.takeIf { it.isNotBlank() }
                ?: "Ingredient information unavailable"

        findViewById<TextView>(R.id.tvDeclaredAllergens).text =
            allergens?.takeIf { it.isNotBlank() }
                ?: "Declared allergen information unavailable"

        val selected = AllergyPreferences.getSelected(this)

        val result = AllergenChecker.check(
            selectedAllergens = selected,
            ingredients = ingredients,
            declaredAllergens = allergens
        )

        val message = when {
            selected.isEmpty() ->
                "Select your allergens in My Allergy Preferences to check this product."

            result.matched.isNotEmpty() ->
                "⚠️ Potential allergen match: " +
                        result.matched.joinToString(", ") +
                        "\nPlease verify the package label."

            !result.canCheck ->
                "Cannot verify: product ingredient information is missing."

            else ->
                "No selected allergen match detected in available information. " +
                        "This does not guarantee allergy safety."
        }

        findViewById<TextView>(R.id.tvPersonalAllergyResult).text = message
    }
}
