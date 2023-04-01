package com.citydata

import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extract
import it.skrape.fetcher.skrape
import it.skrape.selects.Doc
import it.skrape.selects.ElementNotFoundException
import it.skrape.selects.attribute
import it.skrape.selects.html5.*
import java.util.*

fun main() {

    skrape(HttpFetcher) {
        request {
            url = "https://www.walkscore.com/OR/Corvallis"
        }

        extract {
            htmlDocument {
                extractWalkScore()
            }
        }
    }
}

/**
 * Both input strings should be lowercase just to make sure this works right
 * @param stateCode String
 * @param cityName String
 * @return String
 */
fun buildWalkScoreLink(stateCode: String, cityName: String): String {
    val capitalizedStateCode = stateCode.uppercase(Locale.getDefault())
    val capitalizedCityName = cityName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    return "https://www.walkscore.com/$capitalizedStateCode/$capitalizedCityName"
}

data class WalkScoreInfo(val walkScore: Int? = null, val bikeScore: Int? = null, val transitScore: Int? = null)

fun Doc.extractWalkScore(): WalkScoreInfo {
    val walkScoreRegex = Regex("(\\d+) Walk Score of")
    val bikeScoreRegex = Regex("(\\d+) Bike Score of")
    val transitScoreRegex = Regex("(\\d+) Transit Score of")


    var walkScore: Int? = null
    var transitScore: Int? = null
    var bikeScore: Int? = null

    div {
        withClass = "score-info-link"
        img {
            findAll {
                forEachIndexed { idx , element ->
                    when (idx) {
                        0 -> {
                            walkScore = element.attributes["alt"]?.let { walkScoreRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
                        }
                        1 -> {
                            transitScore = element.attributes["alt"]?.let { transitScoreRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
                        }
                        2 -> {
                            bikeScore = element.attributes["alt"]?.let { bikeScoreRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
                        }
                    }
                }
            }
        }
    }

    return WalkScoreInfo(walkScore, bikeScore, transitScore)
}



