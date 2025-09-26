FROM openjdk:21-jdk-slim

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY settings.gradle ./

# Copy service specific files
COPY services/account-service/build.gradle services/account-service/
COPY services/account-service/src services/account-service/src

# Build only the account service
RUN echo "rootProject.name = 'account-service'" > settings.gradle && \
    echo "include 'account-service'" >> settings.gradle && \
    echo "project(':account-service').projectDir = file('services/account-service')" >> settings.gradle

RUN ./gradlew :account-service:build -x test --no-daemon

RUN addgroup --system spring && adduser --system --group spring
USER spring:spring

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

CMD ["java", "-jar", "services/account-service/build/libs/account-service-0.0.1-SNAPSHOT.jar"]