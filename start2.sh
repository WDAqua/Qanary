PARAMETERS="-Xms256M -Xmx512M -XX:MaxDirectMemorySize=256M -Djava.security.egd=file:/dev/./urandom -Dhttp.proxySet=true -Dhttp.proxyHost=cache.univ-st-etienne.fr -Dhttp.proxyPort=3128 -Dhttp.nonProxyHosts=qanaryhost|qa"
killall -9 java
rm /home/dd77474h/stardog-4.1.1/system.lock
/home/dd77474h/stardog-4.1.1/bin/stardog-admin server start
java $PARAMETERS -jar qanary_pipeline-template/target/qa.pipeline-0.1.0.jar &> log/pipline &
java $PARAMETERS -jar qanary_component-Alchemy-NERD/target/qa.Alchemy-NERD-0.1.0.jar &>log/alchemy-NERD &
java $PARAMETERS -jar qanary_component-AGDISTIS-NED/target/qa.AGDISTIS-NED-0.1.0.jar &>log/agdistis-NED &
java $PARAMETERS -jar qanary_component-DBpedia-Spotlight-NER/target/qa.DBpedia-Spotlight-NER-0.1.0.jar &> log/spotlight-NER &
java $PARAMETERS -jar qanary_component-DBpedia-Spotlight-NED/target/qa.DBpedia-Spotlight-NED-0.1.0.jar &> log/spotlight-NED &
java $PARAMETERS -jar qanary_component-FOX-NER/target/qa.FOX-NER-0.1.0.jar &> log/fox-NER &
java -jar qanary_component-stanford-NER/target/qa.StandfordNER-0.1.0.jar &> log/stanford-NER &
