import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extract
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.*
import it.skrape.selects.text


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



data class CityData(var medianAge: String? = null,
                    var medianAgeForState: String? = null,
                    val zipCodes : MutableList<String> = mutableListOf())

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
                section {
                    withClass = "median-age"
                    tr {
                        findFirst {
                            cityData.medianAge = text
                            print("$text\n")
                        }
                        findSecond {
                            cityData.medianAgeForState = text
                            print("$text\n")
                        }
                    }
                }

                section {
                    withClass = "zip-codes"
                    b {
                        findAll {
                            cityData.zipCodes.add(text)
                        }
                    }
                }

                section {
                    withClass = "median-income"
                    findFirst {
                        val medianHomeIncome = parseEconValues(text, "Estimated median household income")
                        val perCapitaIncome = parseEconValues(text, "Estimated per capita income")
                        val meidanHouseCondoValue = parseEconValues(text, "Estimated median house or condo value")
                        print(medianHomeIncome)
                        print(perCapitaIncome)
                        print(meidanHouseCondoValue)
                    }
                }

                // cost of living info
                section { withClass = "cost-of-living-index"
                    findFirst {
                        val groups = "(.+ \\d{4}) cost of living index in .+: ([0-9.]+)".toRegex().find(text)
                        val timeDataWasGathered = groups?.groupValues?.get(1)
                        val costOfLivingIndex = groups?.groupValues?.get(2)

                        print("time data taken: $timeDataWasGathered. Cost of living index: $costOfLivingIndex \n")
                    }
                }

                section {
                    withClass = "population-density"
                    findFirst {
                        val groups = "Land area: ([0-9.]+) square (\\S+).".toRegex().find(text)
                        val areaValue = groups?.groupValues?.get(1)
                        val areaUnite = groups?.groupValues?.get(2)

                        val populationDensityGroups = "Population density: (\\d+) people per square (\\S+)".toRegex().find(text)
                        // todo these will be saved in a data class.
                        val peoplePerUnit = groups?.groupValues?.get(1)
                        val areaUnite2 = groups?.groupValues?.get(2)
                    }
                }
            }
        }
    }
}


data class CityEcon(val description: String, val recentValue: String, val yearOfRecentValue: String, val oldValue: String, val yearOfOldValue: String)

fun parseEconValues(text:String, beginsWith: String): CityEcon? {
    val groups =  "$beginsWith in (\\d+): ([0-9,\\\$]+) \\(it was ([0-9,\\\$]+) in (\\d{4})\\)".toRegex().find(text)

    if (groups?.groupValues?.size == 5) {
        return CityEcon(beginsWith, groups.groupValues[2], groups.groupValues[1],groups.groupValues[3], groups.groupValues[4])
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
                               if(index == 0) return@mapIndexed
                               docElement.td {
                                   findAll {
                                       mapIndexed { index, docElement ->
                                           // skip the first col because this just contains map info
                                           if(index != 0) {
                                               // function l(url) { window.location = url + "-" + stateName;}
                                               if(index == 1) {
                                                   print("city name: ${docElement.text}\n")

                                                   // if link ends with .html don't do this
                                                   // docElement.eachHref.first().replace("javascript:l(\"", "").replace("\");", "")
                                                   if(docElement.eachHref.first().endsWith(".html")) {
                                                       print("link : ${docElement.eachHref.first()}\n")
                                                   } else {
                                                       print("link : ${createLink(docElement.eachHref.first().replace("javascript:l(\"", "").replace("\");", ""), stateName)}\n")
                                                   }


                                               } else if(index == 2) {
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