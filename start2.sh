PARAMETERS="-Xms256M -Xmx715M -XX:MaxDirectMemorySize=256M -Djava.security.egd=file:/dev/./urandom -Dhttp.proxySet=true -Dhttp.proxyHost=cache.univ-st-etienne.fr -Dhttp.proxyPort=3128 -Dhttp.nonProxyHosts=qanaryhost|qa|wikidatalog|speech"
killall -9 java
rm /home/services/stardog-4.1.1/system.lock
/home/services/stardog-4.1.1/bin/stardog-admin server start --disable-security &>> log/stardog
java $PARAMETERS -jar qanary_pipeline-template/target/qa.pipeline-1.1.0.jar &>> log/pipline &
java $PARAMETERS -jar qanary_component-NERD-Alchemy/target/qa.NERD-Alchemy-1.0.0.jar &>> log/NERD-Alchemy &
java $PARAMETERS -jar qanary_component-NED-AGDISTIS/target/qa.NED-AGDISTIS-1.0.0.jar &>> log/NED-Agdistis &
java $PARAMETERS -jar qanary_component-NER-DBpedia-Spotlight/target/qa.NER-DBpedia-Spotlight-1.0.0.jar &>> log/NER-spotlight &
java $PARAMETERS -jar qanary_component-NED-DBpedia-Spotlight/target/qa.NED-DBpedia-Spotlight-1.0.0.jar &>> log/spotlight-NED &
java $PARAMETERS -jar qanary_component-NER-FOX/target/qa.NER-FOX-1.0.0.jar &>> log/NER-FOX &
java $PARAMETERS -jar qanary_component-NER-stanford/target/qa.NER-Standford-1.0.0.jar &>> log/NER-stanford &
java $PARAMETERS -jar qanary_component-LD-Shuyo/target/qa.LD-Shuyo-0.0.1.jar &>> log/LD-Shuyo &
