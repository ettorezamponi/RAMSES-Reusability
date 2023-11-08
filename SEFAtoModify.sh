#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

export GITHUB_REPOSITORY_URL=https://github.com/ettorezamponi/config-server.git
export GITHUB_OAUTH=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

usage() {
  echo "Usage: [-a <arch>] [-l]"
  echo "-a <arch>: Desired architecture. Supported values are 'arm64' and 'amd64'. Default is 'arm64'"
  echo "-l: start only the load generator"
  exit 1
}

LOADGEN=false
while getopts a:l option
do
  case "${option}" in
    a) ARCH=${OPTARG};;
    l) LOADGEN=true;;
    *) usage;;
  esac
done

if [[(${ARCH} != "arm64") && ( ${ARCH} != "amd64")]]; then
  PrintWarn "Desired architecture not specified or unknown. Supported values are 'arm64' and 'amd64'. Using 'arm64' as default option"
  ARCH="arm64"
else
   PrintSuccess "Running script with selected architecture: ${ARCH}"
fi

##### Network #####
docker network rm ramses-sas-net
PrintSuccess "-- Network Removed --"
PrintSuccess "Creating new Docker network called 'ramses-sas-net'"
docker network create ramses-sas-net
echo

##### LOAD GENERATOR ####
#if $LOADGEN; then
#  PrintSuccess "Setting up Load Generator"
#  docker pull sbi98/sefa-load-generator:$ARCH
#  docker run -P --name sefa-load-generator -d --network ramses-sas-net sbi98/sefa-load-generator:$ARCH
#  exit 0
#fi

##### MYSQL #####
PrintSuccess "Setting up MySQL Server"
docker pull sbi98/mysql:$ARCH
docker run -P --name mysql -d --network ramses-sas-net sbi98/mysql:$ARCH
echo
sleep 2

##### SEFA #####
echo; PrintSuccess "Setting up the Managed System, SEFA!"; echo

PrintSuccess "Setting up Netflix Eureka Server"
docker pull sbi98/sefa-eureka:$ARCH
docker run -P --name sefa-eureka -d --network ramses-sas-net sbi98/sefa-eureka:$ARCH
echo
sleep 2

PrintSuccess "Setting up Spring Config Server"
#docker pull sbi98/sefa-configserver:$ARCH
#docker run -P --name sefa-configserver -e GITHUB_REPOSITORY_URL=$GITHUB_REPOSITORY_URL -d --network ramses-sas-net sbi98/sefa-configserver:$ARCH
#PERSONALIZED CONFIG SERVER
docker pull giamburrasca/sefa-configserver:v1.0
docker run -P --name sefa-configserver -e GITHUB_REPOSITORY_URL=$GITHUB_REPOSITORY_URL -d --network ramses-sas-net giamburrasca/sefa-configserver:v1.0
echo
sleep 2

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
   docker pull sbi98/$i:$ARCH
   docker run -P --name $i -d --network ramses-sas-net sbi98/$i:$ARCH
#   docker image tag sbi98/$i:$ARCH $i
#   docker rmi sbi98/$i:$ARCH
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
   docker pull sbi98/$i:$ARCH
#   docker image tag sbi98/$i:$ARCH $i
#   docker rmi sbi98/$i:$ARCH
   echo
   sleep 1
done

##### PROBE AND ACTUATORS #####
echo; PrintSuccess "Setting up probe and actuators!"; echo

PrintSuccess "Pulling Probe"
docker pull giamburrasca/probe:v1.12
docker run -P --name sefa-probe -d --network ramses-sas-net giamburrasca/probe:v1.12
echo
sleep 1

#PrintSuccess "Pulling sefa-instances-manager"
#docker pull giamburrasca/sefa-instance-manager-et:v0.73
#docker run -P --name sefa-instances-manager -d --network ramses-sas-net giamburrasca/sefa-instance-manager-et:v0.73
#echo
#sleep 1
#
#PrintSuccess "Pulling sefa-config-manager"
#docker pull sbi98/sefa-config-manager:$ARCH
#docker run -P --name sefa-config-manager -e GITHUB_OAUTH=$GITHUB_OAUTH -e GITHUB_REPOSITORY_URL=$GITHUB_REPOSITORY_URL -d --network ramses-sas-net sbi98/sefa-config-manager:$ARCH
#echo
#sleep 1

#microservice added to experiment
#PrintSuccess "Setting up Movie Info extra service"
#docker pull giamburrasca/movie-info-service:v1.1
#docker run -P --name movie-info-service -d --network ramses-sas-net giamburrasca/movie-info-service:v1.1
#echo
#sleep 2

echo; PrintSuccess "SEFA DONE!"; echo
