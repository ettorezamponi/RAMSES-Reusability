#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

TAG="2.1.0"
REPO="robotshop"  # Replace with your actual repository

##### Network #####
docker network rm robot-shop
PrintSuccess "-- Network Removed --"
PrintSuccess "Creating new Docker network called 'robot-shop'"
docker network create robot-shop
echo

# Start Eureka container
docker run -d -p 58082:58082 --name sefa-eureka --network robot-shop eureka

# Start MongoDB container
docker run -d --name mongodb --network robot-shop rs-mongodb:2.1.0

# Start Redis container
docker run -d --name redis --network robot-shop redis:6.2-alpine

# Start RabbitMQ container
docker run -d --name rabbitmq --network robot-shop rabbitmq:3.8-management-alpine

# Start Catalogue container (DO mongodb)
docker run -d --name catalogue --network robot-shop --health-cmd="curl -H 'X-INSTANA-SYNTHETIC: 1' -f http://localhost:8080/health" --health-interval=10s --health-timeout=10s --health-retries=3 rs-catalogue:2.1.0

# Start User container (DO mongodb, redis)
docker run -d --name user --network robot-shop --health-cmd="curl -H 'X-INSTANA-SYNTHETIC: 1' -f http://localhost:8080/health" --health-interval=10s --health-timeout=10s --health-retries=3 rs-user:2.1.0

# Start Cart container (DO redis, eureka)
docker run -d --name cart --network robot-shop --health-cmd="curl -H 'X-INSTANA-SYNTHETIC: 1' -f http://localhost:8080/health" --health-interval=10s --health-timeout=10s --health-retries=3 rs-cart:2.1.0

# Start MySQL container
docker run -d --name mysql --network robot-shop --cap-add=NET_ADMIN robotshop/rs-mysql-db:2.1.0

# Start Shipping container (DO MySql)
docker run -d --name shipping --network robot-shop --health-cmd="curl -H 'X-INSTANA-SYNTHETIC: 1' -f http://localhost:8080/health" --health-interval=10s --health-timeout=10s --health-retries=3 robotshop/rs-shipping:2.1.0

# Start Ratings container (DO MySql)
docker run -d --name ratings -e APP_ENV="prod" --network robot-shop --health-cmd="curl -H 'X-INSTANA-SYNTHETIC: 1' -f http://localhost:8080/health" --health-interval=10s --health-timeout=10s --health-retries=3 rs-ratings:2.1.0

# Start Payment container (DO Rabbitmq, Eureka)
docker run -d --name payment --network robot-shop --health-cmd="curl -H 'X-INSTANA-SYNTHETIC: 1' -f http://localhost:8080/health" --health-interval=10s --health-timeout=10s --health-retries=3 rs-payment:2.1.0

# Start Dispatch container (DO Rabbitmq)
docker run -d --name dispatch --network robot-shop rs-dispatch:2.1.0

# Start Web container (DO catalogue, user, shipping, payment)
docker run -d --name web --network robot-shop -e KEY=${INSTANA_AGENT_KEY} --health-cmd="curl -H 'X-INSTANA-SYNTHETIC: 1' -f http://localhost:8080/health" --health-interval=10s --health-timeout=10s --health-retries=3 -p 8080:8080 rs-web:2.1.0

PrintSuccess "ROBOT-SHOP RUNNING!"

durata_timer=20

while [ $durata_timer -gt 0 ]; do
    # Stampa il tempo rimanente
    printf "\rRemaining timer for set up RAMSES: %02d:%02d" $((durata_timer/60)) $((durata_timer%60))

    # Attendi 1 secondo
    sleep 1

    # Riduci il tempo rimanente di 1 secondo
    durata_timer=$((durata_timer-1))
done

docker run -P --name knowledge-db --network robot-shop -d knowledge-db
sleep 10
docker run -P --name rs-probe -d --network robot-shop rs-probe
docker run -p 8081:8081 --name maven-configserver -d --network robot-shop rs-configserver
#docker run -P --name teastore-instancesmanager -d --network teastore instances-manager

sleep 10

docker run -P --name ramses-knowledge -d --network robot-shop rs-knowledge

sleep 15

docker run -P --name ramses-analyse -d --network robot-shop rs-analyse
docker run -P --name ramses-plan -d --network robot-shop rs-plan
docker run -P --name ramses-execute -d --network robot-shop rs-execute
sleep 2
docker run -P --name ramses-monitor -d --network robot-shop rs-monitor
docker run -P --name ramses-dashboard -d --network robot-shop rs-dashboard


PrintSuccess "EVERYTHING SET UP!"
