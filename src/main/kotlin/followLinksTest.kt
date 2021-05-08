import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extract
import it.skrape.fetcher.skrape
import it.skrape.selects.Doc
import it.skrape.selects.html5.*


fun main() {
    // todo this is commented out right now so that we don't hit a bunch of links, but just test one city for the
    // time being. feel free to do what you want here, but maybe add a delay between requests, because you can
    // possibly get blacklisted form a site if you hit them too fast from the same IP.

//    val extracted = skrape(HttpFetcher) {
//        request {
//            url = "https://www.city-data.com/"
//        }
//
//        extract {
//            htmlDocument {
//                div {
//                    withClass="tab-content"
//                    ul {
//                        withClass = "tab-list-long"
//                        li {
//                            findAll {
//                                this.mapIndexed { index, doc ->
//                                    if (index > 2) return@findAll
//                                    doc.eachLink.forEach { (state, url) ->
//                                        print("state: $state, url : https:$url\n")
//                                        extractCityDataFromState(state, "https:$url")
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
    extractCityData()
}

data class CityEcon(
    val description: String,
    val recentValue: String,
    val yearOfRecentValue: String,
    val oldValue: String,
    val yearOfOldValue: String
)

data class RaceDemographic(val title: String, val percentageOfPopulation: Float, val totalPeople: Int)

data class MartelStatus(val title: String, val percentage: Float)

data class EducationDemographic(val title: String, val percentage: Float)

data class UnemploymentDemographic(val title:String, val percentage: Float)

data class CityData(
    var populationTotal: Int? = null,
    var populationInYear: String? = null,
    var malePopulation: Int? = null,
    var femalePopulation: Int? = null,
    var medianAge: Float? = null,
    var medianAgeForState: Float? = null,
    val zipCodes: MutableList<String> = mutableListOf(),
    var medianHouseholdIncome: CityEcon? = null,
    var medianIncomePerCapita: CityEcon? = null,
    var medianHouseCondoValue: CityEcon? = null,
    var costOfLivingIndex: String? = null,
    var costOfLivingIndexDateDataAcquired: String? = null,
    var landAreaOfCity: Measurement? = null,
    var populationDensity: Measurement? = null,
    var elevation: Measurement? = null,
    var raceDemographic: MutableList<RaceDemographic> = mutableListOf(),
    var maritalStatus: MutableList<MartelStatus> = mutableListOf(),
    var educationDemographic: MutableList<EducationDemographic> = mutableListOf(),
    var unemploymentDemographics: MutableList<UnemploymentDemographic> = mutableListOf()
)

data class Measurement(val amount: Float, val unitOfMeasure: String)


// currently this just extracts data for one city, but it will easily be hooked up to our link extractor and
// extract data for all cities. I do need to write some failsafe method so that if there's not a particular datapoint
// we don't hard crash and just leave that data null.
fun extractCityData() {
    val cityData = CityData()
    val extracted = skrape(HttpFetcher) {
        request {
            url = "https://www.city-data.com/city/Abernant-Alabama.html"
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
        print(cityData)
}


private fun Doc.extractUnemployment(cityData: CityData) {
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
                        cityData.unemploymentDemographics.add(UnemploymentDemographic(title, percent))
                    }
                }
            }
        }
    }
}


