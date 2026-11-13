package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.Category
import com.example.data.model.ItemCondition
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiSuggestion(
    val title: String,
    val category: Category,
    val condition: ItemCondition,
    val description: String
)

/**
 * Aide à la publication : propose un titre, une catégorie, un état et une description à partir de la photo.
 * Si l'IA est indisponible (hors ligne, quota), l'écran continue de fonctionner : la saisie reste manuelle.
 */
class AiRepository {

    companion object {
        private const val TAG = "AiRepository"
        /** À adapter si le modèle change dans votre projet Firebase. */
        const val MODEL_NAME = "gemini-2.5-flash"
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig { responseMimeType = "application/json" }
        )
    }

    suspend fun suggest(bitmap: Bitmap?, hint: String): AiSuggestion? = withContext(Dispatchers.IO) {
        try {
            val categories = Category.entries.filter { it != Category.TOUT }.joinToString(", ") { it.name }
            val conditions = ItemCondition.entries.joinToString(", ") { it.name }
            val prompt = """
                Tu aides des voisins au Cameroun à publier un objet à donner ou à échanger.
                Indice saisi par l'utilisateur : "$hint".
                Réponds uniquement avec un objet JSON ayant exactement ces clés :
                - "title" : nom court de l'objet en français (60 caractères max)
                - "category" : une valeur parmi [$categories]
                - "condition" : une valeur parmi [$conditions]
                - "description" : une description honnête en 1 à 2 phrases (sans inventer de détails invisibles)
            """.trimIndent()

            val response = withTimeout(30_000L) {
                if (bitmap != null) {
                    model.generateContent(content {
                        image(bitmap)
                        text(prompt)
                    })
                } else {
                    model.generateContent(prompt)
                }
            }
            val raw = response.text?.trim().orEmpty()
            if (raw.isBlank()) return@withContext null
            val json = JSONObject(raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
            AiSuggestion(
                title = json.optString("title", hint).take(80),
                category = Category.entries.firstOrNull { it.name.equals(json.optString("category"), true) && it != Category.TOUT }
                    ?: Category.AUTRE,
                condition = ItemCondition.entries.firstOrNull { it.name.equals(json.optString("condition"), true) }
                    ?: ItemCondition.BON_ETAT,
                description = json.optString("description", "")
            )
        } catch (e: Exception) {
            Log.w(TAG, "Suggestion IA indisponible : ${e.message}")
            null
        }
    }
}
