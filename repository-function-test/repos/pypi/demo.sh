#!/bin/bash -x

## build & deploy
echo "Test ${repo} config"

grab rt config pypi
cd demo
sed -i "s/0.0.1/0.0.$BUILD_NUMBER/g" setup.py

echo "Test ${repo} upload"
python setup.py sdist upload -r artifactory

## install from cache server
grab rt config pypi-download
#pip install demo

echo "Test ${repo} delete"
grab rt delete "pypi/demo/0.0.$BUILD_NUMBER"