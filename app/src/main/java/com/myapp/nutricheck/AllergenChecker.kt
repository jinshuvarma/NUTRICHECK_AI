package com.myapp.nutricheck

object AllergenChecker {

    private val allergenKeywords = mapOf(
        "Milk" to listOf("milk", "dairy", "casein", "whey", "lactose"),
        "Peanuts" to listOf("peanut", "groundnut", "arachis"),
        "Tree nuts" to listOf(
            "almond", "cashew", "walnut", "pistachio",
            "hazelnut", "pecan", "macadamia", "brazil nut"
        ),
        "Soy" to listOf("soy", "soya", "soybean"),
        "Wheat" to listOf("wheat", "wheat flour", "semolina", "durum"),
        "Eggs" to listOf("egg", "eggs", "albumin", "ovalbumin"),
        "Fish" to listOf("fish", "anchovy", "tuna", "salmon", "cod"),
        "Crustaceans" to listOf(
            "crustacean", "shrimp", "prawn", "crab", "lobster"
        ),
        "Sesame" to listOf("sesame", "tahini"),
        "Mustard" to listOf("mustard")
    )

    data class Result(
        val matched: List<String>,
        val canCheck: Boolean
    )

    fun check(
        selectedAllergens: Set<String>,
        ingredients: String?,
        declaredAllergens: String?
    ): Result {
        val availableText = listOfNotNull(
            ingredients?.takeIf { it.isNotBlank() },
            declaredAllergens?.takeIf { it.isNotBlank() }
        ).joinToString(" ").lowercase()

        if (selectedAllergens.isEmpty()) {
            return Result(emptyList(), availableText.isNotBlank())
        }

        if (availableText.isBlank()) {
            return Result(emptyList(), false)
        }

        val matches = selectedAllergens.filter { selected ->
            val keywords = allergenKeywords[selected].orEmpty()

            keywords.any { keyword ->
                Regex(
                    """(?<![a-z])${Regex.escape(keyword)}(?![a-z])"""
                ).containsMatchIn(availableText)
            }
        }

        return Result(matches, true)
    }
}
