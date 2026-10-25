package com.myapp.nutricheck

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AllergyActivity : AppCompatActivity() {

    private val forest = Color.rgb(23, 63, 53)
    private val green = Color.rgb(40, 122, 88)
    private val backgroundColor = Color.rgb(246, 248, 245)
    private val muted = Color.rgb(100, 115, 108)
    private val border = Color.rgb(224, 232, 225)

    private lateinit var preferencesContainer: LinearLayout
    private lateinit var customContainer: LinearLayout
    private lateinit var customInput: EditText

    private val checkboxes = mutableMapOf<String, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 20)
            setBackgroundColor(forest)
        }

        header.addView(TextView(this).apply {
            text = "Food Preferences"
            textSize = 27f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })

        header.addView(TextView(this).apply {
            text = "Personalize your food checks"
            textSize = 14f
            setTextColor(Color.rgb(221, 244, 231))
            setPadding(0, 6, 0, 0)
        })

        root.addView(header)

        val scrollView = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
            setPadding(18, 18, 18, 18)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(content)

        content.addView(sectionTitle("Common allergens"))
        content.addView(
            sectionSubtitle(
                "Select ingredients you want to avoid."
            )
        )

        preferencesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(16, 10, 16, 10)
        }

        content.addView(preferencesContainer, marginParams(top = 12))

        content.addView(
            sectionTitle("Your custom preferences"),
            marginParams(top = 22)
        )

        content.addView(
            sectionSubtitle(
                "Add any other ingredient or food preference."
            )
        )

        val addCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(16, 16, 16, 16)
        }

        customInput = EditText(this).apply {
            hint = "e.g. Palm Oil"
            textSize = 15f
            setTextColor(forest)
            setHintTextColor(muted)
            setPadding(14, 12, 14, 12)
            background = roundedBackground(Color.WHITE, border)
        }

        addCard.addView(
            customInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val addButton = Button(this).apply {
            text = "+  Add Preference"
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(green)

            setOnClickListener {
                val name = customInput.text.toString().trim()

                if (name.isBlank()) {
                    customInput.error = "Enter a preference"
                    return@setOnClickListener
                }

                if (name.length > 50) {
                    customInput.error = "Maximum 50 characters"
                    return@setOnClickListener
                }

                val added = AllergyPreferences.addCustomPreference(
                    this@AllergyActivity,
                    name
                )

                if (added) {
                    customInput.text.clear()
                    renderPreferences()
                    Toast.makeText(
                        this@AllergyActivity,
                        "Preference added",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@AllergyActivity,
                        "Already exists or invalid preference",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        addCard.addView(addButton, marginParams(top = 10))
        content.addView(addCard, marginParams(top = 12))

        customContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(14, 8, 14, 8)
        }

        content.addView(
            customContainer,
            marginParams(top = 12)
        )

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val saveButton = Button(this).apply {
            text = "Save Preferences"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(forest)

            setOnClickListener {
                val selected = checkboxes
                    .filterValues { it.isChecked }
                    .keys
                    .toSet()

                AllergyPreferences.saveSelected(
                    this@AllergyActivity,
                    selected
                )

                Toast.makeText(
                    this@AllergyActivity,
                    "Preferences saved successfully",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 8, 18, 14)
            setBackgroundColor(Color.WHITE)
            elevation = 8f
            addView(
                saveButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    54.dp()
                )
            )
        }

        root.addView(footer)
        setContentView(root)

        renderPreferences()
    }

    private fun renderPreferences() {
        checkboxes.clear()
        preferencesContainer.removeAllViews()
        customContainer.removeAllViews()

        val selected = AllergyPreferences.getSelected(this)

        AllergyPreferences.availableAllergens.forEach { allergen ->
            val checkbox = createCheckbox(allergen, selected.contains(allergen))
            checkboxes[allergen] = checkbox
            preferencesContainer.addView(checkbox)
        }

        val customItems = AllergyPreferences.getCustomPreferences(this)

        if (customItems.isEmpty()) {
            customContainer.addView(TextView(this).apply {
                text = "No custom preferences added yet."
                textSize = 14f
                setTextColor(muted)
                setPadding(4, 14, 4, 14)
            })
        }

        customItems.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val checkbox = createCheckbox(item, selected.contains(item))
            checkboxes[item] = checkbox

            row.addView(
                checkbox,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )

            val removeButton = TextView(this).apply {
                text = "Remove"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.rgb(180, 55, 55))
                gravity = Gravity.CENTER
                setPadding(10, 10, 4, 10)

                setOnClickListener {
                    androidx.appcompat.app.AlertDialog.Builder(
                        this@AllergyActivity
                    )
                        .setTitle("Remove preference?")
                        .setMessage("Remove \"$item\" from your custom list?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ ->
                            AllergyPreferences.removeCustomPreference(
                                this@AllergyActivity,
                                item
                            )
                            renderPreferences()
                        }
                        .show()
                }
            }

            row.addView(removeButton)
            customContainer.addView(row)
        }
    }

    private fun createCheckbox(
        label: String,
        checked: Boolean
    ): CheckBox {
        return CheckBox(this).apply {
            text = label
            textSize = 15f
            setTextColor(forest)
            isChecked = checked
            buttonTintList =
                android.content.res.ColorStateList.valueOf(green)
            setPadding(2, 5, 2, 5)
        }
    }

    private fun sectionTitle(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(forest)
        }
    }

    private fun sectionSubtitle(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 13f
            setTextColor(muted)
            setPadding(0, 5, 0, 0)
        }
    }

    private fun cardBackground(): GradientDrawable {
        return roundedBackground(Color.WHITE, border)
    }

    private fun roundedBackground(
        fillColor: Int,
        strokeColor: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = 20.dp().toFloat()
            setStroke(1.dp(), strokeColor)
        }
    }

    private fun marginParams(top: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = top.dp()
        }
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
