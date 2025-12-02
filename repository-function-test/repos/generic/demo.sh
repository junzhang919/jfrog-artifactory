#!/bin/bash
set -eax

echo "Test ${repo} upload"
grab rt upload README.md generic/ep/demo/

echo "Test ${repo} download"
grab rt download generic/ep/demo/README.md /tmp/README.md

echo "Test ${repo} delete"
grab rt delete generic/ep/demo/README.md

#if [[ $1 == "set_properties" ]]; then
#	curl -I -X PUT -u "${USERNAME}:${PASSWORD}" \
#		-G -d "properties=a=1;b=2,3" \
#		"http://arti.dev.fwci.aws.fwmrm.net/api/storage/generic-local/ep/iaas/${file}"
#
#elif [[ $1 == "delete_properties" ]]; then
#	curl -I -X DELETE -u "${USERNAME}:${PASSWORD}" \
#		-G -d "properties=a,b" \
#		"http://arti.dev.fwci.aws.fwmrm.net/api/storage/generic-local/ep/iaas/${file}"
#
#elif [[ $1 == "get_properties" ]]; then
#	curl -X GET -u "${USERNAME}:${PASSWORD}" \
#		"http://arti.dev.fwci.aws.fwmrm.net/api/storage/generic/ep/iaas/${file}?properties=a,b"
#
#fi


