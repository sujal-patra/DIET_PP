package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: ResponseSchema? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    val type: String, // "OBJECT" or "ARRAY" or "STRING" etc.
    val properties: Map<String, ResponseProperty>? = null,
    val required: List<String>? = null,
    val items: ResponseSchema? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ResponseProperty(
    val type: String,
    val description: String? = null,
    val items: ResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)


// Concrete models parsed from Gemini's structured response:

@JsonClass(generateAdapter = true)
data class GeminiRecipe(
    val recipeName: String,
    val description: String = "",
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val estimatedCalories: Int = 0,
    val prepTime: String = "",
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class GeminiAnalysis(
    val analysisText: String,
    val suggestions: List<String> = emptyList(),
    val isGoalAchieved: Boolean = false
)
