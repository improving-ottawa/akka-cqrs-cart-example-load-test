### Roadmap

This roadmap attempts to capture outstanding work/refactor to make a more robust load test:

- ~~Refactor item popularity projection in shopping-cart-service to use/support Cassandra as a backend.~~
- ~~Extend load-testing and shopping-cart-service command model to support more complex scenarios (currently load test only work with a single command:  AddItem)~~
- Add support to all sub-projects to run them in a 'deployed' environment:  likely k8s-based
- Less important:  instrument the Akka service with Telemetry, add support for a prometheus/grafana backend
- Add GC logging and heap tuning parameters to the process running shopping-cart-service
