
cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managed-system/restaurant-service/
docker  buildx build --platform linux/amd64 -t giamburrasca/sefa-restaurant-service:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managed-system/ordering-service/
docker  buildx build --platform linux/amd64 -t giamburrasca/sefa-ordering-service:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managed-system/payment-proxies/payment-proxy-1-service/
docker  buildx build --platform linux/amd64 -t giamburrasca/sefa-payment-proxy-1-service:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managed-system/payment-proxies/payment-proxy-2-service/
docker  buildx build --platform linux/amd64 -t giamburrasca/sefa-payment-proxy-2-service:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managed-system/payment-proxies/payment-proxy-3-service/
docker  buildx build --platform linux/amd64 -t giamburrasca/sefa-payment-proxy-3-service:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managed-system/delivery-proxies/delivery-proxy-1-service/
docker  buildx build --platform linux/amd64 -t giamburrasca/sefa-delivery-proxy-1-service:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managed-system/delivery-proxies/delivery-proxy-2-service/
docker  buildx build --platform linux/amd64 -t giamburrasca/sefa-delivery-proxy-2-service:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managed-system/delivery-proxies/delivery-proxy-3-service/
docker  buildx build --platform linux/amd64 -t giamburrasca/sefa-delivery-proxy-3-service:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managed-system/web-service/
docker  buildx build --platform linux/amd64 -t giamburrasca/sefa-web-service:amd64 .

cd /Users/ettorezamponi/Documents/RAMSES/ramses-reusability/managed-system/api-gateway-service/
docker  buildx build --platform linux/amd64 -t giamburrasca/sefa-api-gateway:amd64 .