#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

PrintSuccess "Setting up TeaStore Registry"
docker pull descartesresearch/teastore-registry
docker run --name teastore-registry -e "HOST_NAME=10.1.2.3" -e "SERVICE_PORT=10000" -p 10000:8080 -d descartesresearch/teastore-registry
sleep 2

PrintSuccess "Setting up TeaStore DB"
docker pull descartesresearch/teastore-db
docker run --name teastore-db -p 3306:3306 -d descartesresearch/teastore-db
sleep 2

PrintSuccess "Setting up TeaStore Persistence"
docker pull descartesresearch/teastore-persistence
docker run --name teastore-persistence -e "REGISTRY_HOST=10.1.2.3" -e "REGISTRY_PORT=10000" -e "HOST_NAME=10.1.2.30" -e "SERVICE_PORT=1111" -e "DB_HOST=10.1.2.20" -e "DB_PORT=3306" -p 1111:8080 -d descartesresearch/teastore-persistence
sleep 2

PrintSuccess "Setting up TeaStore Store"
docker pull descartesresearch/teastore-auth
docker run --name teastore-store -e "REGISTRY_HOST=10.1.2.3" -e "REGISTRY_PORT=10000" -e "HOST_NAME=10.1.2.30" -e "SERVICE_PORT=2222" -p 2222:8080 -d descartesresearch/teastore-auth
sleep 2

PrintSuccess "Setting up TeaStore Recommender"
docker pull descartesresearch/teastore-recommender
docker run --name teastore-recommender -e "REGISTRY_HOST=10.1.2.3" -e "REGISTRY_PORT=10000" -e "HOST_NAME=10.1.2.30" -e "SERVICE_PORT=3333" -p 3333:8080 -d descartesresearch/teastore-recommender
sleep 2

PrintSuccess "Setting up TeaStore Image Provider"
docker pull descartesresearch/teastore-image
docker run --name teastore-image -e "REGISTRY_HOST=10.1.2.3" -e "REGISTRY_PORT=10000" -e "HOST_NAME=10.1.2.30" -e "SERVICE_PORT=4444" -p 4444:8080 -d descartesresearch/teastore-image
sleep 2

PrintSuccess "Setting up TeaStore WebUI"
docker pull descartesresearch/teastore-webui
docker run --name teastore-webui -e "REGISTRY_HOST=10.1.2.3" -e "REGISTRY_PORT=10000" -e "HOST_NAME=10.1.2.30" -e "SERVICE_PORT=8080" -p 8080:8080 -d descartesresearch/teastore-webui

PrintSuccess "DONE"


