# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

# Install necessary dependencies for building
RUN apk add --no-cache bash grep sed

# Set working directory
WORKDIR /app

# Set Gradle options to limit memory usage and improve performance
ENV GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx2g -Dorg.gradle.daemon=false -Dorg.gradle.parallel=true"

# Copy only the gradle files first to leverage Docker cache
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./

# Download dependencies
RUN ./gradlew --no-daemon dependencies

# Copy the rest of the project files
COPY . /app

# Clean any previous builds and build the fat JAR
RUN ./gradlew clean fatJar --no-daemon && \
    rm -rf /root/.gradle/wrapper/dists/* && \
    rm -rf /root/.gradle/caches/modules-2/files-2.1

# Get the version from the build.gradle.kts file
RUN VERSION=$(./gradlew properties -q | grep "version:" | awk '{print $2}') && \
    # Create a symbolic link to make the JAR easier to reference
    ln -s /app/build/libs/kanon-$VERSION-fat.jar /app/kanon.jar

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/kanon.jar /app/kanon.jar

# Set environment variables
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

# Set the entrypoint to run the JAR with optimized JVM settings
ENTRYPOINT ["java", "-jar", "/app/kanon.jar"]
