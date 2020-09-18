package de.gally.marketstack

import de.gally.configuration.marketStackAttributes
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
@Suppress("unused")
fun Application.marketStackApi() {

    val config = attributes[marketStackAttributes]

    routing {
        get("/total") {
            log.info("Fetching total value of all configured shares")
            call.respond(
                HttpStatusCode.OK,
                MarketStackService.getTotalSharesValue(config)
            )
        }

        get("/detail") {
            log.info("Fetching detailed values for all configures shares")
            call.respond(
                HttpStatusCode.OK,
                MarketStackService.getDetailedShareValues(config)
            )
        }

        get("/detail/{sym}") {
            val symbol = call.parameters["sym"].toString()
            log.info("Fetching detailed values for one received share with symbol [$symbol]")
            call.respond(
                HttpStatusCode.OK,
                MarketStackService.getDetailedShareValuesFor(symbol, config)
            )
        }
    }
}
