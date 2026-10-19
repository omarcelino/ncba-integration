# Kubernetes Troubleshooting Guide

## Pod Issues

### Pod Stuck in Pending

**Symptoms**: Pod shows `Pending` status for extended time

**Diagnosis**:
```bash
kubectl describe pod <pod-name> -n ncba-integration
```

**Common Causes & Solutions**:

1. **Insufficient Resources**
   - Check node capacity:
   ```bash
   kubectl top nodes
   kubectl describe nodes
   ```
   - Solution: Scale down other applications or add more nodes

2. **Image Pull Issues**
   - Check image exists in registry:
   ```bash
   kubectl get events -n ncba-integration | grep Pull
   ```
   - Solution: Verify image name, tag, and registry credentials

3. **PVC Binding**
   - Check persistent volume claims:
   ```bash
   kubectl get pvc -n ncba-integration
   ```
   - Solution: Create persistent volumes or change storage class

### Pod CrashLoopBackOff

**Symptoms**: Pod keeps restarting

**Diagnosis**:
```bash
kubectl logs <pod-name> -n ncba-integration
kubectl logs <pod-name> -n ncba-integration --previous  # Previous crash
```

**Common Causes**:

1. **Application Errors**
   - Check logs for stack traces
   - Verify configuration (environment variables, secrets)
   ```bash
   kubectl exec -it <pod-name> -n ncba-integration -- env | grep -E 'DB_|SOAP_'
   ```

2. **Database Connection Failed**
   - Verify database is accessible:
   ```bash
   kubectl run -it --rm mysql-client --image=mysql:8 -- \
     mysql -h db-mysql.ncba-integration-svc -u root -p
   ```
   - Check secret values are correct (Base64 decoded)

3. **Out of Memory**
   - Check actual vs requested memory:
   ```bash
   kubectl top pod <pod-name> -n ncba-integration
   ```
   - Increase memory limit in deployment.yaml

### Pod Stuck in ImagePullBackOff

**Diagnosis**:
```bash
kubectl describe pod <pod-name> -n ncba-integration | grep -A5 Events
```

**Solutions**:
1. Verify image name and tag are correct
2. Check Docker registry credentials:
   ```bash
   kubectl get secret country-integration-docker -n ncba-integration -o yaml
   ```
3. Rebuild and push image with correct tag

## Deployment Issues

### Deployment Not Scaling

**Check HPA status**:
```bash
kubectl get hpa -n ncba-integration
kubectl describe hpa country-integration-hpa -n ncba-integration
```

**Common Issues**:
1. Metrics server not installed:
   ```bash
   kubectl get deployment -n kube-system metrics-server
   ```
2. Resource requests not set (HPA needs them)
3. Pod CPU/Memory not tracked yet

**Solutions**:
- Manually scale: `kubectl scale deployment country-integration --replicas=5 -n ncba-integration`
- Install metrics server: `kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml`

## Service & Networking

### Service Not Accessible

**Verify service exists**:
```bash
kubectl get svc -n ncba-integration
kubectl describe svc country-integration-svc -n ncba-integration
```

**Test connectivity**:
```bash
# From pod
kubectl exec -it <pod-name> -n ncba-integration -- curl http://localhost:8080/actuator/health

# Port forward to local
kubectl port-forward -n ncba-integration svc/country-integration-svc 8080:8080
curl http://localhost:8080/api/countries

# DNS resolution
kubectl run -it --rm busybox --image=busybox -- nslookup country-integration-svc.ncba-integration.svc.cluster.local
```

### LoadBalancer Pending

**Check status**:
```bash
kubectl get svc -n ncba-integration country-integration-lb
```

**Solutions**:
1. If using cloud provider (AWS/GCP/Azure), ensure it's configured
2. For on-premise, use NodePort instead:
   ```bash
   kubectl patch svc country-integration-lb -p '{"spec": {"type": "NodePort"}}' -n ncba-integration
   ```
3. Access via: `http://<node-ip>:<node-port>`

## Performance Issues

### High CPU Usage

**Check metrics**:
```bash
kubectl top pods -n ncba-integration --sort-by=cpu
```

