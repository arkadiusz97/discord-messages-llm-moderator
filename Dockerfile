FROM amazoncorretto:25.0.1 AS builder
RUN --mount=type=secret,id=discord_bot_token \
    --mount=type=secret,id=rabbitmq_default_user \
    --mount=type=secret,id=rabbitmq_default_pass
WORKDIR /usr/src/app
RUN dnf install -y findutils
COPY build.gradle gradlew settings.gradle ./
COPY gradle gradle
RUN ./gradlew dependencies
COPY src src
RUN ./gradlew clean build

FROM amazoncorretto:25.0.1 AS final
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
WORKDIR /usr/src/app
COPY --from=builder /usr/src/app/build/libs/*.jar /usr/src/app/*.jar
ENTRYPOINT ["java", "-jar", "/usr/src/app/*.jar"]
