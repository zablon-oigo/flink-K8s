#!/bin/bash

MC_ALIAS=minio
ENDPOINT=http://localhost:9000
ACCESS_KEY=admin
SECRET_KEY=password

mc alias set $MC_ALIAS $ENDPOINT $ACCESS_KEY $SECRET_KEY

mc mb $MC_ALIAS/demo

mc cp ../target/flink-filter.jar $MC_ALIAS/demo/
