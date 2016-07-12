FROM java:8

ENV STARDOG_VER=4.1.1

WORKDIR /

ADD resources/stardog-${STARDOG_VER}.zip /
RUN unzip stardog-${STARDOG_VER}.zip && rm stardog-${STARDOG_VER}.zip
ADD resources/stardog-license-key.bin /stardog-${STARDOG_VER}/

WORKDIR /stardog-${STARDOG_VER}

CMD rm /stardog/system.lock || true && \
    ./bin/stardog-admin server start && \
    sleep 1 && \
    (tail -f ${STARDOG_HOME}/stardog.log &) && \
<<<<<<< HEAD
while (pidof java > /dev/null); do sleep 1; done
=======
    while (pidof java > /dev/null); do sleep 1; done
>>>>>>> 594ce800400bd4639e55387b80e7a42b0f9beea4
