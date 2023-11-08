#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

##### ACTUATORS #####
PrintSuccess "Setting up local ACTUATORS"
docker run -P --name sefa-config-manager -d --network ramses-sas-net config-manager
echo
sleep 3
docker run -P --name sefa-instances-manager -d --network ramses-sas-net instances-manager
echo
sleep 3


##### KNOWLEDGE #####
PrintSuccess "Setting up local RAMSES"
docker run -P --name ramses-knowledge -d --network ramses-sas-net knowledge
echo
sleep 14
##### MONITOR #####
docker run -P --name ramses-monitor -d --network ramses-sas-net monitor
echo
sleep 5
##### AœNALYSE #####
docker run -P --name ramses-analyse -d --network ramses-sas-net analyse
echo
sleep 2
##### PLAN #####
docker run -P --name ramses-plan -d --network ramses-sas-net plan
echo
sleep 2
##### EXECUTE #####
docker run -P --name ramses-execute -d --network ramses-sas-net execute
echo
sleep 2
##### DASHBOARD #####
docker run -P --name ramses-dashboard -d --network ramses-sas-net dashboard
echo
sleep 2

echo; PrintSuccess "RAMSES DONE!"; echo
