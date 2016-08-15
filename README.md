# Spring Cloud Demo
## Overview
This is a simple (not [secured](http://projects.spring.io/spring-security), not [containerized](https://docs.docker.com/engine/understanding-docker/)) demo that showcases a possible (if not typical) microservices [Spring Cloud](http://projects.spring.io/spring-cloud) landscape where :
- Participants (i.e service instances) pull configuration values from a central location ([Configuration Server](https://cloud.spring.io/spring-cloud-config/)).
- Participants self-register with a service registry ([Eureka](https://cloud.spring.io/spring-cloud-netflix/)) that enables other participants to discover them.
- A gateway ([Zuul](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_router_and_filter_zuul)) publicly exposes some of the services. Zuul provides generic routing and filtering [capabilities](http://techblog.netflix.com/2013/06/announcing-zuul-edge-service-in-cloud.html).
- Service availability is controlled using the [circuit breaker](http://martinfowler.com/bliki/CircuitBreaker.html) pattern whose implementation is [Hystrix](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_circuit_breaker_hystrix_clients)
- Services exist in multiple instances. A consumer declared as a [Ribbon](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#spring-cloud-ribbon) client gets client-side load balancing between registered instances (_for invocations that are declared load-balanced_).
- [Turbine](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_turbine) application aggregates invocation statistics for calls that are hystrix-wrapped. These statistics are called hystrix streams (they are enabled provided that the application includes a dependency on [spring actuator](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_hystrix_metrics_stream)). [Hystrix dashboard](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_circuit_breaker_hystrix_dashboard) pulls aggregated metrics from Turbine for presentation.

## Applications
### Summary
|Application|Context Path|Port|Actuator|Comment|
|---|---|---|---|---|
|[Configuration Server](config-server)|`/admin`|8888|No||
|[Gateway](gateway)|`/gateway`|8099|Yes|`/gateway/m1` routed to M1 Service<br>`/gateway/m2` routed to M2 Service|
|[Turbine](turbine)|`/`|8989|Yes|management port 8991|
|[Eureka](eureka)|`/`|8761|Yes||
|[Dashboard](dashboard)|`/`|7980|Yes|management port 7981|
|[M1 Service](m1-service)|`/`|8091|Yes|`GET /things/{id}` endpoint returns a JSON structure with same identifier as well as the total number of M1 invocations retrieved from M3|
|[M2 Service](m2-service)|`/`|8092|Yes|Same as above with M2 tag|
|[M3 Service](m3-service)|`/`|8093|Yes|Counter service<br>`POST /counters/{tag}` increments counter<br>`GET /counters/{tag}` gets counter value<br>`GET /counters` retrieves all counters|
|[Rabbit MQ](https://www.rabbitmq.com)|n/a|5672|n/a||
### Spring server annotations
* All applications use `@SpringBootApplication`.
* Applications that register with Eureka use `@EnableDiscoveryClient`.

|Application|Annotations|
|---|---|
|[Configuration Server](config-server)|`@EnableConfigServer`|
|[Gateway](gateway)|`@EnableZuulProxy`|
|[Turbine](turbine)|`@EnableTurbineStream`|
|[Eureka](eureka)|`@EnableEurekaServer`|
|[Dashboard](dashboard)|`@EnableHystrixDashboard`<br>`@EnableTurbineStream`|
|[M1 Service](m1-service)|`@EnableCircuitBreaker` : some calls are wrapped with `@HystrixCommand` \
`@EnableFeignClients` : invocations of M3 are feigned with `@FeignClient("m3-service")` \
`@RestController` : regular REST controlling|
|[M2 Service](m2-service)|Same as M1|
|[M3 Service](m3-service)||

TODO : insert table, for each line
- app name
- main spring annotations, especially if server
- ports
- actuator yes / no

## Actuator
TODO
- Provide some metrics
- Give some screen shots samples using spring-actuator demo app
