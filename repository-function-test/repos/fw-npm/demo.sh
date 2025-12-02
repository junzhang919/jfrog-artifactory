#!/bin/bash -x

# delete only for Artifactory admins
#grab rt delete "fw-npm/demo/-/demo-1.0.0.tgz"

# build & deploy pkg
echo "Test ${repo} config"
grab rt config fw-npm
cd demo
sed -i "s/1.0.0/1.0.$BUILD_NUMBER/g" package.json

echo "Test ${repo} publish"
npm publish

# install from cache
grab rt config fw-npm-download

# local test
#npm install demo
#find .



