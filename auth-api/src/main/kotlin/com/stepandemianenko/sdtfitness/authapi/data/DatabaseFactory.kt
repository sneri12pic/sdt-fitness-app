package com.stepandemianenko.sdtfitness.authapi.data

import com.stepandemianenko.sdtfitness.authapi.config.AuthConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

object DatabaseFactory {
    fun createDataSource(config: AuthConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.databaseUrl
            username = config.databaseUser
            password = config.databasePassword
            maximumPoolSize = 10
            minimumIdle = 1
            poolName = "sdt-auth-api"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }

    fun migrate(dataSource: HikariDataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}
