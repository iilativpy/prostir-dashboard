package com.prostir.dashboard

import android.app.Application
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.prostir.dashboard.data.AdminApi
import com.prostir.dashboard.data.AdminRepository
import com.prostir.dashboard.data.TokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ProstirApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Hand-wired dependencies. Small enough that a DI framework would only add build time. */
class AppContainer(app: Application) {
    val tokens = TokenStore(app)

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the server rejected the token (30 days passed or it was revoked). */
    val sessionExpired: SharedFlow<Unit> = _sessionExpired

    /** House filter shared by "Місяці" and "Заїзди". Null means all houses. Resets on logout. */
    val houseFilter = MutableStateFlow<Int?>(null)

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor())
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    private val api: AdminApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AdminApi::class.java)

    val repository = AdminRepository(api, tokens)

    private fun authInterceptor() = Interceptor { chain ->
        val token = tokens.token
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        val response = chain.proceed(request)
        // 401 on a request that carried a token: it is no longer valid. Login errors (no token) are not session errors.
        if (response.code == 401 && token != null) {
            tokens.clear()
            _sessionExpired.tryEmit(Unit)
        }
        response
    }
}
