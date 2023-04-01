package com.citydata

import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extract
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.div
import it.skrape.selects.html5.li
import it.skrape.selects.html5.ul

fun stateExtractor(cityLinks: MutableList<CityStateDao>) {
    skrape(HttpFetcher) {
        request {
            url = "https://www.city-data.com/"
        }

        extract {
            htmlDocument {
                div {
                    withClass = "tab-content"
                    ul {
                        withClass = "tab-list-long"
                        li {
                            findAll {
                                this.mapIndexed { index, doc ->
                                    if (index > 2) return@findAll
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
}