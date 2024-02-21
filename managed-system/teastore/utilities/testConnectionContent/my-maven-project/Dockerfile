# Usa un'immagine di base con una versione di Java compatibile con il tuo progetto Maven
FROM openjdk:11

# Copia il file .jar del tuo progetto nella directory /app
COPY target/my-maven-project-1.0-SNAPSHOT.jar /app/configserver.jar

# Imposta la directory di lavoro all'interno del contenitore
WORKDIR /app

# Esponi la porta su cui il tuo server sarà in ascolto
EXPOSE 8081

# Comando per avviare l'applicazione quando il contenitore viene eseguito
CMD ["java", "-jar", "configserver.jar"]
