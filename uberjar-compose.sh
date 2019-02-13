#!/bin/bash -xe

docker-compose -f docker-compose.yml -f docker-compose.uberjar.yml "$@"