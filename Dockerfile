FROM node:18.2.0 as build

RUN apt-get update \
      && DEBIAN_FRONTEND="noninteractive" apt-get install --assume-yes --no-install-recommends \
        "ca-certificates" \
        "fontconfig" \
        "locales" \
        "tzdata" \
      && echo "en_GB.UTF-8 UTF-8" >> "/etc/locale.gen" \
      && locale-gen "en_GB.UTF-8" \
      && (cd "/var/lib/apt/lists" && rm --force --recursive *)
ENV LANG="en_GB.UTF-8" LANGUAGE="en_GB:en" LC_ALL="en_GB.UTF-8"
ENV JAVA_HOME="/usr/lib/jvm/jdk"
COPY --from="bellsoft/liberica-openjdk-debian:21.0.1-12" "${JAVA_HOME}" "${JAVA_HOME}"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app/skull-king-kt
COPY ./gradle ./gradle
COPY ./gradlew .
COPY ./gradle.properties .
COPY ./settings.gradle.kts .
COPY ./build.gradle.kts .
COPY ./src ./src

RUN ./gradlew build -x test

FROM gradle:latest as final

RUN apt-get update \
      && DEBIAN_FRONTEND="noninteractive" apt-get install --assume-yes --no-install-recommends \
        "ca-certificates" \
        "fontconfig" \
        "locales" \
        "tzdata" \
      && echo "en_GB.UTF-8 UTF-8" >> "/etc/locale.gen" \
      && locale-gen "en_GB.UTF-8" \
      && (cd "/var/lib/apt/lists" && rm --force --recursive *)
ENV LANG="en_GB.UTF-8" LANGUAGE="en_GB:en" LC_ALL="en_GB.UTF-8"
ENV JAVA_HOME="/usr/lib/jvm/jdk"
COPY --from="bellsoft/liberica-openjdk-debian:21.0.1-12" "${JAVA_HOME}" "${JAVA_HOME}"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app/skull-king-kt
COPY --from=build /app/skull-king-kt/build/libs/skull-king-kt-1.0-SNAPSHOT.jar ./build/libs/skull-king-kt-1.0-SNAPSHOT.jar
COPY --from=build /app/skull-king-kt/src/main/resources ./src/main/resources
CMD ["java", "-jar", "./build/libs/skull-king-kt-1.0-SNAPSHOT.jar"]
