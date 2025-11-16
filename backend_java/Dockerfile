# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

ENV MAVEN_CONFIG=""

COPY pom.xml .
COPY .mvn ./.mvn
COPY mvnw .
COPY .mvn/wrapper/maven-wrapper.jar ./.mvn/wrapper/maven-wrapper.jar
COPY src ./src
RUN ./mvnw -q -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV PORT=4002
EXPOSE 4002

COPY --from=build /workspace/target/micro-chat-translation-backend-1.0.0.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
