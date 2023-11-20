#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

##### Network #####
docker network rm ramses-sas-net
PrintSuccess "-- Network Removed --"
PrintSuccess "Creating new Docker network called 'ramses-sas-net'"
docker network create ramses-sas-net
echo

##### MYSQL #####
PrintSuccess "Setting up MySQL Server"
docker run -P --name mysql -d --network ramses-sas-net mysql:latest
echo
sleep 2

##### SEFA #####
echo; PrintSuccess "Setting up the Managed System, SEFA!"; echo

PrintSuccess "Setting up Netflix Eureka Server"
docker run -P --name sefa-eureka -d --network ramses-sas-net eureka-registry-server:latest
echo
sleep 2

PrintSuccess "Setting up Config Server"
docker run -P --name config-server -d --network ramses-sas-net config-server:latest
echo
sleep 2

PrintSuccess "Setting up Restaurant Server"
docker run -P --name sefa-restaurant -d --network ramses-sas-net restaurant-service:latest
echo
sleep 2

PrintSuccess "Setting up Ordering Server"
docker run -P --name sefa-ordering -d --network ramses-sas-net ordering-service:latest
echo
sleep 2

PrintSuccess "Setting up Payment Server"
docker run -P --name sefa-payment-1 -d --network ramses-sas-net payment-proxy-1-service:latest
echo
sleep 2

PrintSuccess "Setting up Delivery Server"
docker run -P --name sefa-delivery-1 -d --network ramses-sas-net delivery-proxy-1-service:latest
echo
sleep 2

PrintSuccess "Setting up WebService Server"
docker run -P --name sefa-webservice -d --network ramses-sas-net web-service:latest
echo
sleep 2

PrintSuccess "Setting up API Gateway"
docker run -P --name api-gateway -d --network ramses-sas-net api-gateway-service:latest
echo
sleep 2