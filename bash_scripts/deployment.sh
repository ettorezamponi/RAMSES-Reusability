#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

# MODIFY THESE TWO VARIABLES TO BE ABLE TO MANAGE THE CONFIG SERVER
export GITHUB_REPOSITORY_URL=https://github.com/ettorezamponi/config-server.git
export GITHUB_OAUTH=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

##### Network #####
docker network rm teastore
PrintSuccess "-- Network Removed --"
PrintSuccess "Creating new Docker network called 'teastore'"
docker network create teastore
echo

#DISCOVERY SERVER
docker run -P --name sefa-eureka -d --network teastore sefa-eureka
sleep 5

#MOVIE INFO SERVICE
docker run -P --name movie-service -d --network teastore movie
sleep 2

#REGISTRY TEASTORE
docker run -P --name registry -d --network teastore registry
sleep 2