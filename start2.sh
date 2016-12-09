PARAMETERS="-Xms256M -Xmx512M -XX:MaxDirectMemorySize=256M -Djava.security.egd=file:/dev/./urandom -Dhttp.proxySet=true -Dhttp.proxyHost=cache.univ-st-etienne.fr -Dhttp.proxyPort=3128 -Dhttp.nonProxyHosts=qanaryhost|qa|wikidatalog|speech"
killall -9 java
rm /home/services/stardog-4.1.1/system.lock
/home/services/stardog-4.1.1/bin/stardog-admin server start
java $PARAMETERS -jar qanary_pipeline-template/target/qa.pipeline-1.0.0.jar &>> log/pipline &
java $PARAMETERS -jar qanary_component-Alchemy-NERD/target/qa.Alchemy-NERD-1.0.0.jar &>> log/alchemy-NERD &
java $PARAMETERS -jar qanary_component-AGDISTIS-NED/target/qa.AGDISTIS-NED-1.0.0.jar &>> log/agdistis-NED &
java $PARAMETERS -jar qanary_component-DBpedia-Spotlight-NER/target/qa.DBpedia-Spotlight-NER-1.0.0.jar &>> log/spotlight-NER &
java $PARAMETERS -jar qanary_component-DBpedia-Spotlight-NED/target/qa.DBpedia-Spotlight-NED-1.0.0.jar &>> log/spotlight-NED &
java $PARAMETERS -jar qanary_component-FOX-NER/target/qa.FOX-NER-1.0.0.jar &>> log/fox-NER &
java $PARAMETERS -jar qanary_component-stanford-NER/target/qa.StandfordNER-1.0.0.jar &>> log/stanford-NER &
java $PARAMETERS -jar qanary_component-language-detection/target/qa.language-detection-0.0.1.jar &>> log/language-detection &
