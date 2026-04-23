FROM eclipse-temurin:21-jre

WORKDIR /app

COPY smartdoc-flow-service/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
