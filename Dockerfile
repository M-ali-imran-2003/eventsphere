# ==========================================
# STAGE 1: Build the Application
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy the pom.xml first to cache the dependencies
COPY pom.xml .

# FIX: Use dependency:resolve-plugins to reliably cache everything
RUN mvn dependency:resolve-plugins dependency:resolve -B

# Copy the actual source code
COPY src ./src

# FIX: Remove 'clean' so Maven doesn't wipe out the cached plugins you just downloaded
RUN mvn package -DskipTests

# ==========================================
# STAGE 2: Run the Application
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the compiled .jar file from the 'build' stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
