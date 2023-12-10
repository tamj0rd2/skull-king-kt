FROM bellsoft/liberica-openjdk-debian:21.0.1 as build

WORKDIR /app/skull-king-kt
COPY ./gradle ./gradle
COPY ./gradlew ./gradle.properties ./settings.gradle.kts ./
COPY ./common ./common
COPY ./frontend ./frontend
COPY ./frontend-svelte ./frontend-svelte
COPY ./frontend-solid ./frontend-solid
COPY ./app ./app
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon -i clean build -x test

FROM bellsoft/liberica-openjdk-alpine:21.0.1 as final

WORKDIR /app/skull-king-kt
COPY --from=build /app/skull-king-kt/app/build/libs/app-1.0-SNAPSHOT.jar ./build/libs/app-1.0-SNAPSHOT.jar
COPY --from=build /app/skull-king-kt/app/src/main/resources ./src/main/resources
CMD ["java", "-jar", "./build/libs/app-1.0-SNAPSHOT.jar"]
