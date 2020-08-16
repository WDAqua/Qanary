FROM openjdk:11

ENTRYPOINT ["java", "-jar", "/usr/share/qanary-question-answering-system/my-qanary-qa-system.jar"]

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/qanary-question-answering-system/my-qanary-qa-system.jar
