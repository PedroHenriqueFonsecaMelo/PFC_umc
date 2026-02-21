# ==========================================================
# 1) IMAGEM DE BUILD — MAVEN
# ==========================================================
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY . .

RUN mvn -q clean package -DskipTests


# ==========================================================
# 2) IMAGEM FINAL — SOMENTE O JAR
# ==========================================================
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
