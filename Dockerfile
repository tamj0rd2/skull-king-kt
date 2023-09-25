FROM node:18.2.0 as build

RUN apt-get update
RUN #apt-get -y install default-jre
RUN apt-get -y install openjdk-17-jre-headless

WORKDIR /app/skull-king-kt
COPY ./gradle ./gradle
COPY ./gradlew .
COPY ./gradle.properties .
COPY ./settings.gradle.kts .
COPY ./build.gradle.kts .
COPY ./src ./src

RUN ./gradlew build -x test

FROM gradle:latest

WORKDIR /app/skull-king-kt
COPY --from=build /app/skull-king-kt/build/libs/skull-king-kt-1.0-SNAPSHOT.jar ./build/libs/skull-king-kt-1.0-SNAPSHOT.jar
COPY --from=build /app/skull-king-kt/src/main/resources ./src/main/resources
CMD ["java", "-jar", "./build/libs/skull-king-kt-1.0-SNAPSHOT.jar"]
