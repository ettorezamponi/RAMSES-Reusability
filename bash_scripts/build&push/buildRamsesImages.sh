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
docker buildx build --platform linux/amd64 -t giamburrasca/ramses-knowledge:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managing-system/monitor
docker buildx build --platform linux/amd64 -t giamburrasca/ramses-monitor:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managing-system/analyse
docker buildx build --platform linux/amd64 -t giamburrasca/ramses-analyse:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managing-system/plan
docker buildx build --platform linux/amd64 -t giamburrasca/ramses-plan:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managing-system/execute/
docker buildx build --platform linux/amd64 -t giamburrasca/ramses-execute:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managing-system/dashboard/
docker buildx build --platform linux/amd64 -t giamburrasca/ramses-dashboard:amd64 .

echo; PrintSuccess "DOCKER IMAGES BUILDED!"; echo