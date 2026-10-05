package ru.itmo.repository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DatabaseMigrationStartup {
    @Resource(lookup = "jdbc/LabDS")
    private DataSource dataSource;

    @PostConstruct
    private void migrate() {
        Flyway.configure().dataSource(dataSource).load().migrate();
    }
}
