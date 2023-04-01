package com.citydata

import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extract
import it.skrape.fetcher.skrape
import it.skrape.selects.Doc
import it.skrape.selects.ElementNotFoundException
import it.skrape.selects.html5.*

// test
fun main(){
    val cityData = CityDataDao()
    skrape(HttpFetcher) {
        request {
//            url = "https://www.city-data.com/city/Abbeville-Alabama.html"
            url = "https://www.city-data.com/city/Corvallis-Oregon.html"
        }

        extract {
            htmlDocument {
//                extractNaturalDisasters(cityData)
                extractPercentLivingInPoverty(cityData)
            }
        }
    }
}


fun Doc.extractNaturalDisasters(cityDataDao: CityDataDao){
    section { withClass="natural-disasters"
//        "The number of natural .+ (\\(\\d+\\)) is .+ (\\(\\d+\\))".toRegex().find(text)
        findFirst {
            print(text)
        }
    }

}


fun Doc.extractMedianGrossRent(cityDataDao: CityDataDao) {
    section {
        withClass = "median-rent"
        try {
            findFirst {
                logger.info { text }
                val groups = "Median gross rent in (\\d{4}): \\\$([0-9,]+).".toRegex().find(text)
                if(groups != null && groups.groupValues.size >= 3) {
                    cityDataDao.medianGrossRentYearDataAcquired = groups.groupValues[1].toInt()
                    cityDataDao.medianGrossRent = groups.groupValues[2].replace(",", "").toLong()
                }
            }
        } catch (e : ElementNotFoundException) {
            logger.error(t=e) {"median rent not defined on this page"}
        }
    }
}


fun Doc.extractMaleFemalePopulationCount(cityDataDao: CityDataDao) {
    section {
        withClass = "population-by-sex"
        findFirst {
            tr {
                findFirst {
                    td {
                        findFirst {
                            cityDataDao.malePopulation = text.replace("Males: ", "").replace(",", "").toLong()
                        }
                    }
                }
                findSecond {
                    td {
                        findFirst {
                            cityDataDao.femalePopulation = text.replace("Females: ", "").replace(",", "").toLong()
                        }
                    }
                }
            }
        }
    }
}


fun Doc.extractUnemployment(cityDataDao: CityDataDao) {
    section {
        withClass = "unemployment"
        //*[@id="unemployment"]/div/table/tbody/tr[1]/td[2]/text()
        findFirst {
            tr {
                findAll {
                    forEach {
                        var title = ""
                        var percent = 0.0f
                        it.td {
                            findFirst {
                                title = text
                            }
                            findSecond {
                                percent = text.replace("%", "").toFloat()
                            }
                        }
                        cityDataDao.unemploymentDemographics.add(DemographicDao(title, percent, null))
                    }
                }
            }
        }
    }
}


fun Doc.extractEducationInfo(cityDataDao: CityDataDao) {
    section {
        withClass = "education-info"
        ul {
            li {
                findAll {
                    forEachIndexed { index, docElement ->
                        if (index == this.size - 1) return@findAll
                        val (status, percentage) = docElement.text.split(": ")
                        cityDataDao.educationDemographic.add(
                            DemographicDao(
                                status,
                                percentage.replace("%", "").toFloat()
                            )
                        )
                    }
                }
            }
        }
    }
}

fun Doc.extractMaritalStatus(cityDataDao: CityDataDao) {
    section {
        withClass = "marital-info"

        ul {
            li {
                findAll {
                    forEach {
                        val (status, percentage) = it.text.split(": ")
                        cityDataDao.maritalStatus.add(DemographicDao(status, percentage.replace("%", "").toFloat()))
                    }
                }
            }
        }
    }
}

fun Doc.extractCityPopulation(cityDataDao: CityDataDao) {
    section {
        withClass = "city-population"
        findFirst {
            // we match month of year [A-Za-z ] if it's in there then we match year and population number
            val groups = "Population in[A-Za-z ]+? ?(\\d+): ([0-9,]+)".toRegex().find(text)
            if (groups != null) {
                cityDataDao.apply {
                    populationTotal = groups.groupValues[2].replace(",", "").toLong()
                    populationInYear = groups.groupValues[1].toInt()
                }
            }
        }
    }
}

fun Doc.extractMedianAge(cityDataDao: CityDataDao) {
    section {
        withClass = "median-age"
        tr {
            findFirst {
                val groups = "Median resident age: ([0-9.]+) years".toRegex().find(text)
                if (groups != null) cityDataDao.medianAge = groups.groupValues[1].toFloatOrNull()

            }
            findSecond {
                val groups = " median age: ([0-9.]+) years".toRegex().find(text)
                if (groups != null) cityDataDao.medianAgeForState = groups.groupValues[1].toFloatOrNull()

            }
        }
    }
}

