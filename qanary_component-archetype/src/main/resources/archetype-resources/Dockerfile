
# Java 21 runtime (the 'openjdk' image is deprecated; use eclipse-temurin)
FROM eclipse-temurin:21-jdk

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /qanary-service.jar

ENTRYPOINT ["java", "-jar", "/qanary-service.jar"]
