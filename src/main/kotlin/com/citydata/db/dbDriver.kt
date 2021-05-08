package com.citydata.db


import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver

val db by lazy {
    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:./test.db")
    CityDataDB.Schema.create(driver)
    CityDataDB(driver)
}

