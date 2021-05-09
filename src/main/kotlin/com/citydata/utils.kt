package com.citydata


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