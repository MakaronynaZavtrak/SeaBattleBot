package org.urfu.semyonovowa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки подключения к базе данных, считываемые из префикса {@code db} в application.yml.
 * Пока используется «сырой» JDBC — пул соединений (HikariCP) появится на этапе 4.
 *
 * @param forName  полное имя JDBC-драйвера
 * @param url      строка подключения к базе
 * @param user     имя пользователя базы
 * @param password пароль пользователя базы
 */
@ConfigurationProperties(prefix = "db")
public record DatabaseProperties(String forName, String url, String user, String password) {}
