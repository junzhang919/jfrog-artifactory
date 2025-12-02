#!/bin/bash -x

#push:
echo "Test ${repo} upload"
grab rt u helm ./nginx-0.1.0.gz ep/demo/

#download:
echo "Test ${repo} download"
grab rt dl helm -c ep/demo/nginx-0.1.0.gz

## delete
echo "Test ${repo} delete"
grab rt delete helm/ep/demo/nginx-0.1.0.gz