**Investigation**:
```bash
# Enable verbose logging
kubectl set env deployment/country-integration \
  -n ncba-integration \
  LOGGING_LEVEL_COM_NCBA_INTEGRATION=DEBUG

# Check logs
kubectl logs <pod-name> -n ncba-integration -f | grep -E 'WARN|ERROR'
```

**Solutions**:
1. Increase JVM heap: Update JAVA_OPTS in deployment.yaml
2. Enable caching (already configured)
3. Optimize database queries
4. Add more replicas

### High Memory Usage

**Check memory**:
```bash
kubectl top pods -n ncba-integration --sort-by=memory
kubectl describe pod <pod-name> -n ncba-integration | grep -A4 "Limits\|Requests"
```

**Solutions**:
1. Decrease heap size if OOMKilled
2. Enable memory pressure monitoring:
   ```bash
   kubectl describe node <node-name>
   ```
3. Review cache configuration
4. Check for memory leaks in logs

## Database Issues

### Database Connection Timeout

**Verify connectivity**:
```bash
# Get DB URL from secret
kubectl get secret country-integration-secret -n ncba-integration -o yaml | grep DB_URL | base64 -d

# Test connection
kubectl run -it --rm mysql-test --image=mysql:8 -- \
  mysql -h <db-host> -u <user> -p<password> -e "SELECT 1"
```

**Firewall/Network**:
```bash
# If using internal DB, verify service
kubectl get svc -n ncba-integration db-mysql

# Check network policies
kubectl get networkpolicies -n ncba-integration
```

### Database Lock Issues

```bash
# Check for long-running queries
kubectl exec -it mysql-pod -- mysql -p -e "SHOW PROCESSLIST"

# Kill stuck connection
kubectl exec -it mysql-pod -- mysql -p -e "KILL <connection-id>"
```

## SOAP API Issues

### SOAP Service Unavailable

**Test connectivity**:
```bash
kubectl exec -it <pod-name> -n ncba-integration -- \
  curl -X POST http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso \
  -H "Content-Type: text/xml" \
  -d '<soap:Envelope>...</soap:Envelope>'
```

**Check configuration**:
```bash
kubectl get configmap country-integration-config -n ncba-integration -o yaml | grep soap.url
```

**Solutions**:
1. Verify SOAP URL is accessible from cluster
2. Check network egress policies
3. Increase timeout values in ConfigMap
4. Implement retry logic in application

## Debugging Commands Cheat Sheet

```bash
# General
kubectl cluster-info
kubectl get nodes
kubectl get ns

# Pod debugging
kubectl get pods -n ncba-integration -o wide
kubectl describe pod <pod-name> -n ncba-integration
kubectl logs <pod-name> -n ncba-integration -f
kubectl exec -it <pod-name> -n ncba-integration -- /bin/sh

# Deployment debugging
kubectl rollout status deployment/country-integration -n ncba-integration
kubectl rollout history deployment/country-integration -n ncba-integration
kubectl rollout undo deployment/country-integration -n ncba-integration

# Resource monitoring
kubectl top nodes
kubectl top pods -n ncba-integration
kubectl describe node <node-name>

# Network debugging
kubectl port-forward <pod> <local-port>:<remote-port> -n ncba-integration
kubectl exec -it <pod> -- nslookup <service>
kubectl get svc,ep -n ncba-integration

# Events and logs
kubectl get events -n ncba-integration --sort-by=.metadata.creationTimestamp
kubectl logs <pod> -n ncba-integration --tail=100 -f

# Config debugging
kubectl get cm -n ncba-integration
kubectl get secrets -n ncba-integration
kubectl describe cm <configmap-name> -n ncba-integration
```

## Getting Help

1. Check application logs for specific errors
2. Review Kubernetes events: `kubectl get events -n ncba-integration`
3. Verify all resources are created: `kubectl get all -n ncba-integration`
4. Check cluster health: `kubectl cluster-info dump`

## Prevention

1. **Regular monitoring**: Set up Prometheus + Grafana
2. **Resource limits**: Always set requests and limits
3. **Health checks**: Configure liveness and readiness probes
4. **Network policies**: Restrict traffic between pods
5. **RBAC**: Limit service account permissions
6. **Pod disruption budgets**: Ensure availability during disruptions
