package com.citydata

import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extract
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.table
import it.skrape.selects.html5.td
import it.skrape.selects.html5.tr
import kotlinx.coroutines.delay
import kotlin.math.log


fun extractCityDataFromState(stateName: String, stateUrl: String): List<CityStateDao> {
    val data : MutableList<CityStateDao> = mutableListOf()

    skrape(HttpFetcher) {
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
                                        var cityName = ""
                                        var linkUrl = ""
                                        mapIndexed { index, docElement ->

                                            // skip the first col because this just contains map info
                                            if (index != 0) {
                                                if (index == 1) {
                                                    cityName = if(docElement.text.contains(",")) {
                                                        docElement.text.replaceRange(docElement.text.indexOf(","), docElement.text.length, "")
                                                    } else {
                                                        docElement.text
                                                    }
                                                    logger.info { cityName }

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
                                                    logger.info { "city population: ${docElement.text}" }
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