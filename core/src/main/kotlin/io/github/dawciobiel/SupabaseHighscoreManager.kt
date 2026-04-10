package io.github.dawciobiel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.badlogic.gdx.net.HttpRequestBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RemoteScoreEntry(
    val player_name: String,
    val score: Int
)

@Serializable
data class RemoteScoreResponse(
    val player_name: String,
    val score: Int,
    val created_at: String
)

class SupabaseHighscoreManager(
    private val url: String = "https://utyvqcxggjtvylxfzxbv.supabase.co",
    private val apiKey: String = "sb_publishable_MR5aQ_BDEtXgddDVCg9MdA_2JfkLDID" // todo Extract those keys to config file
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val rb = HttpRequestBuilder()

    fun saveScore(name: String, score: Int, callback: (Boolean) -> Unit = {}) {
        val entry = RemoteScoreEntry(name, score)
        val jsonBody = json.encodeToString(entry)

        val request = rb.newRequest()
            .method(Net.HttpMethods.POST)
            .url("$url/rest/v1/high_scores")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Prefer", "return=minimal")
            .content(jsonBody)
            .build()

        Gdx.net.sendHttpRequest(request, object : Net.HttpResponseListener {
            override fun handleHttpResponse(httpResponse: Net.HttpResponse) {
                val status = httpResponse.status.statusCode
                if (status in 200..299) {
                    Gdx.app.log("Supabase", "Score saved successfully!")
                    callback(true)
                } else {
                    Gdx.app.error("Supabase", "Failed to save score. Status: $status")
                    callback(false)
                }
            }

            override fun failed(t: Throwable) {
                Gdx.app.error("Supabase", "Error saving score: ${t.message}")
                callback(false)
            }

            override fun cancelled() {
                callback(false)
            }
        })
    }

    fun fetchTopScores(limit: Int = 10, callback: (List<RemoteScoreResponse>) -> Unit) {
        val request = rb.newRequest()
            .method(Net.HttpMethods.GET)
            .url("$url/rest/v1/high_scores?select=player_name,score,created_at&order=score.desc&limit=$limit")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $apiKey")
            .build()

        Gdx.net.sendHttpRequest(request, object : Net.HttpResponseListener {
            override fun handleHttpResponse(httpResponse: Net.HttpResponse) {
                val status = httpResponse.status.statusCode
                if (status in 200..299) {
                    try {
                        val responseText = httpResponse.resultAsString
                        val scores = json.decodeFromString<List<RemoteScoreResponse>>(responseText)
                        callback(scores)
                    } catch (e: Exception) {
                        Gdx.app.error("Supabase", "Error parsing scores: ${e.message}")
                        callback(emptyList())
                    }
                } else {
                    Gdx.app.error("Supabase", "Failed to fetch scores. Status: $status")
                    callback(emptyList())
                }
            }

            override fun failed(t: Throwable) {
                Gdx.app.error("Supabase", "Error fetching scores: ${t.message}")
                callback(emptyList())
            }

            override fun cancelled() {
                callback(emptyList())
            }
        })
    }
}
