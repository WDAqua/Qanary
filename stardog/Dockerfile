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
    while (pidof java > /dev/null); do sleep 1; done
