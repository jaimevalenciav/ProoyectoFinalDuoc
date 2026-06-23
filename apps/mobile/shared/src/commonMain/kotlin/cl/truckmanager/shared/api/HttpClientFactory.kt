package cl.truckmanager.shared.api

import io.ktor.client.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createHttpClient(
    engine: HttpClientEngine,
    tokenProvider: () -> String?,
): HttpClient = HttpClient(engine) {

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }

    install(Logging) {
        level = LogLevel.HEADERS
        logger = Logger.DEFAULT
    }

    install(Auth) {
        bearer {
            loadTokens { BearerTokens(tokenProvider() ?: "", "") }
            refreshTokens { BearerTokens(tokenProvider() ?: "", "") }
            sendWithoutRequest { true }   // enviar Bearer en TODAS las requests sin esperar challenge
        }
    }

    install(HttpTimeout) {
        requestTimeoutMillis  = 60_000   // cold start ACA scale-to-zero puede tardar ~30-45s
        connectTimeoutMillis  = 30_000
        socketTimeoutMillis   = 60_000
    }
}
