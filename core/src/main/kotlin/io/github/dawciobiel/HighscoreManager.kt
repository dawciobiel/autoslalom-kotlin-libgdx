package io.github.dawciobiel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import java.security.MessageDigest

data class ScoreEntry(
    var name: String = "Player",
    var score: Int = 0,
    var timestamp: Long = System.currentTimeMillis()
)

data class HighscoreData(
    var scores: MutableList<ScoreEntry> = mutableListOf(),
    var checksum: String = ""
)

class HighscoreManager(private val filename: String = "data/highscores.json") {
    private val json = Json().apply {
        setOutputType(JsonWriter.OutputType.json)
    }
    private val salt = "AUTOSLALOM_SECRET_SALT_2026"
    private val file: FileHandle = Gdx.files.local(filename)

    fun loadScores(): List<ScoreEntry> {
        if (!file.exists()) return emptyList()

        return try {
            val content = file.readString()
            val data = json.fromJson(HighscoreData::class.java, content)
            
            if (verifyChecksum(data)) {
                data.scores.sortedByDescending { it.score }
            } else {
                Gdx.app.error("HighscoreManager", "Sumy kontrolne nie zgadzają się! Plik mógł zostać zmodyfikowany. Kasowanie...")
                file.delete()
                emptyList()
            }
        } catch (e: Exception) {
            Gdx.app.error("HighscoreManager", "Błąd podczas wczytywania wyników: ${e.message}")
            emptyList()
        }
    }

    fun saveScore(name: String, score: Int) {
        val currentScores = loadScores().toMutableList()
        currentScores.add(ScoreEntry(name, score))
        
        val topScores = currentScores
            .sortedByDescending { it.score }
            .take(10)
            .toMutableList()

        val data = HighscoreData(scores = topScores)
        data.checksum = calculateChecksum(data.scores)

        try {
            file.writeString(json.prettyPrint(data), false)
        } catch (e: Exception) {
            Gdx.app.error("HighscoreManager", "Błąd podczas zapisywania wyników: ${e.message}")
        }
    }

    private fun calculateChecksum(scores: List<ScoreEntry>): String {
        val rawData = scores.joinToString("|") { "${it.name}:${it.score}:${it.timestamp}" } + salt
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawData.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun verifyChecksum(data: HighscoreData): Boolean {
        if (data.scores.isEmpty()) return true
        return data.checksum == calculateChecksum(data.scores)
    }
}
