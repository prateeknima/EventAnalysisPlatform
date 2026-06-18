# Kubernetes Local Runtime

This folder contains Kubernetes manifests for running the Event Analysis Platform in a local Kubernetes cluster.

The current local runtime uses minikube for validation. Production deployment would target AWS EKS with managed or separately provisioned dependencies.

## Components

The local Kubernetes stack includes:

- `api-service` - Spring Boot API service
- `postgres` - PostgreSQL database
- `redis` - Redis cache/status store
- `kafka` - Kafka broker
- `elasticsearch` - Elasticsearch search backend
- `keycloak` - OAuth2/OIDC identity provider

## Prerequisites

- Docker
- minikube
- kubectl

Start minikube:

`minikube start --driver=docker --memory=8192 --cpus=4`

Check cluster status:

`kubectl get nodes`

## Build Application Image

Build the application Docker image:

`docker build -t event-analysis-platform:local .`

Load the image into minikube:

`minikube image load event-analysis-platform:local`

This is required because minikube uses its own container runtime.

## Deploy Stack

Apply manifests:

`kubectl apply -f k8s/postgres/`

`kubectl apply -f k8s/redis/`

`kubectl apply -f k8s/kafka/`

`kubectl apply -f k8s/elasticsearch/`

`kubectl apply -f k8s/keycloak/`

`kubectl apply -f k8s/api-service/`

Check pods:

`kubectl get pods`

Expected state:

`api-service     1/1 or 2/2 Running`

`postgres        1/1 Running`

`redis           1/1 Running`

`kafka           1/1 Running`

`elasticsearch   1/1 Running`

`keycloak        1/1 Running`

## Access API

Port-forward the API service:

`kubectl port-forward service/api-service 8080:8080`

Check health:

`curl http://localhost:8080/actuator/health`

Expected:

`"status":"UP"`

## Access Keycloak

Port-forward Keycloak:

`kubectl port-forward service/keycloak 8081:8080`

Open:

`http://localhost:8081`

Default local credentials:

`admin / admin`

The realm and client must be recreated in this local Keycloak instance unless imported separately.

Required realm/client setup:

- Realm: `event-analysis`
- Client: `event-analysis-api`
- Service accounts enabled
- Client roles:
    - `incidents.read`
    - `incidents.write`
    - `metrics.read`

## Common Commands

View pods:

`kubectl get pods`

View services:

`kubectl get services`

View API logs:

`kubectl logs deployment/api-service --tail=120`

Restart API deployment:

`kubectl rollout restart deployment/api-service`

Check rollout:

`kubectl rollout status deployment/api-service`

Scale API locally:

`kubectl scale deployment api-service --replicas=1`

## Troubleshooting

### App tries to connect to localhost

Inside Kubernetes, `localhost` means the current pod.

Use Kubernetes service names instead:

- `postgres:5432`
- `redis:6379`
- `kafka:9092`
- `elasticsearch:9200`
- `keycloak:8080`

### Image changes are not picked up

Rebuild and reload the image:

`docker build -t event-analysis-platform:local .`

`minikube image load event-analysis-platform:local`

`kubectl rollout restart deployment/api-service`

If needed, use a new image tag and update `k8s/api-service/deployment.yml`.

### Pods are running but not ready

Check logs:

`kubectl logs deployment/api-service --tail=120`

Describe pod:

`kubectl describe pod <pod-name>`

Common causes:

- dependency service not ready
- wrong environment variable
- readiness probe failing
- insufficient local minikube resources

### Minikube is slow or kubectl times out

This stack is memory-heavy because it includes Kafka, Elasticsearch, and Keycloak.

For a 16 GB machine, use:

`minikube start --driver=docker --memory=8192 --cpus=4`

If the machine becomes slow, reduce local API replicas:

`kubectl scale deployment api-service --replicas=1`

## Production Notes

This local setup is for Kubernetes validation and learning.

For AWS production, the target architecture would use:

- Amazon EKS for Kubernetes
- Amazon ECR for container images
- Amazon RDS or Aurora PostgreSQL for database
- Amazon ElastiCache for Redis
- Amazon MSK or managed Kafka provider
- Amazon OpenSearch or Elastic Cloud for search
- AWS Secrets Manager for secrets
- AWS Load Balancer Controller for ingress
- IAM Roles for Service Accounts for AWS permissions