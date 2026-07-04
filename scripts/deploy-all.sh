#!/bin/bash

set -e

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
NAMESPACE=flink

echo "Using base dir: $BASE_DIR"

kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

echo "Deploying Kafka..."
helm upgrade --install kafka bitnami/kafka \
  -n $NAMESPACE \
  -f "$BASE_DIR/infra/kafka/values.yaml"

echo "Deploying MinIO..."
helm upgrade --install minio minio/minio \
  -n $NAMESPACE \
  -f "$BASE_DIR/infra/minio/values.yaml"

echo "Deploying AKHQ..."
helm upgrade --install akhq akhq/akhq \
  -n $NAMESPACE \
  -f "$BASE_DIR/infra/akhq/values.yaml"

echo "Deploying Flink..."
kubectl apply -f "$BASE_DIR/infra/flink/flink-deployment.yaml"

sleep 20

kubectl apply -f "$BASE_DIR/infra/flink/flink-session-job.yaml"

echo "DONE "