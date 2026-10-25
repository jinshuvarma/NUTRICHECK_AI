package com.myapp.nutricheck

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import kotlinx.coroutines.withContext

class FoodAssistantActivity : AppCompatActivity() {
    private lateinit var markwon: Markwon
    private lateinit var etQuestion: EditText
    private lateinit var btnSend: Button
    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private lateinit var aiProgress: ProgressBar

    // Optional product details passed from the product screen.
    private val productName by lazy {
        intent.getStringExtra("product_name").orEmpty()
    }
    private val ingredients by lazy {
        intent.getStringExtra("ingredients").orEmpty()
    }
    private val calories by lazy {
        intent.getStringExtra("calories").orEmpty()
    }
    private val sugar by lazy {
        intent.getStringExtra("sugar").orEmpty()
    }
    private val fat by lazy {
        intent.getStringExtra("fat").orEmpty()
    }
    private val protein by lazy {
        intent.getStringExtra("protein").orEmpty()
    }
    private val allergens by lazy {
        intent.getStringExtra("allergens").orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_assistant)
        markwon = Markwon.builder(this).usePlugin(TablePlugin.create(this)).build()

        etQuestion = findViewById(R.id.etQuestion)
        btnSend = findViewById(R.id.btnSend)
        chatContainer = findViewById(R.id.chatContainer)
        chatScrollView = findViewById(R.id.chatScrollView)
        aiProgress = findViewById(R.id.aiProgress)


        if (productName.isNotBlank()) {
            askAI(
                question = "Analyze this product automatically using the NutriCheck report format.",
                showUserMessage = false
            )
        } else {
            addMessage(
                "NutriCheck AI", "Scan a product first to get its automatic analysis."
            )
        }

        btnSend.setOnClickListener {
            val question = etQuestion.text.toString().trim()

            if (question.isBlank()) {
                etQuestion.error = "Please enter a question"
                return@setOnClickListener
            }

            askAI(question)
        }
    }

    private fun askAI(
        question: String, showUserMessage: Boolean = true
    ) {
        if (showUserMessage) {
            addMessage("You", question)
            etQuestion.text.clear()
        }

        btnSend.isEnabled = false
        aiProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.askFoodAssistant(
                        FoodAssistantRequest(
                            question = question,
                            product_name = productName,
                            ingredients = ingredients,
                            calories = calories,
                            sugar = sugar,
                            fat = fat,
                            protein = protein,
                            allergens = allergens
                        )
                    )
                }

                if (response.success) {
                    addMessage("NutriCheck AI", response.answer)
                } else {
                    addMessage(
                        "NutriCheck AI", "I couldn't generate a response. Please try again."
                    )
                }

            } catch (e: Exception) {
                addMessage(
                    "NutriCheck AI",
                    "Couldn't connect to the AI service. Check your network and try again."
                )
                Toast.makeText(
                    this@FoodAssistantActivity,
                    "AI request failed: ${e.javaClass.simpleName}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnSend.isEnabled = true
                aiProgress.visibility = View.GONE
            }
        }
    }


    private fun addMessage(sender: String, message: String) {
        val textView = TextView(this).apply {
            textSize = 15f
            setTextColor(android.graphics.Color.rgb(30, 41, 59))
            setPadding(16, 12, 16, 12)

            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(
                    if (sender == "You") {
                        android.graphics.Color.rgb(220, 238, 255)
                    } else {
                        android.graphics.Color.WHITE
                    }
                )
                cornerRadius = 20f
            }

            if (sender == "NutriCheck AI") {
                markwon.setMarkdown(this, message)
            } else {
                text = message
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 12
        }

        chatContainer.addView(textView, params)

        chatScrollView.post {
            chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
}
