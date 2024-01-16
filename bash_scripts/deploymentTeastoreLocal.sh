#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

# MODIFY THESE TWO VARIABLES TO BE ABLE TO MANAGE THE CONFIG SERVER
export GITHUB_REPOSITORY_URL=https://github.com/ettorezamponi/config-server.git
export GITHUB_OAUTH=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

##### Network #####
docker network rm teastore
PrintWarn "-- Network Removed --"
PrintSuccess "Creating new Docker network called 'teastore'"
docker network create teastore
echo

#DISCOVERY SERVER
docker run -P --name sefa-eureka -d --network teastore eureka

#REGISTRY
docker run --name registry -p 1000:8080 --network teastore -d teastore-registry

#DB
docker run --name db -p 3306:3306 --network teastore -d teastore-db
docker run -P --name knowledge-db --network teastore -d knowledge-db

#PERSISTENCE
docker run --name persistence -e "REGISTRY_HOST=registry" -e "HOST_NAME=persistence" -e "DB_HOST=db" -e "DB_PORT=3306" -p 1111:8080 --network teastore -d teastore-persistence

#STORE/AUTH
docker run --name auth -e "REGISTRY_HOST=registry" -e "HOST_NAME=auth" -p 2222:8080 --network teastore -d teastore-auth

#RECOMMENDER
docker run --name recommender -e "REGISTRY_HOST=registry" -e "HOST_NAME=recommender" -p 3333:8080 --network teastore -d teastore-recommender

#IMAGE
docker run --name image -e "REGISTRY_HOST=registry" -e "HOST_NAME=image" -p 4444:8080 --network teastore -d teastore-image

#WEBUI
docker run --name webui -e "REGISTRY_HOST=registry" -e "HOST_NAME=webui" -p 8080:8080 --network teastore -d teastore-webui

sleep 70

docker run -P --name teastore-probe -d --network teastore probe
docker run -p 8081:8081 --name maven-configserver -d --network teastore configserver

sleep 50

docker run -P --name ramses-knowledge -d --network teastore knowledge

PrintSuccess "EVERYTHING SET UP!"


# this example does not configure the PROXY_NAME and PROXY_PORT variables for the WebUI,
# as it assumes that no front-end load balancer is used. If one is used, both of these variables may be configured for the WebUI.