private fun Doc.extractEducationInfo(cityData: CityData) {
    section {
        withClass = "education-info"
        ul {
            li {
                findAll {
                    forEachIndexed { index, docElement ->
                        if (index == this.size - 1) return@findAll
                        val (status, percentage) = docElement.text.split(": ")
                        cityData.educationDemographic.add(
                            EducationDemographic(
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

private fun Doc.extractMaritalStatus(cityData: CityData) {
    section {
        withClass = "marital-info"

        ul {
            li {
                findAll {
                    forEach {
                        val (status, percentage) = it.text.split(": ")
                        cityData.maritalStatus.add(MartelStatus(status, percentage.replace("%", "").toFloat()))
                    }
                }
            }
        }
    }
}

private fun Doc.extractCityPopulation(cityData: CityData) {
    section {
        withClass = "city-population"
        findFirst {
            val groups = "Population in July (\\d{4}): ([0-9,]+).".toRegex().find(text)
            if (groups != null) {
                cityData.apply {
                    populationTotal = groups.groupValues[2].replace(",", "").toInt()
                    populationInYear = groups.groupValues[1]
                }
            }
        }
    }
}

private fun Doc.extractMedianAge(cityData: CityData) {
    section {
        withClass = "median-age"
        tr {
            findFirst {
                val groups = "Median resident age: ([0-9.]+) years".toRegex().find(text)
                if (groups != null) cityData.medianAge = groups.groupValues[1].toFloatOrNull()

            }
            findSecond {
                val groups = " median age: ([0-9.]+) years".toRegex().find(text)
                if (groups != null) cityData.medianAgeForState = groups.groupValues[1].toFloatOrNull()

            }
        }
    }
}

private fun Doc.extractCityZipCodes(cityData: CityData) {
    section {
        withClass = "zip-codes"
        a {
            findAll {
                this.forEach {
                    cityData.zipCodes.add(it.text)
                }
            }
        }
    }
}

private fun Doc.extractIncomeDemographics(cityData: CityData) {
    section {
        withClass = "median-income"
        findFirst {
            cityData.apply {
                medianHouseholdIncome = parseEconValues(text, "Estimated median household income")
                medianIncomePerCapita = parseEconValues(text, "Estimated per capita income")
                medianHouseCondoValue = parseEconValues(text, "Estimated median house or condo value")
            }
        }
    }
}

private fun Doc.extractCostOfLivingIndex(cityData: CityData) {
    section {
        withClass = "cost-of-living-index"
        findFirst {
            val groups = "(.+ \\d{4}) cost of living index in .+: ([0-9.]+)".toRegex().find(text)
            cityData.apply {
                costOfLivingIndex = groups?.groupValues?.get(2)
                costOfLivingIndexDateDataAcquired = groups?.groupValues?.get(1)
            }
        }
    }
}

private fun Doc.extractPopulationDensity(cityData: CityData) {
    section {
        withClass = "population-density"
        findFirst {
            cityData.apply {
                val groups = "Land area: ([0-9.]+) (\\S+).".toRegex().find(text)
                if (groups != null && groups.groupValues.size >= 3) {
                    landAreaOfCity = Measurement(groups.groupValues[1].toFloat(), groups.groupValues[2])
                }
            }

            cityData.apply {
                val groups = "Population density: (\\d+) people per (\\S+)".toRegex().find(text)
                if (groups != null && groups.groupValues.size >= 3) {
                    populationDensity = Measurement(groups.groupValues[1].toFloat(), groups.groupValues[2])
                }
            }
        }
    }
}

private fun Doc.extractCityElevation(cityData: CityData) = section {
    withClass = "elevation"

    findFirst {
        val groups = "Elevation: (\\d+) (\\S+)".toRegex().find(text)
        cityData.apply {
            if (groups != null && groups.groupValues.size >= 3) {
                elevation = Measurement(groups.groupValues[1].toFloat(), groups.groupValues[2])
            }
        }
    }
}

private fun Doc.extractRaceDemographics(cityData: CityData) {
    section {
        withClass = "races-graph"
        ul {
            withClass = "list-group"
            findSecond {
                li {
                    findAll {
                        forEach {
                            var title = ""
                            var numberOfPeople = 0
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
                                        numberOfPeople = text.replace(",", "").toInt()
                                    }
                                }
                            }
                            cityData.raceDemographic.add(
                                RaceDemographic(
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


fun parseEconValues(text: String, beginsWith: String): CityEcon? {
    val groups = "$beginsWith in (\\d+): ([0-9,\\\$]+) \\(it was ([0-9,\\\$]+) in (\\d{4})\\)".toRegex().find(text)

    if (groups?.groupValues?.size == 5) {
        return CityEcon(
            beginsWith,
            groups.groupValues[2],
            groups.groupValues[1],
            groups.groupValues[3],
            groups.groupValues[4]
        )
    }
    return null
}


fun createLink(cityName: String, stateName: String): String {
    return "https://www.city-data.com/city/$cityName-$stateName.html"
}

fun extractCityDataFromState(stateName: String, stateUrl: String) {
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
                                        mapIndexed { index, docElement ->
                                            // skip the first col because this just contains map info
                                            if (index != 0) {
                                                // function l(url) { window.location = url + "-" + stateName;}
                                                if (index == 1) {
                                                    print("city name: ${docElement.text}\n")

                                                    // if link ends with .html don't do this
                                                    // docElement.eachHref.first().replace("javascript:l(\"", "").replace("\");", "")
                                                    if (docElement.eachHref.first().endsWith(".html")) {
                                                        print("link : ${docElement.eachHref.first()}\n")
                                                    } else {
                                                        print(
                                                            "link : ${
                                                                createLink(
                                                                    docElement.eachHref.first()
                                                                        .replace("javascript:l(\"", "")
                                                                        .replace("\");", ""),
                                                                    stateName
                                                                )
                                                            }\n"
                                                        )
                                                    }


                                                } else if (index == 2) {
                                                    print("city population: ${docElement.text}\n")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}