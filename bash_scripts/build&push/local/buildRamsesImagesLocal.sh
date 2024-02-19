#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

##### INSTANCE MANAGER #####
cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/actuators/config-manager
# gradle build
#docker build -t config-manager .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/actuators/instances-manager
# gradle build
#docker build -t instances-manager .

##### RAMSES #####
cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managing-system/knowledge
docker build -t rs-knowledge .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managing-system/monitor
docker build -t rs-monitor .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managing-system/analyse
docker build -t rs-analyse .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managing-system/plan
docker build -t rs-plan .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managing-system/execute/
docker build -t rs-execute .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managing-system/dashboard/
docker build -t rs-dashboard .

echo; PrintSuccess "DOCKER LOCAL IMAGES BUILDED!"; echo