fun Doc.extractCityZipCodes(cityDataDao: CityDataDao) {
    section {
        withClass = "zip-codes"
        try {
            a {
                findAll {
                    this.forEach {
                        cityDataDao.zipCodes.add(it.text)
                    }
                }
            }
        } catch (e : ElementNotFoundException) {
            // todo add backup logic here. Some zips don't have links, so we should handle that here.
                logger.error { "zip code link not defined on this page" }
                logger.error { e.stackTrace }
        }
    }
}

fun Doc.extractIncomeDemographics(cityDataDao: CityDataDao) {
    section {
        withClass = "median-income"
        findFirst {
            cityDataDao.apply {
                medianHouseholdIncome = parseEconValues(text, "Estimated median household income")
                medianIncomePerCapita = parseEconValues(text, "Estimated per capita income")
                medianHouseCondoValue = parseEconValues(text, "Estimated median house or condo value")
            }
        }
    }
}

fun Doc.extractPercentLivingInPoverty(cityDataDao: CityDataDao) {
    section {
        withId = "poverty-level"
        findFirst {
            cityDataDao.apply {
                val groups = "Percentage of residents living in poverty in (\\d{4}): (\\d+\\.\\d+)\\%".toRegex().find(text)
                populationLivingInPovertyYearOfData = groups?.groupValues?.get(1)?.toInt()
                percentageOfPopulationLivingInPoverty = groups?.groupValues?.get(2)?.toFloat()
            }
        }
    }
}


fun Doc.extractCostOfLivingIndex(cityDataDao: CityDataDao) {
    section {
        withClass = "cost-of-living-index"
        findFirst {
            val groups = "(\\d{4}) cost of living index in .+: ([0-9.]+)".toRegex().find(text)
            cityDataDao.apply {
                costOfLivingIndex = groups?.groupValues?.get(2)?.toFloat()
                costOfLivingIndexYearDataAcquired = groups?.groupValues?.get(1)?.toInt()
            }
        }
    }
}

fun Doc.extractPopulationDensity(cityDataDao: CityDataDao) {
    section {
        withClass = "population-density"
        findFirst {
            cityDataDao.apply {
                val groups = "Land area: ([0-9.]+) (\\S+).".toRegex().find(text)
                if (groups != null && groups.groupValues.size >= 3) {
                    landAreaOfCity = MeasurementDao(groups.groupValues[1].toFloat(), groups.groupValues[2])
                }
            }

            cityDataDao.apply {
                val groups = "Population density: (\\d+) people per (\\S+)".toRegex().find(text)
                if (groups != null && groups.groupValues.size >= 3) {
                    populationDensity = MeasurementDao(groups.groupValues[1].toFloat(), groups.groupValues[2])
                }
            }
        }
    }
}

fun Doc.extractCityElevation(cityDataDao: CityDataDao) = section {
    withClass = "elevation"

    // our script should not fail due to a minor datapoint like this not being available.
    try {
        findFirst {
            val groups = "Elevation: (\\d+) (\\S+)".toRegex().find(text)
            cityDataDao.apply {
                if (groups != null && groups.groupValues.size >= 3) {
                    elevation = MeasurementDao(groups.groupValues[1].toFloat(), groups.groupValues[2])
                }
            }
        }
    } catch (e : ElementNotFoundException) {
        logger.error(t=e) {"elevation data point not available for ${cityDataDao.cityName}, ${cityDataDao.state}"}
    }
}

fun Doc.extractRaceDemographics(cityDataDao: CityDataDao) {
    section {
        withClass = "races-graph"
        ul {
            withClass = "list-group"
            findSecond {
                li {
                    findAll {
                        forEach {
                            var title = ""
                            var numberOfPeople = 0L
                            var percentageOfPopulation = 0.0f
                            it.b {
                                findFirst {
                                    title = text
                                }
                                it.span {
                                    withClass = "alert-info"
                                    findFirst {
                                        percentageOfPopulation = text.replace("%", "").toFloat()
                                    }
                                }
                                it.span {
                                    withClass = "badge"
                                    findFirst {
                                        numberOfPeople = text.replace(",", "").toLong()
                                    }
                                }
                            }
                            cityDataDao.raceDemographic.add(
                                DemographicDao(
                                    title,
                                    percentageOfPopulation,
                                    numberOfPeople
                                )
                            )

                        }
                    }
                }
            }

        }
    }
}
