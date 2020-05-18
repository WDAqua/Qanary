
FROM openjdk:11                                                                                                                                                                                                                             

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /qanary-service.jar

ENTRYPOINT ["java", "-jar", "/qanary-service.jar"]
