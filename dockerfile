# ==========================================================
# 1) IMAGEM DE BUILD — MAVEN + JAVA 21
# ==========================================================
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY . .

RUN mvn -q clean package -DskipTests

# ==========================================================
# 2) IMAGEM FINAL — JRE 21 SLIM
# ==========================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# --enable-native-access=ALL-UNNAMED suprime o warning do sqlite-jdbc no Java 21+
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
