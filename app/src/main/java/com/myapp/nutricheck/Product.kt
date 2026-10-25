package com.myapp.nutricheck

data class Product(
    val id: Int?,
    val barcode: String?,
    val product_name: String?,
    val brand: String?,
    val ingredients: String?,
    val calories: Double?,
    val sugar: Double?,
    val fat: Double?,
    val protein: Double?,
    val allergens: String?,
    val image_url: String?
)