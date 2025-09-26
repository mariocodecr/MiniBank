FROM openjdk:21-jdk-slim

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle

COPY src src

RUN ./gradlew build -x test

RUN addgroup --system spring && adduser --system --group spring
USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

CMD ["java", "-jar", "build/libs/api-gateway-0.0.1-SNAPSHOT.jar"]