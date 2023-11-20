#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

declare -a ramsesarr=("ramses-knowledge" "ramses-analyse" "ramses-plan" "ramses-execute" "ramses-monitor" "ramses-dashboard")
for i in "${ramsesarr[@]}"
do
   PrintSuccess "Pushing $i"

   docker tag $i giamburrasca/$i:arm64
   docker push giamburrasca/$i:arm64

   echo
   sleep 2
done