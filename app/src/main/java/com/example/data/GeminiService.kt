package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val recipeJsonAdapter = moshi.adapter(GeminiRecipe::class.java)
    val analysisJsonAdapter = moshi.adapter(GeminiAnalysis::class.java)
}

class GeminiRepository {
    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    suspend fun generateRecipe(pantryItems: List<PantryItem>, preference: String): GeminiRecipe? {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return GeminiRecipe(
                recipeName = "Please Configure Gemini API Key",
                description = "To generate personalized smart recipes using AI, please configure your actual GEMINI_API_KEY in the Secrets tab of AI Studio.",
                ingredients = listOf("Pantry Items: " + pantryItems.joinToString { "${it.name} (${it.quantity} ${it.unit})" }),
                instructions = listOf("1. Go to AI Studio Sidebar", "2. Open Secrets Panel", "3. Add GEMINI_API_KEY", "4. Restart the App to load visual insights!"),
                estimatedCalories = 0,
                prepTime = "N/A"
            )
        }

        val ingredientsList = pantryItems.joinToString("\n") { "- ${it.name}: ${it.quantity} ${it.unit} (${it.category})" }
        val prompt = if (pantryItems.isEmpty()) {
            "Suggest a creative entry-level recipe you can make with basic pantry ingredients. Dietary style: $preference."
        } else {
            "Generate an original recipe maximizing the use of these available pantry items:\n$ingredientsList\n\nDietary preference: $preference. You can include common pantry elements like salt, pepper, oil, water, etc. but prioritize the specified items."
        }

        val recipeSchema = ResponseSchema(
            type = "OBJECT",
            properties = mapOf(
                "recipeName" to ResponseProperty(type = "STRING", description = "The name of the recipe."),
                "description" to ResponseProperty(type = "STRING", description = "Brief enticing description."),
                "prepTime" to ResponseProperty(type = "STRING", description = "Preparation time (e.g., '25 mins')."),
                "estimatedCalories" to ResponseProperty(type = "INTEGER", description = "Total kcal value of the meal."),
                "protein" to ResponseProperty(type = "NUMBER", description = "Protein content in grams."),
                "carbs" to ResponseProperty(type = "NUMBER", description = "Carbohydrate content in grams."),
                "fat" to ResponseProperty(type = "NUMBER", description = "Fat content in grams."),
                "ingredients" to ResponseProperty(
                    type = "ARRAY",
                    description = "List of ingredients with exact measurement/amounts.",
                    items = ResponseSchema(type = "STRING")
                ),
                "instructions" to ResponseProperty(
                    type = "ARRAY",
                    description = "Step-by-step instructions to prepare this dish.",
                    items = ResponseSchema(type = "STRING")
                )
            ),
            required = listOf("recipeName", "description", "prepTime", "estimatedCalories", "ingredients", "instructions")
        )

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are a professional Michelin Star chef. Suggest creative recipes based on user ingredients. Return JSON mapping the provided schema. Do not include extra text beyond JSON structure."))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = recipeSchema,
                temperature = 0.2f
            )
        )

        return try {
            val response = GeminiClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                GeminiClient.recipeJsonAdapter.fromJson(jsonText)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun analyzeDietAndMeals(mealLogs: List<DietLog>, goal: UserGoal): GeminiAnalysis? {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return GeminiAnalysis(
                analysisText = "Gemini API key is not configured. Please enter your GEMINI_API_KEY in the AI Studio secrets panel to authorize professional dietary coaching and custom logs tracking.",
                suggestions = listOf("Add GEMINI_API_KEY to AI Studio Secrets", "Restart App to unlock premium meal coaches"),
                isGoalAchieved = false
            )
        }

        val mealsSummary = mealLogs.joinToString("\n") { "- [${it.mealType}] ${it.foodName}: ${it.calories} kcal (P: ${it.protein}g, C: ${it.carbs}g, F: ${it.fat}g)" }
        val prompt = """
            Analyze my diet structure for today:
            Current goals:
            - Daily Calories Limit: ${goal.dailyCalorieGoal} kcal
            - Daily Protein Goal: ${goal.dailyProteinGoal}g
            - Daily Carbs Goal: ${goal.dailyCarbsGoal}g
            - Daily Fat Goal: ${goal.dailyFatGoal}g
            - Dietary Preference: ${goal.dietPreference}
            
            Meals consumed:
            $mealsSummary
            
            Evaluate if we hit the diet parameters. Give suggestions for next meals or corrections.
        """.trimIndent()

        val analysisSchema = ResponseSchema(
            type = "OBJECT",
            properties = mapOf(
                "analysisText" to ResponseProperty(type = "STRING", description = "An analytical summary review of today's macronutrients and nutrition balance. Friendly and professional tone."),
                "suggestions" to ResponseProperty(
                    type = "ARRAY",
                    description = "Bullet-point dietary tips or meal corrections based on the data.",
                    items = ResponseSchema(type = "STRING")
                ),
                "isGoalAchieved" to ResponseProperty(type = "BOOLEAN", description = "True if total calories consumed are within +/- 15% range of the target, and protein is near or exceeding goal.")
            ),
            required = listOf("analysisText", "suggestions", "isGoalAchieved")
        )

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an elite, certified health and dietitian coach. Analyze meal intake and suggest small lifestyle tweaks. Ensure response strictly corresponds to JSON design criteria."))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = analysisSchema,
                temperature = 0.2f
            )
        )

        return try {
            val response = GeminiClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                GeminiClient.analysisJsonAdapter.fromJson(jsonText)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
