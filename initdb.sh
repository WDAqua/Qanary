#!/usr/bin/env bash

# Create qanary database at startup
echo "Creating qanary triple store"
docker exec -it stardog /bin/bash -c "./bin/stardog-admin db create -n qanary; exit"