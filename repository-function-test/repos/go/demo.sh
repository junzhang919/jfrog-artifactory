#!/bin/bash -x

# config
echo "Test ${repo} config"
grab rt config jfrog

# build
cd hello
jfrog rt go build go --build-name=demo --build-number=$BUILD_NUMBER

#push:
echo "Test ${repo} upload"
jfrog rt gp go v1.0.0 --build-name=demo --build-number=$BUILD_NUMBER

#pushinfo:
jfrog rt bce demo $BUILD_NUMBER && jfrog rt bp demo $BUILD_NUMBER

# will delete all version
echo "Test ${repo} delete"
grab rt delete "go/github.private-domain/ep/iaas/hello"

# delete the specify version
#grab rt delete "go/github.private-domain/ep/iaas/hello/@v/xxx.info"
#grab rt delete "go/github.private-domain/ep/iaas/hello/@v/xxx.mod"
#grab rt delete "go/github.private-domain/ep/iaas/hello/@v/xxx.zip"