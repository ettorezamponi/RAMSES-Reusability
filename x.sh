#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability
sh SEFAtoModify.sh
sleep 5
sh RAMSEStoModify.sh

echo; PrintSuccess "EVERYTHING STARTED!"; echo