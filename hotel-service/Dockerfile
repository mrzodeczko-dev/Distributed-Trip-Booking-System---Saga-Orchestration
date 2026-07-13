# -----------------------------------------------------------------------------------------------
# STAGE 1: BUILD
# -----------------------------------------------------------------------------------------------
FROM maven:3.9.11-eclipse-temurin-25-alpine AS build
WORKDIR /build

COPY pom.xml .
COPY saga-common saga-common
COPY hotel-service hotel-service

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl saga-common,hotel-service clean package -DskipTests --no-transfer-progress

RUN java -Djarmode=tools -jar hotel-service/target/hotel-service-0.0.1.jar extract --destination target/extracted

# -----------------------------------------------------------------------------------------------
# STAGE 2: RUNTIME
# -----------------------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

LABEL org.opencontainers.image.title="Hotel Service (Saga Participant)"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

COPY --from=build --chown=appuser:appgroup /build/target/extracted/lib/   ./lib/
COPY --from=build --chown=appuser:appgroup /build/target/extracted/*.jar  ./

USER appuser

ENV SERVER_PORT=8082
EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar *.jar"]
