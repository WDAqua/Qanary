FROM java:8

# Install maven
RUN apt-get update && apt-get -y install maven

# Install smaph dependencies
RUN git clone https://github.com/diegoceccarelli/hpc-utils
WORKDIR /hpc-utils
RUN mvn install -DskipTests

# Install smaph
WORKDIR /
RUN git clone http://github.com/marcocor/smaph-erd

# Run smaph
WORKDIR /smaph-erd
ADD smaph-config.xml smaph-config.xml
ENTRYPOINT ["mvn", "-Djetty.port=9090", "jetty:run"]

#VOLUME /tmp
#ADD qa.smaph-erd-0.0.1.jar app.jar
#RUN sh -c 'touch /app.jar'
#ENTRYPOINT ["java", "-server", "-Xms256M", "-Xmx512M", "-XX:MaxDirectMemorySize=256M", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]