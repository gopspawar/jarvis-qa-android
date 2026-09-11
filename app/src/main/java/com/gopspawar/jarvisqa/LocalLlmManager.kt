package com.gopspawar.jarvisqa

import android.content.Context
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

sealed interface ModelState {
    data object NotDownloaded : ModelState
    data class Downloading(val percent: Int) : ModelState
    data object Loading : ModelState
    data object Ready : ModelState
    data class Error(val message: String) : ModelState
}

class LocalLlmManager(context: Context) {
    companion object {
        const val MODEL_NAME = "Qwen3-1.7B-Q4_K_M.gguf"
        const val MODEL_URL =
            "https://huggingface.co/Qwen/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf?download=true"
    }

    private val appContext = context.applicationContext
    private val modelDir = File(appContext.getExternalFilesDir(null), "models").apply { mkdirs() }
    private val modelFile = File(modelDir, MODEL_NAME)
    private val partialFile = File(modelDir, "$MODEL_NAME.part")
    private val client = OkHttpClient.Builder().build()
    private var modelHandle: LlamaModel? = null
    private val _state = MutableStateFlow<ModelState>(
        if (modelFile.exists() && modelFile.length() > 500_000_000L) ModelState.Loading
        else ModelState.NotDownloaded
    )
    val state: StateFlow<ModelState> = _state.asStateFlow()

    suspend fun initializeIfPresent() {
        if (modelFile.exists() && modelFile.length() > 500_000_000L) load()
    }

    suspend fun downloadAndLoad() = withContext(Dispatchers.IO) {
        try {
            _state.value = ModelState.Downloading(0)
            val existing = if (partialFile.exists()) partialFile.length() else 0L
            val request = Request.Builder().url(MODEL_URL).apply {
                if (existing > 0) header("Range", "bytes=$existing-")
            }.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) error("Download failed: HTTP ${response.code}")
                val body = response.body ?: error("Empty model download")
                val total = when {
                    response.code == 206 -> existing + body.contentLength()
                    body.contentLength() > 0 -> body.contentLength()
                    else -> -1L
                }
                FileOutputStream(partialFile, response.code == 206 && existing > 0).use { output ->
                    val buffer = ByteArray(1024 * 256)
                    var downloaded = if (response.code == 206) existing else 0L
                    body.byteStream().use { input ->
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            downloaded += count
                            if (total > 0) _state.value = ModelState.Downloading((downloaded * 100 / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
            }
            if (modelFile.exists()) modelFile.delete()
            if (!partialFile.renameTo(modelFile)) error("Could not finalize model file")
            load()
        } catch (e: Exception) {
            _state.value = ModelState.Error(e.message ?: "Model setup failed")
        }
    }

    private suspend fun load() = withContext(Dispatchers.IO) {
        try {
            _state.value = ModelState.Loading
            modelHandle?.let { Llama.releaseModel(it) }
            modelHandle = Llama.loadModel(
                modelPath = modelFile.absolutePath,
                config = LlamaConfig(contextSize = 2048, threads = 6)
            )
            _state.value = ModelState.Ready
        } catch (e: Exception) {
            _state.value = ModelState.Error("Model load failed: ${e.message}")
        }
    }

    suspend fun complete(prompt: String, mode: AgentMode): String = withContext(Dispatchers.IO) {
        val handle = modelHandle ?: error("Download and load the local model first")
        val system = if (mode == AgentMode.QA) {
            "You are Jarvis, Gopal's concise senior QA assistant. Help with manual testing, Java, Selenium, Playwright, API testing, SQL, test design and defect analysis. Give practical, accurate answers."
        } else {
            "You are Jarvis, Gopal's friendly personal assistant. Be concise, helpful and natural. The user is in India. Never claim you performed a phone action that you cannot perform."
        }
        Llama.complete(handle, prompt = prompt, systemPrompt = system, maxTokens = 320).text.trim()
    }

    fun close() {
        modelHandle?.let { Llama.releaseModel(it) }
        modelHandle = null
    }
}
