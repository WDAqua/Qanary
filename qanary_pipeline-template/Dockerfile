FROM openjdk:17

ENTRYPOINT ["java", "--add-opens", "java.base/java.io=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/usr/share/qanary-question-answering-system/my-qanary-qa-system.jar"]

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/qanary-question-answering-system/my-qanary-qa-system.jar
