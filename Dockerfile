# =====================================================================
#  Многоэтапная сборка консольного приложения «Йога-студия».
#  Этап 1: сборка исполняемого jar Maven'ом внутри контейнера —
#          на хосте не нужны ни JDK, ни Maven.
#  Этап 2: лёгкий образ только с JRE и готовым jar-файлом.
# =====================================================================

# ---------- Этап 1: сборка ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Сначала только pom.xml — слой с зависимостями кэшируется отдельно
# от исходников и не пересобирается при каждом изменении кода.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Этап 2: запуск ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Явная UTF-8 кодировка вывода — иначе кириллица в консоли может
# отображаться некорректно в зависимости от локали хоста/терминала.
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

COPY --from=build /build/target/yoga-studio.jar ./yoga-studio.jar

# Каталог для экспорта Excel/CSV — монтируется наружу через docker-compose,
# чтобы выгруженные файлы были доступны на хосте.
RUN mkdir -p /app/export
VOLUME ["/app/export"]

ENTRYPOINT ["java", "-jar", "yoga-studio.jar"]
