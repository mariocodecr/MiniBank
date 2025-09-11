# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.4.0 banking backend application (`minibank-backend`) using Java 21 and Gradle. The application uses PostgreSQL as the database with JPA/Hibernate for data persistence.

## Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Clean build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Run a single test class
./gradlew test --tests "com.minibank.minibank_backend.SpecificTestClass"

# Run tests in continuous mode
./gradlew test --continuous
```

### Database
```bash
# Start with PostgreSQL database (requires Docker)
# Note: Database configuration needed in application.properties
```

## Architecture

### Package Structure
- **Base package**: `com.minibank.minibank_backend`
- **Main application**: `MinibankBackendApplication.java` - Standard Spring Boot entry point
- **Resources**: Application configuration in `application.properties`

### Key Dependencies
- **Spring Boot Starters**: Web, Data JPA, Security, Validation, Actuator
- **Database**: PostgreSQL driver
- **Development**: Lombok for reducing boilerplate
- **Testing**: Spring Boot Test, Spring Security Test, JUnit 5

### Configuration Notes
- The original package name `com.minibank.minibank-backend` was invalid and was corrected to `com.minibank.minibank_backend`
- Application name is configured as `minibank-backend` in properties
- Uses Java 21 language features and toolchain

## Development Setup Requirements

1. **Java 21** - Required for compilation and runtime
2. **PostgreSQL** - Database server for persistence
3. **Gradle** - Build tool (wrapper included)

Database connection properties will need to be configured in `application.properties` for PostgreSQL connectivity.