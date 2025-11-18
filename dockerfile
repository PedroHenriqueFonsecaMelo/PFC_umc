# 1. Usa imagem base com JDK 17
FROM eclipse-temurin:17-jdk

# 2. Define o diretório de trabalho dentro do container
WORKDIR /app

# 3. Copia o arquivo WAR para dentro do container
COPY target/exs-0.0.1-SNAPSHOT.war app.war

# 4. Expõe a porta usada pela aplicação
EXPOSE 8443

# 5. Comando para iniciar a aplicação
CMD ["java", "-jar", "app.war"]
