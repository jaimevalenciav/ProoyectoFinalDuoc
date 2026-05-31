package cl.fleetmanager.shared.api

import io.ktor.client.*
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
        }
    }

    install(HttpTimeout) {
        requestTimeoutMillis  = 15_000
        connectTimeoutMillis  = 10_000
        socketTimeoutMillis   = 15_000
    }
}
