#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="${IMAGE_NAME:-country-integration}"
IMAGE_TAG="${IMAGE_TAG:-0.0.1}"
REGISTRY="${REGISTRY:-}"
NAMESPACE="${NAMESPACE:-ncba-integration}"
KUBECONFIG="${KUBECONFIG:-}"

echo -e "${GREEN}=== Country Integration Kubernetes Deployment ===${NC}\n"

# Functions
print_step() {
    echo -e "${YELLOW}[*] $1${NC}"
}

print_success() {
    echo -e "${GREEN}[✓] $1${NC}"
}

print_error() {
    echo -e "${RED}[✗] $1${NC}"
}

# Step 1: Validate Prerequisites
print_step "Validating prerequisites..."

if ! command -v kubectl &> /dev/null; then
    print_error "kubectl not found. Please install kubectl."
    exit 1
fi
print_success "kubectl found"

if ! command -v docker &> /dev/null; then
    print_error "docker not found. Please install docker."
    exit 1
fi
print_success "docker found"

# Step 2: Build Application
print_step "Building application..."
./gradlew clean build -x test
print_success "Application built"

# Step 3: Build Docker Image
print_step "Building Docker image: $IMAGE_NAME:$IMAGE_TAG..."
docker build -t $IMAGE_NAME:$IMAGE_TAG .
print_success "Docker image built"

# Step 4: Push to Registry (optional)
if [ ! -z "$REGISTRY" ]; then
    print_step "Pushing image to registry: $REGISTRY..."
    FULL_IMAGE="$REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
    docker tag $IMAGE_NAME:$IMAGE_TAG $FULL_IMAGE
    docker push $FULL_IMAGE
    IMAGE_TO_DEPLOY=$FULL_IMAGE
    print_success "Image pushed to registry"
else
    IMAGE_TO_DEPLOY=$IMAGE_NAME:$IMAGE_TAG
    print_step "Skipping registry push (no REGISTRY specified)"
fi

# Step 5: Validate Kubernetes Context
print_step "Validating Kubernetes context..."
CURRENT_CONTEXT=$(kubectl config current-context 2>/dev/null || echo "none")
if [ "$CURRENT_CONTEXT" = "none" ]; then
    print_error "No Kubernetes context configured"
    exit 1
fi
print_success "Connected to cluster: $CURRENT_CONTEXT"

# Step 6: Create Namespace
print_step "Creating namespace: $NAMESPACE..."
if kubectl get namespace $NAMESPACE &>/dev/null; then
    print_success "Namespace already exists"
else
    kubectl create namespace $NAMESPACE
    print_success "Namespace created"
fi

# Step 7: Apply Kubernetes Resources
print_step "Applying Kubernetes resources..."

kubectl apply -f k8s/namespace.yaml
print_success "Namespace configured"

kubectl apply -f k8s/secret.yaml
print_success "Secrets configured"

kubectl apply -f k8s/configmap.yaml
print_success "ConfigMap configured"

kubectl apply -f k8s/rbac.yaml
print_success "RBAC configured"

# Update deployment with correct image
print_step "Applying deployment with image: $IMAGE_TO_DEPLOY..."
kubectl set image deployment/country-integration \
    country-integration=$IMAGE_TO_DEPLOY \
    -n $NAMESPACE \
    --record || true

kubectl apply -f k8s/deployment.yaml
print_success "Deployment configured"

kubectl apply -f k8s/service.yaml
print_success "Services configured"

kubectl apply -f k8s/hpa.yaml
print_success "Autoscaling configured"

# Step 8: Wait for Deployment
print_step "Waiting for deployment to be ready (timeout: 5 minutes)..."
if kubectl rollout status deployment/country-integration \
    -n $NAMESPACE \
    --timeout=5m; then
    print_success "Deployment is ready"
else
    print_error "Deployment failed or timed out"
    echo "Debug information:"
    kubectl describe deployment country-integration -n $NAMESPACE
    kubectl get pods -n $NAMESPACE
    exit 1
fi

# Step 9: Verify Service
print_step "Verifying service..."
SERVICE_IP=$(kubectl get svc country-integration-svc -n $NAMESPACE -o jsonpath='{.spec.clusterIP}')
if [ ! -z "$SERVICE_IP" ]; then
    print_success "Service available at: $SERVICE_IP"
else
    print_error "Could not determine service IP"
fi

# Step 10: Show Status
print_step "Deployment status:"
echo ""
kubectl get all -n $NAMESPACE
echo ""

# Step 11: Port Forward (Optional)
echo ""
read -p "Do you want to setup port-forward for testing? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_step "Setting up port-forward..."
    print_success "Port-forward started: http://localhost:8080"
    print_success "Press Ctrl+C to stop"
    kubectl port-forward -n $NAMESPACE svc/country-integration-svc 8080:80
fi

print_success "Deployment completed successfully!"
echo ""
echo "Next steps:"
echo "1. Check logs: kubectl logs -n $NAMESPACE -l app=country-integration -f"
echo "2. Test API: curl http://localhost:8080/api/countries"
echo "3. View metrics: kubectl port-forward -n $NAMESPACE svc/country-integration-svc 9090:9090"
echo "4. View resources: kubectl get all -n $NAMESPACE"
