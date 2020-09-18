package de.gally.marketstack

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import de.gally.configuration.MarketStackConfiguration
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import java.time.LocalDate

object MarketStackService {

    /** Returns daily total close value of all configured shares */
    suspend fun getTotalSharesValue(config: MarketStackConfiguration): Map<String, Double> {
        return getDetailedShareValues(config)
            .groupBy { it.date }
            .map { (date, allDetailsForDate) ->
                date to allDetailsForDate.sumByDouble { it.close }
            }.toMap()
    }

    /** Get daily close value of all configured Shares */
    suspend fun getDetailedShareValues(config: MarketStackConfiguration): List<DetailedShareResponse> {
        val url = URLBuilder(
            config.baseUrl +
                    "?access_key=${config.apiKey}" +
                    "&symbols=${config.symbols.keys.joinToString(",")}" +
                    "&date_from=${getOldestPurchaseDate(config.symbols.values.map { it.purchaseDate })}"
        ).buildString()

        return config
            .client
            .get<DetailedShareDataResponse> {
                url(url)
                contentType(ContentType.Application.Json)
            }.data
            .filter {
                val purchaseDate = config.symbols[it.symbol]?.purchaseDate ?: LocalDate.now()
                val responseDate = LocalDate.parse(it.date.substringBefore("T"))
                purchaseDate.isBefore(responseDate)
            }
    }

    private fun getOldestPurchaseDate(purchaseDates: List<LocalDate>) = purchaseDates
        .minByOrNull { it }
        ?.toString() ?: LocalDate.now().toString()

    /** Get daily close value of received Share for [symbolKey] */
    suspend fun getDetailedShareValuesFor(
        symbolKey: String,
        config: MarketStackConfiguration,
    ): List<DetailedShareResponse> {
        val url = URLBuilder(
            config.baseUrl +
                    "?access_key=${config.apiKey}" +
                    "&symbols=$symbolKey" +
                    "&date_from=${config.symbols[symbolKey]?.purchaseDate}"
        ).buildString()

        return config
            .client
            .get<DetailedShareDataResponse> {
                url(url)
                contentType(ContentType.Application.Json)
            }.data
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class DetailedShareDataResponse(val data: List<DetailedShareResponse>)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DetailedShareResponse(val close: Double, val symbol: String, val date: String)
}

