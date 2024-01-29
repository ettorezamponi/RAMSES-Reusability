#!/bin/bash

socat -d TCP-LISTEN:2375,reuseaddr,fork UNIX:/var/run/docker.sock &
sleep 3

docker run -P --name teastore-instancesmanager -d --network teastore instances-manager

pid_socat = $(pgrep -f "socat -d TCP-LISTEN:2375,reuseaddr,fork UNIX:/var/run/docker.sock")

echo "Press any number to stop the socat process"
read userInput
if [[ $userInput =~ ^[1-9]$ ]]; then
    #kill pid_socat
    printf pid_socat
    echo "Socat stopped."
else
    echo "Remember to stop the socat!"
fi