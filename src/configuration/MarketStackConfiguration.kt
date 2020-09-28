@file:Suppress("EXPERIMENTAL_API_USAGE")

package de.gally.configuration

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.Application
import io.ktor.application.ApplicationStopPreparing
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpHeaders
import io.ktor.jackson.jackson
import io.ktor.util.AttributeKey
import java.time.LocalDate

val marketStackAttributes = AttributeKey<MarketStackConfiguration>("marketStackAttributes")

@Suppress("unused")
fun Application.initConfiguration() {
    // Header added to every application response
    install(DefaultHeaders) {
        header("X-Developer", "Arnold Schwarzenegger")
        header(HttpHeaders.Server, "Nothing but a server")
    }
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.INDENT_OUTPUT, true)
            setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
                indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                indentObjectsWith(DefaultIndenter("  ", "\n"))
            })
            registerModule(JavaTimeModule()) // support java.time.* types
        }
    }

    // init client and subscribe on application stop to close the client
    val client = HttpClient(Apache) {
        install(JsonFeature)
    }
    environment.monitor.subscribe(ApplicationStopPreparing) {
        log.warn("Application is about to stop. Client Connections will be closed")
        client.close()
    }

    // Init MarketStackProperties
    val apple = "AAPL"
    val nvidia = "NVDA"

    attributes.put(
        marketStackAttributes,
        MarketStackConfiguration(
            getProperty("marketstack.baseUrl"),
            getProperty("marketstack.apiKey"),
            mapOf(
                apple to getSymbolFor(apple),
                nvidia to getSymbolFor(nvidia)
            ),
            client
        )
    )
}

private fun Application.getSymbolFor(smb: String) = Symbol(
    getProperty("marketstack.symbols.$smb.name"),
    getProperty("marketstack.symbols.$smb.purchaseDate").toLocalDate()
)

data class MarketStackConfiguration(
    val baseUrl: String,
    val apiKey: String,
    val symbols: Map<String, Symbol>,
    val client: HttpClient
)

data class Symbol(
    val name: String,
    val purchaseDate: LocalDate,
)

private fun Application.getProperty(path: String) = environment.config.property(path).getString()

private fun String.toLocalDate() = LocalDate.parse(this)


