# Kubernetes Deployment Guide

## Prerequisites

- Kubernetes cluster (v1.21+)
- kubectl CLI configured
- Docker registry access (optional, for private images)
- MySQL database (external or within cluster)
- Helm (optional, for templating)

## Quick Start

### 1. Build and Push Docker Image

```bash
# Build the JAR
./gradlew build -x test

# Build Docker image
docker build -t country-integration:0.0.1 .

# Push to registry (optional)
docker tag country-integration:0.0.1 your-registry/country-integration:0.0.1
docker push your-registry/country-integration:0.0.1
```

### 2. Deploy to Kubernetes

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Create secrets (update values first!)
kubectl apply -f k8s/secret.yaml

# Create configuration
kubectl apply -f k8s/configmap.yaml

# Create RBAC
kubectl apply -f k8s/rbac.yaml

# Deploy application
kubectl apply -f k8s/deployment.yaml

# Expose service
kubectl apply -f k8s/service.yaml

# Setup autoscaling
kubectl apply -f k8s/hpa.yaml
```

### 3. Verify Deployment

```bash
# Check pod status
kubectl get pods -n ncba-integration

# View deployment status
kubectl rollout status deployment/country-integration -n ncba-integration

# Check service
kubectl get svc -n ncba-integration

# View logs
kubectl logs -n ncba-integration -l app=country-integration -f
```

## Configuration

### Environment Variables

All configuration is managed through:
- **ConfigMap**: `country-integration-config` - Non-sensitive configuration
- **Secret**: `country-integration-secret` - Sensitive data (DB credentials)

### Database Setup

The application requires a MySQL database. Options:

**Option 1: External MySQL**
```bash
# Update DB_URL in k8s/secret.yaml
# Example: jdbc:mysql://external-db-host:3306/countrydb
```

**Option 2: MySQL in Kubernetes**
```bash
# Deploy MySQL (example)
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install mysql bitnami/mysql \
  --namespace ncba-integration \
  --set auth.rootPassword=changeme \
  --set primary.persistence.size=10Gi
```

## Scaling

The deployment includes HPA (Horizontal Pod Autoscaler):
- **Min replicas**: 2
- **Max replicas**: 10
- **Triggers**: CPU 70%, Memory 80%

To manually scale:
```bash
kubectl scale deployment country-integration \
  -n ncba-integration \
  --replicas=5
```

## Updates & Rollback

### Deploy New Version
```bash
# Update image
kubectl set image deployment/country-integration \
  country-integration=country-integration:0.0.2 \
  -n ncba-integration

# Monitor rollout
kubectl rollout status deployment/country-integration -n ncba-integration
```

### Rollback
```bash
kubectl rollout undo deployment/country-integration -n ncba-integration
kubectl rollout history deployment/country-integration -n ncba-integration
```

## Health Checks

### Liveness Probe
Checks if pod is running and healthy. Restarts if failed.
```
GET /actuator/health/liveness
Initial delay: 60s, Period: 10s
```

### Readiness Probe
Checks if pod is ready to receive traffic. Removes from service if failed.
```
GET /actuator/health/readiness
Initial delay: 30s, Period: 5s
```

## Monitoring

### Access Management Endpoints
```bash
# Port forward to local
kubectl port-forward -n ncba-integration \
  svc/country-integration-svc 9090:9090

# Health check
curl http://localhost:9090/actuator/health

# Metrics
curl http://localhost:9090/actuator/prometheus
```

## Cleanup

```bash
# Delete entire namespace (includes all resources)
kubectl delete namespace ncba-integration

# Delete specific resource
kubectl delete deployment country-integration -n ncba-integration
```

## Troubleshooting

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common issues and solutions.
