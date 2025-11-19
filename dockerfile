# 1) Imagem de build
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY . .
RUN mvn -q clean package -DskipTests


# 2) Imagem final
FROM eclipse-temurin:17-jdk

WORKDIR /app

# copia o JAR correto
COPY --from=build /app/target/*.jar app.jar

# Render ignora EXPOSE, mas padronizamos
EXPOSE 8080

# Render fornece a porta automaticamente
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
