#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

# MODIFY THESE TWO VARIABLES TO BE ABLE TO MANAGE THE CONFIG REPO
export GITHUB_REPOSITORY_URL=...
export GITHUB_OAUTH=...

PrintWarn "Please enter the architecture on which you are going to run the system ('arm64' or 'amd64'):"
read ARCH

if [ "$ARCH" == "arm64" ]; then
    echo "Running script with selected architecture $ARCH."
elif [ "$ARCH" == "amd64" ]; then
    echo "Running script with selected architecture $ARCH."
else
    echo "Architecture not supported."
    exit 1
fi
sleep 1


if [[("${GITHUB_OAUTH}" = "") || ("${GITHUB_REPOSITORY_URL}" = "")]]; then
  PrintError "Env var GITHUB_OAUTH and GITHUB_REPOSITORY_URL must be set!"
  exit 1
fi

##### Network #####
docker network rm ramses-sas-net
PrintSuccess "-- Network Removed --"
PrintSuccess "Creating new Docker network called 'ramses-sas-net'"
docker network create ramses-sas-net
echo

##### MYSQL #####
PrintSuccess "Setting up MySQL Server"
docker pull giamburrasca/mysql:$ARCH
docker run -P --name mysql -d --network ramses-sas-net giamburrasca/mysql:$ARCH
echo
sleep 2

##### SEFA #####
echo; PrintSuccess "Setting up the Managed System, SEFA!"; echo

PrintSuccess "Setting up Netflix Eureka Server"
docker pull giamburrasca/sefa-eureka:$ARCH
docker run -P --name sefa-eureka -d --network ramses-sas-net giamburrasca/sefa-eureka:$ARCH
echo
sleep 2

PrintSuccess "Setting up Spring Config Server"
docker pull giamburrasca/sefa-configserver:$ARCH
docker run -P --name sefa-configserver -e GITHUB_REPOSITORY_URL=$GITHUB_REPOSITORY_URL -d --network ramses-sas-net giamburrasca/sefa-configserver:$ARCH
echo
sleep 15 #time for configurations to be pulled

declare -a arr=("sefa-restaurant-service"
                "sefa-ordering-service"
                "sefa-payment-proxy-1-service"
                "sefa-delivery-proxy-1-service"
		            "sefa-web-service"
                "sefa-api-gateway"
                )
for i in "${arr[@]}"
do
   PrintSuccess "Setting up $i"
   docker pull giamburrasca/$i:$ARCH
   docker run -P --name $i -d --network ramses-sas-net giamburrasca/$i:$ARCH
   echo
   sleep 1
done

declare -a extra=("sefa-payment-proxy-2-service"
                  "sefa-delivery-proxy-2-service"
                  "sefa-payment-proxy-3-service"
                  "sefa-delivery-proxy-3-service"
                  )
for i in "${extra[@]}"
do
   PrintSuccess "Pulling $i"
   docker pull giamburrasca/$i:$ARCH
   echo
   sleep 1
done

##### PROBE AND ACTUATORS #####
echo; PrintSuccess "Setting up probe and actuators!"; echo


PrintSuccess "Pulling sefa-probe"
docker pull giamburrasca/sefa-probe:$ARCH
docker run -P --name sefa-probe -d --network ramses-sas-net giamburrasca/sefa-probe:$ARCH
echo
sleep 1

# https://www.cloudbees.com/blog/using-the-add-host-flag-for-dns-mapping-within-docker-containers
PrintSuccess "Pulling sefa-instances-manager"
docker pull giamburrasca/sefa-instances-manager:$ARCH
docker run -P --name sefa-instances-manager -d --network ramses-sas-net --add-host=host.docker.internal:host-gateway giamburrasca/sefa-instances-manager:$ARCH
echo
sleep 1

PrintSuccess "Pulling sefa-config-manager"
docker pull giamburrasca/sefa-config-manager:$ARCH
docker run -P --name sefa-config-manager -e GITHUB_OAUTH=$GITHUB_OAUTH -e GITHUB_REPOSITORY_URL=$GITHUB_REPOSITORY_URL -d --network ramses-sas-net giamburrasca/sefa-config-manager:$ARCH
echo

##### RAMSES #####
echo; PrintSuccess "Setting up the Managing System, RAMSES!"; echo
sleep 15

docker pull giamburrasca/ramses-knowledge:$ARCH
docker run -P --name ramses-knowledge -d --network ramses-sas-net giamburrasca/ramses-knowledge:$ARCH
sleep 10

declare -a ramsesarr=("ramses-analyse" "ramses-execute" "ramses-monitor" "ramses-dashboard")
for i in "${ramsesarr[@]}"
do
   PrintSuccess "Pulling $i"
   docker pull giamburrasca/$i:$ARCH
   docker run -P --name $i -d --network ramses-sas-net giamburrasca/$i:$ARCH
   echo
   sleep 3
done

PrintSuccess "Pulling ramses-plan"
sleep 2
docker pull giamburrasca/ramses-plan:arm64
docker run -P --name ramses-plan -d --network ramses-sas-net giamburrasca/ramses-plan:arm64


echo; PrintSuccess "DONE!"; echo

sleep 3

echo; PrintWarn "Do you wanna simulate a scenario ?!"; echo
echo; PrintWarn "1:addIstance \ 2:changeImplementation \ 3:changeLBWeights \ 4:shutdownInstance"; echo


echo "Insert the number of the scenario you want or press "RETURN" to exit:"
read userInput

if [[ $userInput =~ ^[1-4]$ ]]; then
    NUMBER=$userInput
    echo "You choose to simulate the scenario $NUMBER."
    docker pull giamburrasca/scenario$NUMBER:$ARCH
    docker run -P --name simulation-scenario-$NUMBER -d --network ramses-sas-net giamburrasca/scenario$NUMBER:$ARCH
    echo "Simulation started."
else
    echo "Enjoy RAMSES!"
fi
