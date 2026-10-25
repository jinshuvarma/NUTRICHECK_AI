package com.myapp.nutricheck

data class FoodAssistantRequest(
    val question: String,
    val product_name: String = "",
    val ingredients: String = "",
    val calories: String = "",
    val sugar: String = "",
    val fat: String = "",
    val protein: String = "",
    val allergens: String = ""
)

data class FoodAssistantResponse(
    val success: Boolean,
    val answer: String
)
