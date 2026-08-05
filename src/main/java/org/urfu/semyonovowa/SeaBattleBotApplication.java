package org.urfu.semyonovowa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Точка входа приложения. Заменяет ручной {@code Main} со чтением
 * property-файлов из аргументов командной строки: теперь конфигурация,
 * логирование и жизненный цикл управляются Spring Boot.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SeaBattleBotApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(SeaBattleBotApplication.class, args);
    }
}
