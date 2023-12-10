#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }

cd ../managed/teastore/services/tools.descartes.teastore.auth/
docker buildx build --platform linux/arm64 -t teastore-auth .
PrintSuccess "Auth image created"
sleep 2

cd ../managed/teastore/services/tools.descartes.teastore.image/
docker buildx build --platform linux/arm64 -t teastore-image .
PrintSuccess "Image image created"
sleep 2

cd ../managed/teastore/services/tools.descartes.teastore.persistence/
docker buildx build --platform linux/arm64 -t teastore-persistence .
PrintSuccess "Persistence image created"
sleep 2

cd ../managed/teastore/services/tools.descartes.teastore.recommender/
docker buildx build --platform linux/arm64 -t teastore-recommender .
PrintSuccess "Recommender image created"
sleep 2

cd ../managed/teastore/services/tools.descartes.teastore.registry/
docker buildx build --platform linux/arm64 -t teastore-registry .
PrintSuccess "Registry image created"
sleep 2

cd ../managed/teastore/services/tools.descartes.teastore.webui/
docker buildx build --platform linux/arm64 -t teastore-webui .
PrintSuccess "WebUI image created"
sleep 2

cd ../managed/teastore/utilities/tools.descartes.teastore.database/
docker buildx build --platform linux/arm64 -t teastore-db .
PrintSuccess "DB image created"

PrintSuccess "Teastore images for arm64 created"
