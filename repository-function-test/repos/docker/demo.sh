#!/bin/bash -x
## use docker cmd tool
echo "Test ${repo} config"
grab rt config docker # for docker login

echo "RUN touch /tmp/${BUILD_NUMBER}" >>Dockerfile
docker build -t arti.private-domain/ep/demo/alpine:latest .

echo "Test ${repo} upload"
docker push arti.private-domain/ep/demo/alpine:latest

## use grab & jfrog tool
docker pull alpine:latest
grab rt u docker alpine:latest ep/demo/alpine:grab_latest

# build and upload multi platfrom image: need buildx
docker buildx build --platform linux/amd64,linux/arm64 -t arti.private-domain/ep/demo/alpine:latest -f ./Dockerfile --push .

## pull
echo "Test ${repo} download"
docker pull arti.private-domain/ep/demo/alpine:latest
docker pull arti-cache.private-domain/ep/demo/alpine:latest
