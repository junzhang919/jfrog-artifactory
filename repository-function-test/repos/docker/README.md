# docker
- https://www.jfrog.com/confluence/display/RTF/Getting+Started+with+Artifactory+as+a+Docker+Registry

## use docker cmd tool
```
# for docker login
grab rt config docker

docker pull alpine:latest
docker tag alpine:latest arti.private-domain/ep/demo/alpine:latest
docker push arti.private-domain/ep/demo/alpine:latest
```

## use grab & jfrog tool
```
docker pull alpine:latest
grab rt u docker alpine:latest ep/demo/alpine:latest
```

## pull
```
docker pull arti.private-domain/ep/demo/alpine:latest
```
