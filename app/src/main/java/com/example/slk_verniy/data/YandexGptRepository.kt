package com.example.slk_verniy.data

import com.example.slk_verniy.BuildConfig

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class YandexGptRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<Message>
)

@JsonClass(generateAdapter = true)
data class CompletionOptions(
    val stream: Boolean = false,
    val temperature: Double = 0.6,
    val maxTokens: String = "2000"
)

@JsonClass(generateAdapter = true)
data class Message(
    val role: String,
    val text: String
)

@JsonClass(generateAdapter = true)
data class YandexGptResponse(
    val result: GptResult?
)

@JsonClass(generateAdapter = true)
data class GptResult(
    val alternatives: List<Alternative>?,
    val modelVersion: String?
)

@JsonClass(generateAdapter = true)
data class Alternative(
    val message: Message?,
    val status: String?
)

interface YandexGptApi {
    @POST("foundationModels/v1/completion")
    suspend fun getCompletion(
        @Header("Authorization") authorization: String,
        @Header("x-folder-id") folderId: String,
        @Body request: YandexGptRequest
    ): YandexGptResponse
}

object YandexGptRepository {
    private const val BASE_URL = "https://llm.api.cloud.yandex.net/"
    private val API_KEY = BuildConfig.YANDEX_API_KEY
    private val FOLDER_ID = BuildConfig.YANDEX_FOLDER_ID
    private val MODEL_URI = "gpt://$FOLDER_ID/yandexgpt/latest"
    private const val SYSTEM_PROMPT = "Ты умный, дружелюбный и заботливый кот-помощник в приложении графика работы 'Моя Работа'. Помогай пользователю с вопросами о сменах, графике и зарплате. Отвечай тепло, коротко и с кошачьим юмором."

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val api: YandexGptApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(YandexGptApi::class.java)

    suspend fun askGpt(userQuery: String): String {
        return try {
            val request = YandexGptRequest(
                modelUri = MODEL_URI,
                completionOptions = CompletionOptions(),
                messages = listOf(
                    Message(role = "system", text = SYSTEM_PROMPT),
                    Message(role = "user", text = userQuery)
                )
            )
            val response = api.getCompletion(
                authorization = API_KEY,
                folderId = FOLDER_ID,
                request = request
            )
            val text = response.result?.alternatives?.firstOrNull()?.message?.text
            if (!text.isNullOrBlank()) {
                text
            } else {
                "Мур... Не удалось получить ответ от облачного кота. Попробуй еще раз! 🐾"
            }
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 403) {
                "Ошибка 403: Проверьте, назначена ли роль 'ai.languageModels.user' вашему сервисному аккаунту в Yandex Cloud, и активен ли биллинг, мяу! 🐾"
            } else {
                "Мур-р... Ошибка сервера (${e.code()}): ${e.message()}. Проверь настройки, мяу! 🐾"
            }
        } catch (e: java.io.IOException) {
            "Мур-р... Проблема с сетью: ${e.localizedMessage ?: "нет соединения"}. Проверь интернет, мяу! 🐾"
        } catch (e: Exception) {
            "Мур-р... Ошибка связи с облаком: ${e.localizedMessage ?: "неизвестная ошибка"}. Проверь интернет, мяу! 🐾"
        }
    }
}
