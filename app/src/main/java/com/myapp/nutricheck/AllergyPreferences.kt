
package com.myapp.nutricheck

import android.content.Context

object AllergyPreferences {

    private const val PREFS = "nutricheck_preferences"
    private const val KEY_ALLERGENS = "selected_allergens"
    private const val KEY_CUSTOM = "custom_food_preferences"

    val availableAllergens = listOf(
        "Milk",
        "Peanuts",
        "Tree nuts",
        "Soy",
        "Wheat",
        "Eggs",
        "Fish",
        "Crustaceans",
        "Sesame",
        "Mustard"
    )

    fun getSelected(context: Context): Set<String> {
        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).getStringSet(KEY_ALLERGENS, emptySet())
            ?.toSet() ?: emptySet()
    }

    fun saveSelected(context: Context, selected: Set<String>) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .putStringSet(KEY_ALLERGENS, selected.toSet())
            .apply()
    }

    fun getCustomPreferences(context: Context): List<String> {
        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).getStringSet(KEY_CUSTOM, emptySet())
            ?.toList()
            ?.sortedBy { it.lowercase() }
            ?: emptyList()
    }

    fun addCustomPreference(context: Context, name: String): Boolean {
        val cleanedName = name.trim()

        if (cleanedName.isBlank() || cleanedName.length > 50) {
            return false
        }

        if (availableAllergens.any {
                it.equals(cleanedName, ignoreCase = true)
            }
        ) {
            return false
        }

        val current = getCustomPreferences(context)

        if (current.any {
                it.equals(cleanedName, ignoreCase = true)
            }
        ) {
            return false
        }

        val updated = current.toMutableSet()
        updated.add(cleanedName)

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .putStringSet(KEY_CUSTOM, updated)
            .apply()

        return true
    }

    fun removeCustomPreference(context: Context, name: String) {
        val updated = getCustomPreferences(context)
            .filterNot { it.equals(name, ignoreCase = true) }
            .toSet()

        val preferences = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        val selected = getSelected(context)
            .filterNot { it.equals(name, ignoreCase = true) }
            .toSet()

        preferences.edit()
            .putStringSet(KEY_CUSTOM, updated)
            .putStringSet(KEY_ALLERGENS, selected)
            .apply()
    }
}
