#!/bin/bash
#source ami: ep-slave-base
#install dependency tool

# npm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
source ~/.bashrc
nvm install 16.0.0
npm version
# gem
sudo yum install -y gem
gem --version
# sbt
curl -L https://www.scala-sbt.org/sbt-rpm.repo > sbt-rpm.repo
sudo mv sbt-rpm.repo /etc/yum.repos.d/
sudo  yum install -y sbt
sbt version
#go
sudo yum install go -y
go env
# maven
sudo yum install maven -y
mvn --version
