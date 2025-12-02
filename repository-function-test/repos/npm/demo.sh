#!/bin/bash -x

# build & deploy pkg
cd demo
sed -i "s/1.0.0/1.0.$BUILD_NUMBER/g" package.json

echo "Test ${repo} config"
grab rt config npm

echo "Test ${repo} upload"
npm publish

# install from cache
grab rt config npm-download

# delete
echo "Test ${repo} delete"
grab rt delete "npm/demo/-/demo-1.0.$BUILD_NUMBER.tgz"

#npm install demo
#find .


