package com.citydata.db

import com.citydata.*


fun CityEconDao.saveToDb(cityId: Long) {
//    println("Saving $description to db")
    db.cityEconQueries.insert(CityEcon(-1, description, recentValue, yearOfRecentValue, oldValue, yearOfOldValue, cityId))
}

fun MeasurementDao.saveToDb(name: String, cityId: Long) {
//    println("Saving $name to db")
    db.mesurementQueries.insert(Mesurement(-1, name, amount, unitOfMeasure, cityId ))
}

fun DemographicDao.saveToDb(type: String, cityId: Long) {
//    println("Saving demographic: $type to db")
    db.demographicQueries.insert(Demographic(-1, type, title, percentageOfPopulation, totalPeople, cityId))
}

fun CityDataDao.saveToDb() {
    logger.info { "Saving city data to db" }
    db.cityDataQueries.insert(
        CityData(-1, cityName, state, url, populationTotal, populationInYear, malePopulation,
        femalePopulation, medianAge, medianAgeForState, medianGrossRent, medianGrossRentYearDataAcquired, costOfLivingIndex, costOfLivingIndexYearDataAcquired)
    )

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