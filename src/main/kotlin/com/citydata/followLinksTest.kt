package com.citydata

import com.citydata.db.*
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extract
import it.skrape.fetcher.skrape
import it.skrape.selects.Doc
import it.skrape.selects.html5.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


fun main(): Unit = runBlocking {
    // todo this is commented out right now so that we don't hit a bunch of links, but just test one city for the
    // time being. feel free to do what you want here, but maybe add a delay between requests, because you can
    // possibly get blacklisted form a site if you hit them too fast from the same IP.

    val cityLinks : MutableList<CityStateDao> = mutableListOf()

    val extracted = skrape(HttpFetcher) {
        request {
            url = "https://www.city-data.com/"
        }

        extract {
            htmlDocument {
                div {
                    withClass="tab-content"
                    ul {
                        withClass = "tab-list-long"
                        li {
                            findAll {
                                this.mapIndexed { index, doc ->
                                    if (index > 1) return@findAll
                                    doc.eachLink.forEach { (state, url) ->
                                        cityLinks.addAll(extractCityDataFromState(state, "https:$url"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    launch {
        cityLinks.mapIndexed { index, dao ->
            if (index > 3) return@mapIndexed
            print(dao.cityName)
            extractCityData(dao)
        }
    }

}


data class CityEconDao(
    val description: String,
    val recentValue: Long,
    val yearOfRecentValue: Int,
    val oldValue: Long,
    val yearOfOldValue: Int
)


data class DemographicDao(val title: String, val percentageOfPopulation: Float, val totalPeople: Long? = null)


data class CityDataDao(
    var cityName: String = "Abernant",
    var state: String = "Alabama",
    var url: String = "",
    var populationTotal: Long = 0,
    var populationInYear: Int? = null,
    var malePopulation: Long? = null,
    var femalePopulation: Long? = null,
    var medianAge: Float? = null,
    var medianAgeForState: Float? = null,
    val zipCodes: MutableList<String> = mutableListOf(),
    var medianHouseholdIncome: CityEconDao? = null,
    var medianIncomePerCapita: CityEconDao? = null,
    var medianHouseCondoValue: CityEconDao? = null,
    var costOfLivingIndex: Float? = null,
    var costOfLivingIndexYearDataAcquired: Int? = null,
    var landAreaOfCity: MeasurementDao? = null,
    var populationDensity: MeasurementDao? = null,
    var elevation: MeasurementDao? = null,
    var raceDemographic: MutableList<DemographicDao> = mutableListOf(),
    var maritalStatus: MutableList<DemographicDao> = mutableListOf(),
    var educationDemographic: MutableList<DemographicDao> = mutableListOf(),
    var unemploymentDemographics: MutableList<DemographicDao> = mutableListOf()
)

data class MeasurementDao(val amount: Float, val unitOfMeasure: String)

fun CityEconDao.saveToDb(cityId: Long) {
    print("Saving $description to db")
    db.cityEconQueries.insert(CityEcon(-1, description, recentValue, yearOfRecentValue, oldValue, yearOfOldValue, cityId))
}

fun MeasurementDao.saveToDb(name: String, cityId: Long) {
    print("Saving $name to db")
    db.mesurementQueries.insert(Mesurement(-1, name, amount, unitOfMeasure, cityId ))
}

fun DemographicDao.saveToDb(type: String, cityId: Long) {
    print("Saving demographic: $type to db")
    db.demographicQueries.insert(Demographic(-1, type, title, percentageOfPopulation, totalPeople, cityId))
}

fun CityDataDao.saveToDb() {
    print("saving city data to db")
    db.cityDataQueries.insert(CityData(-1, cityName, state, url, populationTotal, populationInYear, malePopulation,
        femalePopulation, medianAge, medianAgeForState, costOfLivingIndex, costOfLivingIndexYearDataAcquired))

    val cityId = db.cityDataQueries.lastInsertedId().executeAsOne()

    landAreaOfCity?.saveToDb("Land Area of City", cityId)
    populationDensity?.saveToDb("Population Density", cityId)
    elevation?.saveToDb("Elevation", cityId)
    raceDemographic.forEach { it.saveToDb("Race", cityId) }
    maritalStatus.forEach { it.saveToDb("Marital Status", cityId) }
    educationDemographic.forEach { it.saveToDb("Education", cityId) }
    unemploymentDemographics.forEach { it.saveToDb("Unemployment", cityId) }
    medianHouseholdIncome?.saveToDb(cityId)
    medianIncomePerCapita?.saveToDb(cityId)
    medianHouseCondoValue?.saveToDb(cityId)
}


// currently this just extracts data for one city, but it will easily be hooked up to our link extractor and
// extract data for all cities. I do need to write some failsafe method so that if there's not a particular datapoint
// we don't hard crash and just leave that data null.
suspend fun extractCityData(cityState: CityStateDao) {
    val cityData = CityDataDao()

    cityData.cityName = cityState.cityName
    cityData.state = cityState.state
    cityData.url = cityState.cityUrl

    val extracted = skrape(HttpFetcher) {
        request {
            url = cityState.cityUrl
        }

        extract {
            htmlDocument {
                extractCityPopulation(cityData)
                extractMedianAge(cityData)
                extractCityZipCodes(cityData)
                extractIncomeDemographics(cityData)
                extractCostOfLivingIndex(cityData)
                extractRaceDemographics(cityData)
                extractPopulationDensity(cityData)
                extractCityElevation(cityData)
                extractMaritalStatus(cityData)
                extractEducationInfo(cityData)
                extractUnemployment(cityData)
            }
        }
    }
    cityData.saveToDb()
    print(cityData)
    print("\n")
    delay(3000)
}


private fun Doc.extractUnemployment(cityDataDao: CityDataDao) {
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


private fun Doc.extractEducationInfo(cityDataDao: CityDataDao) {
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

private fun Doc.extractMaritalStatus(cityDataDao: CityDataDao) {
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

private fun Doc.extractCityPopulation(cityDataDao: CityDataDao) {
    section {
        withClass = "city-population"
        findFirst {
            val groups = "Population in July (\\d{4}): ([0-9,]+).".toRegex().find(text)
            if (groups != null) {
                cityDataDao.apply {
                    populationTotal = groups.groupValues[2].replace(",", "").toLong()
                    populationInYear = groups.groupValues[1].toInt()
                }
            }
        }
    }
}

private fun Doc.extractMedianAge(cityDataDao: CityDataDao) {
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

private fun Doc.extractCityZipCodes(cityDataDao: CityDataDao) {
    section {
        withClass = "zip-codes"
        a {
            findAll {
                this.forEach {
                    cityDataDao.zipCodes.add(it.text)
                }
            }
        }
    }
}

private fun Doc.extractIncomeDemographics(cityDataDao: CityDataDao) {
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

private fun Doc.extractCostOfLivingIndex(cityDataDao: CityDataDao) {
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

private fun Doc.extractPopulationDensity(cityDataDao: CityDataDao) {
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

private fun Doc.extractCityElevation(cityDataDao: CityDataDao) = section {
    withClass = "elevation"

    findFirst {
        val groups = "Elevation: (\\d+) (\\S+)".toRegex().find(text)
        cityDataDao.apply {
            if (groups != null && groups.groupValues.size >= 3) {
                elevation = MeasurementDao(groups.groupValues[1].toFloat(), groups.groupValues[2])
            }
        }
    }
}

private fun Doc.extractRaceDemographics(cityDataDao: CityDataDao) {
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


fun parseEconValues(text: String, beginsWith: String): CityEconDao? {
    val groups = "$beginsWith in (\\d+): ([0-9,\\\$]+) \\(it was ([0-9,\\\$]+) in (\\d{4})\\)".toRegex().find(text)

    if (groups?.groupValues?.size == 5) {
        return CityEconDao(
            beginsWith,
            groups.groupValues[2].replace("$", "").replace(",", "").toLong(),
            groups.groupValues[1].toInt(),
            groups.groupValues[3].replace("$", "").replace(",", "").toLong(),
            groups.groupValues[4].toInt()
        )
    }
    return null
}


fun createLink(cityName: String, stateName: String): String {
    return "https://www.city-data.com/city/$cityName-$stateName.html"
}

fun createBasicLink(href: String): String {
    return "https://www.city-data.com/city/$href"
}

data class CityStateDao(val cityName: String, val state: String, val cityUrl:String)

fun extractCityDataFromState(stateName: String, stateUrl: String): List<CityStateDao> {
    val data : MutableList<CityStateDao> = mutableListOf()

    val extracted = skrape(HttpFetcher) {
        request {
            url = stateUrl
        }

        extract {
            htmlDocument {
                table {
                    withId = "cityTAB"
                    tr {
                        this.findAll {
                            mapIndexed { index, docElement ->
                                // skip the first row because this just contains the headings
                                if (index == 0) return@mapIndexed
                                docElement.td {
                                    findAll {
                                        var cityName: String = ""
                                        var linkUrl: String = ""
                                        mapIndexed { index, docElement ->

                                            // skip the first col because this just contains map info
                                            if (index != 0) {
                                                // function l(url) { window.location = url + "-" + stateName;}
                                                if (index == 1) {
                                                    cityName = if(docElement.text.contains(",")) {
                                                        docElement.text.replaceRange(docElement.text.indexOf(","), docElement.text.length, "")
                                                    } else {
                                                        docElement.text
                                                    }
                                                    print("$cityName \n")

//                                                    print("city name: $cityName\n")
                                                    // if link ends with .html don't do this
                                                    // docElement.eachHref.first().replace("javascript:l(\"", "").replace("\");", "")
                                                    linkUrl = if (docElement.eachHref.first().endsWith(".html")) {
                                                        createBasicLink(docElement.eachHref.first())
                                                    } else {
                                                        createLink(
                                                            docElement.eachHref.first()
                                                                .replace("javascript:l(\"", "")
                                                                .replace("\");", ""),
                                                            stateName
                                                        )
                                                    }


                                                } else if (index == 2) {
//                                                    print("city population: ${docElement.text}\n")
                                                }
                                            }
                                        }
                                        data.add(CityStateDao(cityName, stateName, linkUrl))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    return data
}