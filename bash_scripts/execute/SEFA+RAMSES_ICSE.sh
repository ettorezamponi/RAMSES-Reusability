#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

export GITHUB_REPOSITORY_URL=https://github.com/ettorezamponi/config-server.git
export GITHUB_OAUTH=


if [[(${ARCH} != "arm64") && ( ${ARCH} != "amd64")]]; then
  PrintWarn "Desired architecture not specified or unknown. Supported values are 'arm64' and 'amd64'. Using 'arm64' as default option"
  ARCH="arm64"
else
   PrintSuccess "Running script with selceted architecture: ${ARCH}"
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
sleep 8

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

declare -a pract=("sefa-probe" "sefa-instances-manager")
for i in "${pract[@]}"
do
   PrintSuccess "Pulling $i"
   docker pull giamburrasca/$i:$ARCH
   docker run -P --name $i -d --network ramses-sas-net giamburrasca/$i:$ARCH
   echo
   sleep 1
done

PrintSuccess "Pulling sefa-config-manager"
docker pull giamburrasca/sefa-config-manager:$ARCH
docker run -P --name sefa-config-manager -e GITHUB_OAUTH=$GITHUB_OAUTH -e GITHUB_REPOSITORY_URL=$GITHUB_REPOSITORY_URL -d --network ramses-sas-net giamburrasca/sefa-config-manager:$ARCH
echo

##### RAMSES #####
echo; PrintSuccess "Setting up the Managing System, RAMSES!"; echo
sleep 20

docker run -P --name ramses-knowledge -d --network ramses-sas-net ramses-knowledge
sleep 10

declare -a ramsesarr=("ramses-analyse" "ramses-plan" "ramses-execute" "ramses-monitor" "ramses-dashboard")
for i in "${ramsesarr[@]}"
do
   PrintSuccess "Pulling $i"
   #docker pull giamburrasca/$i:v1
   docker run -P --name $i -d --network ramses-sas-net $i
   echo
   sleep 2
done


echo; PrintSuccess "DONE!"; echo


#echo; PrintWarn "Add instances scenario"; echo
# payment after 90 seconds, ordering after 9 minutes
# docker pull giamburrasca/simulation-scenario1:arm64
# docker run -P --name simulation-scenario-1 -d --network ramses-sas-net giamburrasca/simulation-scenario1:arm64

#echo; PrintWarn "Change implementation scenario"; echo
# change implementation
# giamburrasca/ramses-knowledge-scenario2:arm64
# docker run -P --name simulation-scenario-2 -d --network ramses-sas-net giamburrasca/simulation-scenario2:arm64

#scenario 3
# da aggiustare perchè il 3 è stato sovrascritto dal seguente scenario senza cambiare nome
# giamburrasca/ramses-knowledge-scenario3:arm64
# giamburrasca/simulation-scenario3:arm64

#scenario 4
# giamburrasca/ramses-knowledge-scenario4:arm64

#"service_id":"ORDERING-SERVICE",
#			"implementations" : [
#				{
#					"implementation_id" : "ordering-service",
#					"implementation_trust" : 1,
#					"preference" : 1,
#					"instance_load_shutdown_threshold" : 0.5
#				}

