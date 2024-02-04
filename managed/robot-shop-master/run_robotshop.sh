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

# Start MongoDB container
docker run -d --name mongodb --network robot-shop --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 rs-mongodb:${TAG}

# Start Redis container
docker run -d --name redis --network robot-shop --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 redis:6.2-alpine

# Start RabbitMQ container
docker run -d --name rabbitmq --network robot-shop --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 rabbitmq:3.8-management-alpine

# Start Catalogue container
docker run -d --name catalogue --network robot-shop --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 ${REPO}/rs-catalogue:${TAG}

# Start User container
docker run -d --name user --network robot-shop --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 ${REPO}/rs-user:${TAG}

# Start Cart container
docker run -d --name cart --network robot-shop --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 rs-cart:${TAG}

# Start Eureka container
#docker run -d --name sefa-eureka --network robot-shop --build sefa-eureka --image eureka

# Start MySQL container
docker run -d --name mysql --network robot-shop --cap-add NET_ADMIN --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 ${REPO}/rs-mysql-db:${TAG}

# Start Shipping container
docker run -d --name shipping --network robot-shop --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 ${REPO}/rs-shipping:${TAG}

# Start Ratings container
docker run -d --name ratings --network robot-shop --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 ${REPO}/rs-ratings:${TAG}

# Start Payment container
docker run -d --name payment --network robot-shop --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 ${REPO}/rs-payment:${TAG}

# Start Dispatch container
docker run -d --name dispatch --network robot-shop --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 ${REPO}/rs-dispatch:${TAG}

# Start Web container
docker run -d --name web --network robot-shop --log-driver json-file --log-opt max-size=25m --log-opt max-file=2 --args "KEY=${INSTANA_AGENT_KEY}" --depends-on catalogue,user,shipping,payment --ports "8080:8080" ${REPO}/rs-web:${TAG}
