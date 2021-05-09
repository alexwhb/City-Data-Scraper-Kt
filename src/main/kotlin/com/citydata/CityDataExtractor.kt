package com.citydata

import com.citydata.db.saveToDb
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extract
import it.skrape.fetcher.skrape
import kotlinx.coroutines.delay
import java.lang.Exception

// currently this just extracts data for one city, but it will easily be hooked up to our link extractor and
// extract data for all cities. I do need to write some failsafe method so that if there's not a particular datapoint
// we don't hard crash and just leave that data null.
suspend fun extractCityData(cityState: CityStateDao) {
    val cityData = CityDataDao()

    cityData.cityName = cityState.cityName
    cityData.state = cityState.state
    cityData.url = cityState.cityUrl

    skrape(HttpFetcher) {
        request {
            url = cityState.cityUrl
        }

        extract {
            htmlDocument {
                try {
                    extractCityPopulation(cityData)
                    extractMaleFemalePopulationCount(cityData)
                    extractMedianAge(cityData)
                    extractCityZipCodes(cityData)
                    extractIncomeDemographics(cityData)
                    extractMedianGrossRent(cityData)
                    extractCostOfLivingIndex(cityData)
                    extractRaceDemographics(cityData)
                    extractPopulationDensity(cityData)
                    extractCityElevation(cityData)
                    extractMaritalStatus(cityData)
                    extractEducationInfo(cityData)
                    extractUnemployment(cityData)
                } catch (e : Exception) {
                    logger.error(e) {"Skipping ${cityState.cityName} because of exception" }
                }
            }
        }
    }
    cityData.saveToDb()
    delay(5000)
}