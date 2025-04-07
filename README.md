# Disaster-Relief-Claims
This platform models a comprehensive disaster relief claims processing system inspired by FEMA workflows, using a microservices architecture to handle the full lifecycle of claims from submission through fund disbursement.


This set of code samples provides a comprehensive implementation of the Disaster Relief Claims Platform. The system follows a microservices architecture with:

Claim Intake Service: Handles the submission of new claims and evidence upload
Damage Assessment Service: Processes evidence using AI to determine damage severity
Eligibility Validation Service: Uses a rules engine to determine claim eligibility
Fund Disbursement Service: Processes payments to claimants through various methods

Key features implemented include:

- Spring Boot microservices with clean separation of concerns
- Kafka for event-driven architecture and asynchronous processing
- Kubernetes deployment configuration for container orchestration
- API Gateway security with OAuth2 and rate limiting
- Prometheus monitoring configuration for observability
- Integration with AWS services like S3 for document storage
- Drools rules engine for eligibility determination

This architecture provides high availability, scalability, and resilience, essential for a system that must operate reliably during disaster scenarios when user loads may be high and unpredictable.RetryClaude can make mistakes. Please double-check responses. 3.7 Sonnet

# Extra Configs
## Apigee
### API Gateway Configuration
<!-- apiproxy/disaster-relief-api.xml -->
<APIProxy revision="1" name="disaster-relief-api">
    <Description>Disaster Relief Claims Processing API</Description>
    <DisplayName>Disaster Relief Claims API</DisplayName>
    <Policies>
        <!-- Security Policies -->
        <Policy>OAuth2-Validation</Policy>
        <Policy>Quota-Enforcement</Policy>
        <Policy>Spike-Arrest</Policy>
        <Policy>JSON-Threat-Protection</Policy>
        <Policy>API-Key-Verification</Policy>
        
        <!-- Operational Policies -->
        <Policy>Response-Cache</Policy>
        <Policy>CORS-Handling</Policy>
        <Policy>Error-Handling</Policy>
        <Policy>Analytics-Capture</Policy>
    </Policies>
    <ProxyEndpoints>
        <ProxyEndpoint>default</ProxyEndpoint>
    </ProxyEndpoints>
    <TargetEndpoints>
        <TargetEndpoint>claim-intake</TargetEndpoint>
        <TargetEndpoint>claim-assessment</TargetEndpoint>
        <TargetEndpoint>eligibility</TargetEndpoint>
        <TargetEndpoint>disbursement</TargetEndpoint>
    </TargetEndpoints>
</APIProxy>

### OAuth2 Authentication Policy 
<!-- apiproxy/policies/OAuth2-Validation.xml -->
<OAuthV2 async="false" continueOnError="false" enabled="true" name="OAuth2-Validation">
    <DisplayName>OAuth2 Token Validation</DisplayName>
    <Operation>VerifyAccessToken</Operation>
    <Scope>read write</Scope>
    <ExpiresIn>1800000</ExpiresIn>
    <GenerateResponse enabled="false"/>
    <ReuseRefreshToken>true</ReuseRefreshToken>
    <SupportedGrantTypes>
        <GrantType>authorization_code</GrantType>
        <GrantType>client_credentials</GrantType>
    </SupportedGrantTypes>
    <GrantType>request.queryparam.grant_type</GrantType>
    <AccessToken>request.header.Authorization</AccessToken>
    <RefreshToken>request.queryparam.refresh_token</RefreshToken>
    <ClientId>request.queryparam.client_id</ClientId>
    <ClientSecret>request.queryparam.client_secret</ClientSecret>
    <GenerateErrorResponse>false</GenerateErrorResponse>
</OAuthV2>

## Kubernetes
### Kubernetes Deployment Configuration
claim-intake-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: claim-intake-service
  namespace: disaster-relief
  labels:
    app: claim-intake-service
    version: v1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: claim-intake-service
  template:
    metadata:
      labels:
        app: claim-intake-service
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      containers:
      - name: claim-intake-service
        image: disasterrelief/claim-intake-service:latest
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: KAFKA_BOOTSTRAP_SERVERS
          valueFrom:
            configMapKeyRef:
              name: kafka-config
              key: bootstrap-servers
        - name: POSTGRES_URL
          valueFrom:
            secretKeyRef:
              name: db-secrets
              key: postgres-url
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: db-secrets
              key: postgres-user
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secrets
              key: postgres-password
        - name: AWS_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: aws-secrets
              key: access-key
        - name: AWS_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: aws-secrets
              key: secret-key
        - name: AWS_S3_BUCKET
          valueFrom:
            configMapKeyRef:
              name: aws-config
              key: s3-bucket
---
apiVersion: v1
kind: Service
metadata:
  name: claim-intake-service
  namespace: disaster-relief
spec:
  selector:
    app: claim-intake-service
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP

## Prometheus
### Prometheus Monitoring Configuration
# prometheus-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
      
    alerting:
      alertmanagers:
      - static_configs:
        - targets:
          - alertmanager:9093

    rule_files:
      - /etc/prometheus/rules/*.rules

    scrape_configs:
      # Kubernetes service discovery configuration
      - job_name: 'kubernetes-pods'
        kubernetes_sd_configs:
        - role: pod
        relabel_configs:
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
          action: keep
          regex: true
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
          action: replace
          target_label: __metrics_path__
          regex: (.+)
        - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
          action: replace
          regex: ([^:]+)(?::\d+)?;(\d+)
          replacement: $1:$2
          target_label: __address__
        - action: labelmap
          regex: __meta_kubernetes_pod_label_(.+)
        - source_labels: [__meta_kubernetes_namespace]
          action: replace
          target_label: kubernetes_namespace
        - source_labels: [__meta_kubernetes_pod_name]
          action: replace
          target_label: kubernetes_pod_name
          
      # Service discovery for Spring Boot applications
      - job_name: 'spring-boot'
        metrics_path: '/actuator/prometheus'
        kubernetes_sd_configs:
        - role: pod
        relabel_configs:
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
          action: keep
          regex: true
        - source_labels: [__meta_kubernetes_pod_label_app]
          regex: .*disaster-relief.*
          action: keep
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
          action: replace
          target_label: __metrics_path__
          regex: (.+)
        - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
          action: replace
          regex: ([^:]+)(?::\d+)?;(\d+)
          replacement: $1:$2
          target_label: __address__
        - action: labelmap
          regex: __meta_kubernetes_pod_label_(.+)
        - source_labels: [__meta_kubernetes_namespace]
          action: replace
          target_label: kubernetes_namespace
        - source_labels: [__meta_kubernetes_pod_name]
          action: replace
          target_label: kubernetes_pod_name
