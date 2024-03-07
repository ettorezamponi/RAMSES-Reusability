#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

# MODIFY THESE TWO VARIABLES TO BE ABLE TO MANAGE THE CONFIG SERVER
export GITHUB_REPOSITORY_URL=https://github.com/ettorezamponi/config-server.git
export GITHUB_OAUTH=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

# Set to 'Y' to simulate SCENARIO 1
injection="N"
time_injection=240

##### Network #####
docker network rm teastore
PrintWarn "-- Network Removed --"
PrintSuccess "Creating new Docker network called 'teastore'"
docker network create teastore
echo

sleep 2
PrintSuccess "Setting up Teastore"
sleep 1

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
docker run --name auth -e "REGISTRY_HOST=registry" -e "HOST_NAME=auth" -p 2222:8080 --network teastore -d teastore-auth-fault

#RECOMMENDER
docker run --name recommender -e "REGISTRY_HOST=registry" -e "HOST_NAME=recommender" -p 3333:8080 --network teastore -d teastore-recommender

#IMAGE
docker run --name image -e "REGISTRY_HOST=registry" -e "HOST_NAME=image" -p 4444:8080 --network teastore -d teastore-image-fault

#WEBUI
docker run --name webui -e "REGISTRY_HOST=registry" -e "HOST_NAME=webui" -p 8080:8080 --network teastore -d teastore-webui

durata_timer=60

while [ $durata_timer -gt 0 ]; do
    # Stampa il tempo rimanente
    printf "\rRemaining timer for set up RAMSES: %02d:%02d" $((durata_timer/60)) $((durata_timer%60))

    # Attendi 1 secondo
    sleep 1

    # Riduci il tempo rimanente di 1 secondo
    durata_timer=$((durata_timer-1))
done

# Alla fine del timer, vai a capo per migliorare la leggibilità
echo

docker run -P --name teastore-probe -d --network teastore probe
docker run -P --name teastore-instances-manager -d --network teastore instances-manager
docker run -P --name teastore-config-manager -d --network teastore actuator-configmanager
docker run -p 8081:8081 --name maven-config-server -d --network teastore configserver

durata_timer=50

while [ $durata_timer -gt 0 ]; do
    # Stampa il tempo rimanente
    printf "\rRemaining timer for MAPE-K loop: %02d:%02d" $((durata_timer/60)) $((durata_timer%60))

    # Attendi 1 secondo
    sleep 1

    # Riduci il tempo rimanente di 1 secondo
    durata_timer=$((durata_timer-1))
done
echo

docker run -P --name ramses-knowledge -d --network teastore ramses-knowledge

sleep 15

declare -a ramsesarr=("ramses-analyse" "ramses-plan" "ramses-execute" "ramses-monitor" "ramses-dashboard")
for i in "${ramsesarr[@]}"
do
   PrintSuccess "Running $i"
   docker run -P --name $i -d --network teastore $i
   echo
   sleep 2
done

PrintSuccess "EVERYTHING SET UP!"


# this example does not configure the PROXY_NAME and PROXY_PORT variables for the WebUI,
# as it assumes that no front-end load balancer is used. If one is used, both of these variables may be configured for the WebUI.


if [[ $injection == "Y" ]]; then
    echo "INJECTION STARTED"
    sleep $time_injection
    CONTAINER_ID=$(docker ps -qf "name=recommender")
    docker stop $CONTAINER_ID
    PrintSuccess "CONTAINER STOPPED!"

    sleep $time_injection
    CONTAINER_ID=$(docker ps -qf "name=persistence")
    docker stop $CONTAINER_ID
    PrintSuccess "CONTAINER STOPPED!"
fi