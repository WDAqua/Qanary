FROM openjdk:8-jre

ENTRYPOINT ["/usr/local/openjdk-8/bin/java", "-jar", "/usr/share/qanary-components/qanary-service.jar"]

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/qanary-components/qanary-service.jar
