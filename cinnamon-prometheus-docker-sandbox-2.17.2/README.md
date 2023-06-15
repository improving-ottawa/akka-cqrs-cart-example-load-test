For deploying to K8s

Setting up prom stack:  https://github.com/prometheus-operator/prometheus-operator/blob/main/Documentation/user-guides/getting-started.md

Once operator is installed, create instance and grafana instance by deploying resources.yaml:

```
kubectl apply -f resources.yaml
```

For configuring Prometheus on app side, use the service monitor, defined in akka-shopping-cart services
