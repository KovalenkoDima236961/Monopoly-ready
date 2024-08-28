FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/CatanBackend-0.0.1-SNAPSHOT.jar /app/CatanBackend-0.0.1-SNAPSHOT.jar

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/CatanBackend-0.0.1-SNAPSHOT.jar"]