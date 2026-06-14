# Runtime-only image: the prebuilt Spring Boot fat jar runs on a JRE.
# Java lives only inside this container — the host needs no JDK/JRE.
FROM eclipse-temurin:25-jre

WORKDIR /app
COPY backend-local.jar /app/backend-local.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/backend-local.jar"]
