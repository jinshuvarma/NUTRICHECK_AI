package com.myapp.nutricheck

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ProductHistoryDbHelper(context: Context) :
    SQLiteOpenHelper(context, "nutricheck_history.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE scan_history (
                barcode TEXT PRIMARY KEY,
                product_name TEXT,
                brand TEXT,
                ingredients TEXT,
                calories REAL,
                sugar REAL,
                fat REAL,
                protein REAL,
                allergens TEXT,
                image_url TEXT,
                scanned_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase, oldVersion: Int, newVersion: Int
    ) {
        // Preserve existing history when schema versions change.
    }

    fun saveProduct(barcode: String, product: Product) {
        val values = ContentValues().apply {
            put("barcode", barcode)
            put("product_name", product.product_name)
            put("brand", product.brand)
            put("ingredients", product.ingredients)
            put("calories", product.calories)
            put("sugar", product.sugar)
            put("fat", product.fat)
            put("protein", product.protein)
            put("allergens", product.allergens)
            put("image_url", product.image_url)
            put("scanned_at", System.currentTimeMillis())
        }

        writableDatabase.insertWithOnConflict(
            "scan_history", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getRecentProducts(): List<Product> {
        val products = mutableListOf<Product>()

        readableDatabase.rawQuery(
            "SELECT * FROM scan_history ORDER BY scanned_at DESC", null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                products.add(
                    Product(
                        id = null,
                        barcode = cursor.getString(
                            cursor.getColumnIndexOrThrow("barcode")
                        ),
                        product_name = cursor.getString(
                            cursor.getColumnIndexOrThrow("product_name")
                        ),
                        brand = cursor.getString(
                            cursor.getColumnIndexOrThrow("brand")
                        ),
                        ingredients = cursor.getString(
                            cursor.getColumnIndexOrThrow("ingredients")
                        ),
                        calories = getNullableDouble(cursor, "calories"),
                        sugar = getNullableDouble(cursor, "sugar"),
                        fat = getNullableDouble(cursor, "fat"),
                        protein = getNullableDouble(cursor, "protein"),
                        allergens = cursor.getString(
                            cursor.getColumnIndexOrThrow("allergens")
                        ),
                        image_url = cursor.getString(
                            cursor.getColumnIndexOrThrow("image_url")
                        )
                    )
                )
            }
        }


        return products
    }

    fun deleteProduct(barcode: String): Int {
        return writableDatabase.delete(
            "scan_history", "barcode = ?", arrayOf(barcode)
        )
    }

    fun clearHistory(): Int {
        return writableDatabase.delete("scan_history", null, null)
    }


    private fun getNullableDouble(
        cursor: android.database.Cursor, column: String
    ): Double? {
        val index = cursor.getColumnIndexOrThrow(column)
        return if (cursor.isNull(index)) null else cursor.getDouble(index)
    }
}
