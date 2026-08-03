## Real-Time Kafka Stream Processing with Apache Flink on Kubernetes

![Kubernetes](https://img.shields.io/badge/Kubernetes-v1.34+-326CE5?logo=kubernetes&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-4.x-231F20?logo=apachekafka&logoColor=white)
![Strimzi](https://img.shields.io/badge/Strimzi-1.1.0-orange)
![Kind](https://img.shields.io/badge/Kind-Local_Cluster-0094F5)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?logo=apachemaven&logoColor=white)
![Apache Flink](https://img.shields.io/badge/Apache_Flink-1.20-E6526F?logo=apacheflink&logoColor=white)
![MinIO](https://img.shields.io/badge/MinIO-RELEASE-C72E49?logo=minio&logoColor=white)
![Helm](https://img.shields.io/badge/Helm-v3-0F1689?logo=helm&logoColor=white)


#### Architecture Diagram

<img width="654" height="418" alt="filter excalidraw" src="https://github.com/user-attachments/assets/cfb8730d-1131-4dc7-913b-e2f15e577d3f" />


#### Prerequisites

Install the following tools before getting started:

- Docker
- Kind
- kubectl
- Helm
- Java 17+
- Maven
- Git


Create the Kubernetes Cluster

```sh
kind create cluster --name demo --config kind-multi-node.yaml
```
Verify the cluster:

```sh
kubectl cluster-info
kubectl get nodes
```

Create the Namespace
```sh
kubectl create namespace flink
```

Install the Strimzi Kafka Operator
```sh
curl -L https://github.com/strimzi/strimzi-kafka-operator/releases/download/1.1.0/strimzi-cluster-operator-1.1.0.yaml \
  | sed 's/namespace: myproject/namespace: flink/g' \
  | kubectl create -f - -n flink
```

Wait for the operator:
```sh
kubectl wait deployment/strimzi-cluster-operator -n flink --for=condition=Available --timeout=180s
```

Deploy Kafka
```sh
kubectl apply -f kafka-controller.yaml -n flink
kubectl apply -f kafka-cluster.yaml -n flink
kubectl apply -f kafka-broker.yaml -n flink
```
Wait until Kafka is ready:
```sh
kubectl wait kafka/my-cluster --for=condition=Ready --timeout=300s -n flink
```
Verify:
```sh
kubectl get pods -n flink
```

Create Kafka Topic
```sh
kubectl apply -f kafka-topic.yaml -n flink
```

Install cert-manager

```sh
kubectl exec -it -n flink my-cluster-broker-0 -- \
bin/kafka-topics.sh \
--bootstrap-server localhost:9092 \
--list
```

```sh
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.18.2/cert-manager.yaml
```

Wait until all pods are running:
```sh
kubectl get pods -n cert-manager
```

Install the Flink Kubernetes Operator
```sh
helm upgrade --install flink-kubernetes-operator \
  flink-operator-repo/flink-kubernetes-operator \
  --namespace flink \
  --create-namespace
```

