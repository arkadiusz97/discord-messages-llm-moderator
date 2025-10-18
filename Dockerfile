FROM openjdk:25 AS builder
WORKDIR /usr/src/app
RUN microdnf install findutils
COPY build.gradle gradlew settings.gradle ./
COPY gradle gradle
RUN ./gradlew dependencies
COPY src src
RUN ./gradlew clean build

FROM openjdk:25 AS final
ENV DISCORD_BOT_TOKEN=${DISCORD_BOT_TOKEN}
WORKDIR /usr/src/app
COPY --from=builder /usr/src/app/build/libs/*.jar /usr/src/app/*.jar
ENTRYPOINT ["java", "-jar", "/usr/src/app/*.jar"]
