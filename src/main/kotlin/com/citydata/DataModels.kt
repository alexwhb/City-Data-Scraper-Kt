package com.citydata

data class CityStateDao(val cityName: String,
                        val state: String, // this is like Oregon
                        val cityUrl:String,
                        val population: Long)

data class CityEconDao(
    val description: String,
    val recentValue: Long,
    val yearOfRecentValue: Int,
    val oldValue: Long,
    val yearOfOldValue: Int
)

data class DemographicDao(val title: String, val percentageOfPopulation: Float, val totalPeople: Long? = null)

data class CityDataDao(
    var cityName: String = "",
    var state: String = "",
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
    var populationLivingInPovertyYearOfData: Int? = null,
    var percentageOfPopulationLivingInPoverty: Float? = null,
    var medianGrossRent:Long? = null,
    var medianGrossRentYearDataAcquired : Int? = null,
    var costOfLivingIndex: Float? = null,
    var costOfLivingIndexYearDataAcquired: Int? = null,
    var landAreaOfCity: MeasurementDao? = null,
    var populationDensity: MeasurementDao? = null,
    var elevation: MeasurementDao? = null,
    var raceDemographic: MutableList<DemographicDao> = mutableListOf(),
    var maritalStatus: MutableList<DemographicDao> = mutableListOf(),
    var educationDemographic: MutableList<DemographicDao> = mutableListOf(),
    var unemploymentDemographics: MutableList<DemographicDao> = mutableListOf(),
    var walkScoreInfo: WalkScoreInfo = WalkScoreInfo()
)

data class MeasurementDao(val amount: Float, val unitOfMeasure: String)
