# Usa un'immagine di base con Java
FROM openjdk:11-jre-slim

# Crea una directory per l'app
WORKDIR /app

# Copia i file necessari
COPY target/*.jar /app/configserver.jar

# Comando di avvio dell'app
CMD ["java", "-jar", "configserver.jar"]
