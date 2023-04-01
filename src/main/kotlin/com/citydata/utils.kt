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


fun convertStateNameToShort(stateName: String): String? {
    val statesMap = mapOf(
        "Alabama" to "AL",
        "Alaska" to "AK",
        "Arizona" to "AZ",
        "Arkansas" to "AR",
        "California" to "CA",
        "Colorado" to "CO",
        "Connecticut" to "CT",
        "Delaware" to "DE",
        "Florida" to "FL",
        "Georgia" to "GA",
        "Hawaii" to "HI",
        "Idaho" to "ID",
        "Illinois" to "IL",
        "Indiana" to "IN",
        "Iowa" to "IA",
        "Kansas" to "KS",
        "Kentucky" to "KY",
        "Louisiana" to "LA",
        "Maine" to "ME",
        "Maryland" to "MD",
        "Massachusetts" to "MA",
        "Michigan" to "MI",
        "Minnesota" to "MN",
        "Mississippi" to "MS",
        "Missouri" to "MO",
        "Montana" to "MT",
        "Nebraska" to "NE",
        "Nevada" to "NV",
        "New Hampshire" to "NH",
        "New Jersey" to "NJ",
        "New Mexico" to "NM",
        "New York" to "NY",
        "North Carolina" to "NC",
        "North Dakota" to "ND",
        "Ohio" to "OH",
        "Oklahoma" to "OK",
        "Oregon" to "OR",
        "Pennsylvania" to "PA",
        "Rhode Island" to "RI",
        "South Carolina" to "SC",
        "South Dakota" to "SD",
        "Tennessee" to "TN",
        "Texas" to "TX",
        "Utah" to "UT",
        "Vermont" to "VT",
        "Virginia" to "VA",
        "Washington" to "WA",
        "West Virginia" to "WV",
        "Wisconsin" to "WI",
        "Wyoming" to "WY"
    )
    return statesMap[stateName]
}