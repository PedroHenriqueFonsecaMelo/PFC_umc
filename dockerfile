# 1) Imagem de build (Maven)
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copia o pom.xml e baixa dependências
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copia o projeto inteiro
COPY . .

# Compila e gera o WAR (ou JAR)
RUN mvn -q clean package -DskipTests


# 2) Imagem final
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copia o war gerado
COPY --from=build /app/target/*.war app.war

# O Render ignora EXPOSE, mas deixe 8080 por padrão
EXPOSE 8080

# O Render definirá a variável PORT automaticamente
ENTRYPOINT ["sh", "-c", "java -jar app.war --server.port=${PORT}"]
