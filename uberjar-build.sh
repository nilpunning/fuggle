#!/bin/bash -xe

docker build --tag=fuggle:base .
docker build --tag=fuggle:builder --file=Dockerfile.dev .
docker run --volume=${HOME}/.m2:/root/.m2 --name=fuggle_builder fuggle:builder time lein uberjar

mkdir -p target
docker cp fuggle_builder:/opt/fuggle/target/fuggle-standalone.jar target/
docker rm fuggle_builder

docker build --tag=fuggle:latest --file=Dockerfile.uberjar .