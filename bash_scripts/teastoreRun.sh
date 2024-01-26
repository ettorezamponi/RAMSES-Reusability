#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

##### Network #####
docker network rm teastore
PrintWarn "-- Network Removed --"
PrintSuccess "Creating new Docker network called 'teastore'"
docker network create teastore
echo

PrintSuccess "Setting up TeaStore Registry"
docker pull descartesresearch/teastore-registry
docker run --name registry -p 10000:8080 --network teastore -d descartesresearch/teastore-registry
sleep 2

PrintSuccess "Setting up TeaStore DB"
docker pull descartesresearch/teastore-db
docker run --name db -p 3306:3306 --network teastore -d descartesresearch/teastore-db
sleep 2

PrintSuccess "Setting up TeaStore Persistence"
docker pull descartesresearch/teastore-persistence
docker run --name persistence -e "REGISTRY_HOST=registry" -e "HOST_NAME=persistence" -e "DB_HOST=db" -e "DB_PORT=3306" -p 1111:8080 --network teastore -d descartesresearch/teastore-persistence
sleep 2

PrintSuccess "Setting up TeaStore Store"
docker pull descartesresearch/teastore-auth
docker run --name auth -e "REGISTRY_HOST=registry" -e "HOST_NAME=auth" -p 2222:8080 --network teastore -d descartesresearch/teastore-auth
sleep 2

PrintSuccess "Setting up TeaStore Recommender"
docker pull descartesresearch/teastore-recommender
docker run --name recommender -e "REGISTRY_HOST=registry" -e "HOST_NAME=recommender" -p 3333:8080 --network teastore -d descartesresearch/teastore-recommender
sleep 2

PrintSuccess "Setting up TeaStore Image Provider"
docker pull descartesresearch/teastore-image
docker run --name image -e "REGISTRY_HOST=registry" -e "HOST_NAME=image" -p 4444:8080 --network teastore -d descartesresearch/teastore-image
sleep 2

PrintSuccess "Setting up TeaStore WebUI"
docker pull descartesresearch/teastore-webui
docker run --name webui -e "REGISTRY_HOST=registry" -e "HOST_NAME=webui" -p 8080:8080 --network teastore -d descartesresearch/teastore-webui

PrintSuccess "DONE"


