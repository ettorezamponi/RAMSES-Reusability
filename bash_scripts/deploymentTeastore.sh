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

# DISCOVERY SERVER
#docker run -P --name eureka -d --network teastore eureka

# REGISTRY
docker run --name registry -e "HOST_NAME=registry" -e "SERVICE_PORT=10000" -p 10000:8080 --network teastore -d descartesresearch/teastore-registry
sleep 5

#DB
docker run --name db -p 3306:3306 --network teastore -d descartesresearch/teastore-db
sleep 2

# PERSISTENCE
docker run --name persistence -e "REGISTRY_HOST=registry" -e "REGISTRY_PORT=10000" -e "HOST_NAME=persistence" -e "SERVICE_PORT=1111" -e "DB_HOST=db" -e "DB_PORT=3306" -p 1111:8080 --network teastore -d descartesresearch/teastore-persistence
sleep 2

#STORE/AUTH
docker run --name auth -e "REGISTRY_HOST=registry" -e "REGISTRY_PORT=10000" -e "HOST_NAME=auth" -e "SERVICE_PORT=2222" -p 2222:8080 --network teastore -d descartesresearch/teastore-auth
sleep 2

#RECOMMENDER
docker run --name recommender -e "REGISTRY_HOST=registry" -e "REGISTRY_PORT=10000" -e "HOST_NAME=recommender" -e "SERVICE_PORT=3333" -p 3333:8080 --network teastore -d descartesresearch/teastore-recommender
sleep 2

#IMAGE
docker run --name image -e "REGISTRY_HOST=registry" -e "REGISTRY_PORT=10000" -e "HOST_NAME=image" -e "SERVICE_PORT=4444" -p 4444:8080 --network teastore -d descartesresearch/teastore-image
sleep 2

#WEBUI
docker run --name webui -e "REGISTRY_HOST=registry" -e "REGISTRY_PORT=10000" -e "HOST_NAME=webui" -e "SERVICE_PORT=8080" -p 8080:8080 --network teastore -d descartesresearch/teastore-webui
sleep 5

PrintSuccess "All Teastore services are UP!"


# this example does not configure the PROXY_NAME and PROXY_PORT variables for the WebUI,
# as it assumes that no front-end load balancer is used. If one is used, both of these variables may be configured for the WebUI.
