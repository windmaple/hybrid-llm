package com.google.mediapipe.examples.llminference

// NB: Make sure the filename is *unique* per model you use!
// Weight caching is currently based on filename alone.
enum class Model(
    val path: String,
    val url: String,
    val needsAuth: Boolean,
    val uiState: UiState,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
) {
    GEMMA_CPU(
        path = "/data/local/tmp/llm/gemma2-2b-it-cpu-int8.task",
        url = "",
        needsAuth = true,
        GemmaUiState(),
        temperature = 0f, //0.8f,
        topK = 40,
        topP = 1.0f
    ),
    GEMMA_GPU(
        path = "/data/local/tmp/llm/gemma-2b-it-gpu-int4.bin",
        url = "",
        needsAuth = true,
        GemmaUiState(),
        temperature = 0.8f,
        topK = 40,
        topP = 1.0f
    ),
    DEEPSEEK_CPU(
        path = "/data/local/tmp/llm/deepseek3k_q8_ekv1280.task",
        url = "",
        needsAuth = false,
        DeepSeeUiState(),
        temperature = 0.6f,
        topK = 40,
        topP = 0.7f
    ),
}
