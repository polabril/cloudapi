package com.gabriela.cloudapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        // Try multiple environment variable names in order of preference
        String dbUrl = env.getProperty("DB_URL");
        String dbUser = env.getProperty("DB_USER");
        String dbPassword = env.getProperty("DB_PASSWORD");

        // Diagnostic helpers: check raw System.getenv and resolved spring property
        String sysDbUrl = System.getenv("DB_URL");
        String springDatasourceUrl = env.getProperty("spring.datasource.url");
        log.info("DB env presence: DB_URL (env.getProperty)? {} ; System.getenv(DB_URL)? {} ; spring.datasource.url={}",
                dbUrl != null && !dbUrl.isBlank(), sysDbUrl != null && !sysDbUrl.isBlank(),
                springDatasourceUrl == null ? "<null>" : (springDatasourceUrl.contains("${") ? "<unresolved>" : mask(springDatasourceUrl)));

        // Allow using spring.datasource.url (resolved) or System.getenv as fallbacks
        if ((dbUrl == null || dbUrl.isBlank()) && sysDbUrl != null && !sysDbUrl.isBlank()) {
            log.info("Falling back to System.getenv(DB_URL)");
            dbUrl = sysDbUrl;
        }
        if ((dbUrl == null || dbUrl.isBlank()) && springDatasourceUrl != null && !springDatasourceUrl.isBlank() && !springDatasourceUrl.contains("${")) {
            log.info("Falling back to spring.datasource.url property");
            dbUrl = springDatasourceUrl;
        }

        if (dbUrl != null && !dbUrl.isBlank()) {
            String url = dbUrl.trim();
            // Convert postgres://user:pass@host:port/db into JDBC url when supplied in DB_URL
            if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
                try {
                    URI uri = new URI(url);
                    String userInfo = uri.getUserInfo();
                    String username = dbUser;
                    String password = dbPassword;
                    if (userInfo != null) {
                        String[] parts = userInfo.split(":", 2);
                        username = parts[0];
                        if (parts.length > 1) password = parts[1];
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("jdbc:postgresql://")
                            .append(uri.getHost())
                            .append(uri.getPort() == -1 ? "" : ":" + uri.getPort())
                            .append(uri.getPath());
                    if (uri.getQuery() != null && !uri.getQuery().isBlank()) sb.append("?").append(uri.getQuery());

                    url = sb.toString();
                    dbUser = username;
                    dbPassword = password;
                } catch (URISyntaxException e) {
                    throw new IllegalStateException("Invalid DB_URL format", e);
                }
            }

            log.info("Using database configuration from DB_URL/JDBC string (masked)={} ", mask(url));
            return DataSourceBuilder.create()
                    .url(url)
                    .username(dbUser)
                    .password(dbPassword)
                    .driverClassName(driverClassName)
                    .build();
        }

        // Fallback: check JDBC_DATABASE_URL (already in JDBC format), e.g. provided by some PaaS
        String jdbcDatabaseUrl = env.getProperty("JDBC_DATABASE_URL");
        if (jdbcDatabaseUrl != null && !jdbcDatabaseUrl.isBlank()) {
            String jdbcUser = env.getProperty("JDBC_DATABASE_USERNAME", dbUser);
            String jdbcPass = env.getProperty("JDBC_DATABASE_PASSWORD", dbPassword);
            log.info("Using database configuration from JDBC_DATABASE_URL");
            return DataSourceBuilder.create()
                    .url(jdbcDatabaseUrl)
                    .username(jdbcUser)
                    .password(jdbcPass)
                    .driverClassName(driverClassName)
                    .build();
        }

        // Fallback: parse DATABASE_URL (e.g. postgres://user:pass@host:port/dbname)
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            try {
                URI uri = new URI(databaseUrl);

                String userInfo = uri.getUserInfo();
                String username = null;
                String password = null;
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    username = parts[0];
                    if (parts.length > 1) password = parts[1];
                }

                StringBuilder url = new StringBuilder();
                url.append("jdbc:postgresql://")
                        .append(uri.getHost())
                        .append( uri.getPort() == -1 ? "" : ":" + uri.getPort() )
                        .append(uri.getPath());

                if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                    url.append("?").append(uri.getQuery());
                }

                log.info("Using database configuration from DATABASE_URL (converted to JDBC)");
                return DataSourceBuilder.create()
                        .url(url.toString())
                        .username(username)
                        .password(password)
                        .driverClassName(driverClassName)
                        .build();

            } catch (URISyntaxException e) {
                throw new IllegalStateException("Invalid DATABASE_URL format", e);
            }
        }

        throw new IllegalStateException("No database configuration found. Set one of: DB_URL (jdbc:postgresql://...), JDBC_DATABASE_URL, or DATABASE_URL (postgres://user:pass@host:port/db). Useful env examples: DB_URL=jdbc:postgresql://host:5432/dbname with DB_USER and DB_PASSWORD, or set DATABASE_URL provided by some PaaS.");
    }

    // Mask user:pass@ from URLs for safe logging
    private String mask(String url) {
        if (url == null) return "<null>";
        return url.replaceAll("^(.*://)[^@]+@(.+)$", "$1***@$2");
    }
}