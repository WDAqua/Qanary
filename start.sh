#!/usr/bin/env bash
# Stop all containers
docker stop $(docker ps -a -q)

# Delete all containers
docker rm $(docker ps -a -q)
# Delete all images
# docker rmi $(docker images -q)

# Start stardog container
echo "Starting stardog container"
docker run -itd -v /home/ilytra/data/qanary:/stardog-4.1.1/qanary -p 5820:4000 --net="host" --name stardog stardog

# Start qapipeline container
echo "Starting qapipeline container"
docker run -itd -p 8080:5000 --net="host" --name qapipeline qanary/qapipeline

# Start containers for all qanary components
echo "Starting agdistis-ned component container"
docker run -d -P --net="host" --name agdistis-ned -t qanary/qa.AGDISTIS-NED

echo "Starting alchemy-nerd component container"
docker run -d -P --net="host" --name alchemy-nerd -t qanary/qa.Alchemy-NERD

echo "Starting dbpedia-spotlight-ned component container"
docker run -d -P --net="host" --name dbpedia-spotlight-ned -t qanary/qa.DBpedia-Spotlight-NED

echo "Starting dbpedia-spotlight-ner component container"
docker run -d -P --net="host" --name dbpedia-spotlight-ner -t qanary/qa.DBpedia-Spotlight-NER

echo "Starting fox-ner component container"
docker run -d -P --net="host" --name fox-ner -t qanary/qa.FOX-NER

echo "Starting lucene-linker-nerd component container"
docker run -d -P --net="host" --name lucene-linker-nerd -t qanary/qa.Lucene-Linker-NERD

echo "Starting stanford-ner component container"
docker run -d -P --net="host" --name stanford-ner -t qanary/qa.StanfordNER