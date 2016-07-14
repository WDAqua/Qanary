#!/usr/bin/env bash
# Comment/uncomment the following code lines according to your needs

# Stop all containers
docker stop $(docker ps -a -q)

# Delete all containers
docker rm $(docker ps -a -q)
# Delete all images
# docker rmi $(docker images -q)

# Start stardog container
echo "Starting stardog container"
docker run -itd -v /data/qanary:/stardog-4.1.1/qanary -p 5820:4000 --net="host" --name stardog qanary/stardog

# Start qapipeline container
echo "Starting qapipeline container"
docker run -itd -p 8080:5000 --net="host" --name qapipeline qanary/qapipeline

# Start containers for qanary components
echo "Starting agdistis-ned component container"
docker run -d -P --net="host" --name agdistis-ned -t qanary/agdistis-ned

# echo "Starting alchemy-nerd component container"
# docker run -d -P --net="host" --name alchemy-nerd -t qanary/alchemy-nerd

# echo "Starting dbpedia-spotlight-ned component container"
# docker run -d -P --net="host" --name dbpedia-spotlight-ned -t qanary/dbpedia-spotlight-ned

# echo "Starting dbpedia-spotlight-ner component container"
# docker run -d -P --net="host" --name dbpedia-spotlight-ner -t qanary/dbpedia-spotlight-ner

# echo "Starting fox-ner component container"
# docker run -d -P --net="host" --name fox-ner -t qanary/fox-ner

# echo "Starting lucene-linker-nerd component container"
# docker run -d -P --net="host" --name lucene-linker-nerd -t qanary/lucene-linker-nerd

# echo "Starting stanford-ner component container"
# docker run -d -P --net="host" --name stanford-ner -t qanary/stanford-ner
