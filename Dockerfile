# ===== Стадия сборки =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Сначала только pom — слой с зависимостями кэшируется, пока pom не меняется
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# Затем исходники и сборка исполняемого jar. Тесты гоняет CI отдельно, в образе
# их пропускаем: интеграционные на Testcontainers потребовали бы Docker-in-Docker.
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# ===== Стадия рантайма =====
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Непривилегированный пользователь — бот не должен работать от root
RUN addgroup -S bot && adduser -S bot -G bot

# Только исполняемый jar (repackage оставляет один *.jar; исходный — *.jar.original)
COPY --from=build --chown=bot:bot /build/target/*.jar app.jar

USER bot

# Long-polling бот: входящих портов нет, EXPOSE не нужен. Вся конфигурация — через
# переменные окружения: DB_URL, DB_USER, DB_PASSWORD, BOT_TOKEN, BOT_NAME,
# BOT_CREATOR_CHAT_ID (см. application.yml). JVM 21 сама учитывает лимиты контейнера.
ENTRYPOINT ["java", "-jar", "app.jar"]
