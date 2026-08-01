package org.urfu.semyonovowa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки Telegram-бота, считываемые из префикса {@code bot} в application.yml.
 * Значения подставляются из переменных окружения, чтобы токен не попадал в репозиторий.
 *
 * @param name          имя бота (@username)
 * @param token         токен, выданный BotFather
 * @param creatorChatId chatId создателя для отправки сообщений об ошибках
 */
@ConfigurationProperties(prefix = "bot")
public record BotProperties(String name, String token, Long creatorChatId) {}
