FROM java:8
VOLUME /tmp
ADD qa.qanary_component-speech-recognition-0.0.1.jar app.jar
RUN sh -c 'touch /app.jar'
ENTRYPOINT ["java", "-server", "-Xms256M", "-Xmx512M", "-XX:MaxDirectMemorySize=256M", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
