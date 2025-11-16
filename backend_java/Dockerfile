# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn -q -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV PORT=4002
EXPOSE 4002

COPY --from=build /workspace/target/micro-chat-translation-backend-1.0.0.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
