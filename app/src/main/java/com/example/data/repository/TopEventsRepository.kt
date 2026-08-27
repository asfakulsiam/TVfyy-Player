package com.example.data.repository

import android.content.Context
import com.example.domain.model.EventServer
import com.example.domain.model.TopEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class TopEventsRepository(private val context: Context) {

    companion object {
        const val TOP_EVENTS_GITHUB_URL = "https://raw.githubusercontent.com/asfakulsiam/TVfyy-Player/main/top-events.xml"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _topEvents = MutableStateFlow<List<TopEvent>>(emptyList())
    val topEvents: StateFlow<List<TopEvent>> = _topEvents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun loadTopEvents(): List<TopEvent> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        var events = fetchFromRemote()
        if (events.isEmpty()) {
            events = loadFromLocalAsset()
        }
        _topEvents.value = events
        _isLoading.value = false
        events
    }

    private fun fetchFromRemote(): List<TopEvent> {
        return try {
            val request = Request.Builder()
                .url(TOP_EVENTS_GITHUB_URL)
                .header("User-Agent", "TVfyyPlayer/1.1 (Android)")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    parseEventsJson(body)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadFromLocalAsset(): List<TopEvent> {
        return try {
            val inputStream = context.assets.open("top-events.xml")
            val content = inputStream.bufferedReader().use { it.readText() }
            parseEventsJson(content)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseEventsJson(jsonString: String): List<TopEvent> {
        val list = mutableListOf<TopEvent>()
        try {
            val jsonArray = JSONArray(jsonString.trim())
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "event-$i")
                val title = obj.optString("title", "Featured Event")
                val description = obj.optString("description", "")
                val image = obj.optString("image", "")

                val servers = mutableListOf<EventServer>()
                if (obj.has("server")) {
                    val serverArr = obj.getJSONArray("server")
                    for (j in 0 until serverArr.length()) {
                        val sObj = serverArr.getJSONObject(j)
                        // Handle server_namd or server_name typos seamlessly
                        val name = if (sObj.has("server_namd")) {
                            sObj.getString("server_namd")
                        } else if (sObj.has("server_name")) {
                            sObj.getString("server_name")
                        } else {
                            "Server ${j + 1}"
                        }
                        val url = sObj.optString("server_url", "")
                        if (url.isNotBlank()) {
                            servers.add(EventServer(name = name, url = url))
                        }
                    }
                }

                list.add(
                    TopEvent(
                        id = id,
                        title = title,
                        description = description,
                        image = image,
                        servers = servers
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
