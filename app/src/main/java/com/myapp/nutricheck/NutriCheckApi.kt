
package com.myapp.nutricheck

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Body

interface NutriCheckApi {

    @GET("api/products/{barcode}")
    fun getProduct(
        @Path("barcode") barcode: String
    ): Call<ProductResponse>

    @POST("api/ai/food-assistant")
    suspend fun askFoodAssistant(
        @Body request: FoodAssistantRequest
    ): FoodAssistantResponse
}
