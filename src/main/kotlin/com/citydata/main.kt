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
import java.lang.Exception


fun main(): Unit = runBlocking {
    // todo this is commented out right now so that we don't hit a bunch of links, but just test one city for the
    // time being. feel free to do what you want here, but maybe add a delay between requests, because you can
    // possibly get blacklisted form a site if you hit them too fast from the same IP.

    val cityLinks: MutableList<CityStateDao> = mutableListOf()
    stateExtractor(cityLinks)
    launch {
        cityLinks.mapIndexed { index, dao ->
            if (index > 50) return@mapIndexed
            logger.info { "Extracting: ${index+1} of ${cityLinks.size} - ${dao.cityName}   ${dao.cityUrl} "  }
            extractCityData(dao)
        }
    }

}


















