#!/bin/bash -x

cd demo
gem build demo.gemspec

echo "Test ${repo} config"
grab rt config gems

# upload
echo "Test ${repo} upload"
grab rt u gems demo-0.0.1.gem

# install
grab rt config gems-download
echo "Test ${repo} download"
gem install demo
gem list demo -d

echo "Test ${repo} delete"
grab rt delete "gems/gems/demo-0.0.1.gem"


