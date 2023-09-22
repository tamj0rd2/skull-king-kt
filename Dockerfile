FROM node:18.2.0

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
CMD ["java", "-jar", "./build/libs/skull-king-kt-1.0-SNAPSHOT.jar"]