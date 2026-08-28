package org.urfu.semyonovowa.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.urfu.semyonovowa.dataBase.GameSnapshot;

/**
 * Reachability-метаданные для нативного образа (GraalVM), которые нельзя вывести
 * статическим анализом.
 *
 * <p>Ресурсы: {@code texts/*.txt} и {@code images/*.png} грузятся с classpath через
 * {@code getResourceAsStream} (правила, инфа о проекте, картинка лобби). Без явной
 * регистрации они не попадут в бинарь и дадут null в рантайме.
 *
 * <p>Рефлексия: {@link GameSnapshot} с вложенными записями сериализуется/десериализуется
 * Jackson-ом в JSON-поле таблицы games. Для биндинга через рефлексию в нативном образе
 * их граф типов нужно зарегистрировать.
 *
 * <p>Метаданные для сторонних библиотек (PostgreSQL JDBC, Flyway, Caffeine) подтягиваются
 * автоматически: Flyway/драйвер — через native-хинты Spring Boot, остальное — из
 * GraalVM Reachability Metadata Repository (включён в Native Build Tools по умолчанию).
 * Рефлексия Jackson-модели telegrambots здесь НЕ покрыта — это добивается на шаге 20b
 * по реальным ошибкам запуска нативного бинаря.
 */
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativeHints.AppResourceHints.class)
@RegisterReflectionForBinding({
        GameSnapshot.class,
        GameSnapshot.PlayerState.class,
        GameSnapshot.ShipState.class
})
public class NativeHints
{
    static class AppResourceHints implements RuntimeHintsRegistrar
    {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader)
        {
            hints.resources().registerPattern("texts/*.txt");
            hints.resources().registerPattern("images/*.png");
        }
    }
}
