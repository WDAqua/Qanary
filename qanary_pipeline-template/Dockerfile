FROM openjdk:8-jre

ENTRYPOINT ["/usr/local/openjdk-8/bin/java", "-jar", "/usr/share/qanary-question-answering-system/my-qanary-qa-system.jar"]

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/qanary-question-answering-system/my-qanary-qa-system.jar
