# Spring Cloud Demo
- [Overview](#overview)
- [Applications](#applications)
  - [Summary](#summary)
  - [Application level annotations](#application-level-annotations)
  - [Interaction diagram](##interaction-diagram)
- [Client side load balancing](#client-side-load-balancing)
- [Actuator](#actuator)
- [Examples and screen shots](#examples-and-screen-shots)
  - [Run it all locally](#run-it-all-locally)
  - [Eureka](#eureka)
  - [Generate traffic and watch dashboard](#dashboard)
  - [View Actuator Data](#actuator-data)

## Overview
This is a simple (not [secured](http://projects.spring.io/spring-security), not [containerized](https://docs.docker.com/engine/understanding-docker/)) demo that showcases a possible (if not typical) microservices [Spring Cloud](http://projects.spring.io/spring-cloud) landscape where :
- Participants (i.e service instances) pull configuration values from a central location ([Configuration Server](https://cloud.spring.io/spring-cloud-config/)). Participants self-register with a service registry ([Eureka](https://cloud.spring.io/spring-cloud-netflix/)) that enables other participants to discover them. A gateway ([Zuul](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_router_and_filter_zuul)) publicly exposes some of the services (Zuul provides generic routing and filtering [capabilities](http://techblog.netflix.com/2013/06/announcing-zuul-edge-service-in-cloud.html)).
<img src="https://cloud.githubusercontent.com/assets/13286393/17674081/df6b0168-62d8-11e6-8803-06682109aa92.png"
     border="0" width="50%" />
- Services exist in multiple registered instances. A consumer declared as a [Ribbon](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#spring-cloud-ribbon) client gets client-side load balancing between registered instances (_for invocations that are declared load-balanced_). This will typically be used when gateway routes incoming calls as well as for regular inter-service communication.
- Service availability is controlled using the [circuit breaker](http://martinfowler.com/bliki/CircuitBreaker.html) pattern whose implementation is [Hystrix](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_circuit_breaker_hystrix_clients). [Turbine](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_turbine) application aggregates invocation statistics for calls that are hystrix-wrapped. These statistics are called hystrix streams (they are enabled provided that the application includes a dependency on [spring actuator](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_hystrix_metrics_stream)). [Hystrix dashboard](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_circuit_breaker_hystrix_dashboard) pulls aggregated metrics from Turbine for presentation. With the default configuration, Turbine pulls Hystix streams from applications but in this demo, applications channel metrics through [Rabbit MQ](https://www.rabbitmq.com).  
<img src="https://cloud.githubusercontent.com/assets/13286393/17674080/df69be48-62d8-11e6-9b38-8de10b404aee.png"
     border="0" width="40%" />
- Microservices landscape is highly dynamic but participants must get hold of something **fixed** to be able to start working : you will typically have to choose between a fixed **configuration server** or a fixed **discovery service**. This demo uses the default option ([Config First Bootstrap](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#config-first-bootstrap)) while the other option ([Discovery First Bootstrap](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#discovery-first-bootstrap)) has applications bootstrap with the discovery service to discover the configuration server. Applications are configured to [fail fast](http://projects.spring.io/spring-cloud/spring-cloud.html#config-client-fail-fast) in case configuration server is not available but you can also tell an application to keep [trying](http://projects.spring.io/spring-cloud/spring-cloud.html#config-client-retry).  
_In a docker-containerized application, one option is to fail fast and use the [always restart](https://docs.docker.com/engine/reference/run/#restart-policies-restart) policy._

## Applications
### Summary
|Application|Context Path|Port|Actuator|Comment|
|---|---|---|---|---|
|[Configuration Server](config-server)|`/admin`|8888|No||
|[Gateway](gateway)|`/gateway`|8099|Yes|`/gateway/m1` routed to M1 Service<br>`/gateway/m2` routed to M2 Service|
|[Turbine](turbine)|`/`|8989|Yes|management port 8991|
|[Eureka](eureka)|`/`|8761|Yes||
|[Dashboard](dashboard)|`/`|7980|Yes|management port 7981|
|[M1 Service](m1-service)|`/`|8091|Yes|`GET /items/{id}` endpoint echoes the same identifier along with the total number of M1 invocations retrieved from M3|
|[M2 Service](m2-service)|`/`|8092|Yes|Same as above with M2 tag|
|[M3 Service](m3-service)|`/`|8093|Yes|Counter service<br>`POST /counters/{tag}` increments counter<br>`GET /counters/{tag}` gets counter value<br>`GET /counters` retrieves all counters|

_**Note**_ : [Rabbit MQ](https://www.rabbitmq.com) is running with port `5672`.

### Application level annotations
* All applications use `@SpringBootApplication`.
* Applications that register with Eureka use `@EnableDiscoveryClient`.

|Application|Annotations|
|---|---|
|[Configuration Server](config-server)|`@EnableConfigServer`|
|[Gateway](gateway)|`@EnableZuulProxy`|
|[Turbine](turbine)|`@EnableTurbineStream`|
|[Eureka](eureka)|`@EnableEurekaServer`|
|[Dashboard](dashboard)|`@EnableHystrixDashboard`<br>`@EnableTurbineStream`|
|[M1 Service](m1-service)|`@EnableCircuitBreaker` : some calls are wrapped with `@HystrixCommand`<br>`@EnableFeignClients` : invocations of M3 are feigned with `@FeignClient("m3-service")`<br>`@RestController`|
|[M2 Service](m2-service)|Same as M1|
|[M3 Service](m3-service)|`@RestController`|

### Interaction Diagram
<img src="https://cloud.githubusercontent.com/assets/13286393/17678268/36026ab8-62eb-11e6-9725-ac3e5d5564b1.png"
     border="0" width="60%" />

## Client Side Load Balancing
[Ribbon](https://spring.io/guides/gs/client-side-load-balancing) provides client-side load balancing. It will typically be used for **Gateway Routing** as well as with other **App to App** communication.  
<img src="https://cloud.githubusercontent.com/assets/13286393/17674082/df849a7e-62d8-11e6-9c20-c9254f338c4a.png"
     border="0" width="40%" />

Using the [Feign](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#spring-cloud-feign) declaration, it is even easier to get a load-balanced invocation. Feign is an extremely handy shortcut that :
- Attaches REST invocations to regular java functions, making it really simple to code REST consumers,
- Load balances invocations,
- Hystrix-wraps them (this can however be [disabled](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#spring-cloud-feign-hystrix)).

In this demo, M1 and M2 [invocations](https://github.com/sfogo/spring-democloud/blob/master/m1-service/src/main/java/com/vnet/democloud/m1/Application.java) of M3 are feigned.

## Actuator
Spring Cloud emphasizes the importance of Spring [Actuator](https://spring.io/guides/gs/actuator-service) endpoints as most participants must have them enabled to participate fully (especially for Hystrix streams). It also shows the extent of Spring configurability. Here are some stats (pulled from using the [actuator demo app](https://github.com/sfogo/spring-actuator-data)) for demo services that have almost no customization.

|Application|# of env props|# of config props|# of metrics|
|---|---|---|---|
|[Configuration Server](config-server)|149|262|37|
|[Gateway](gateway)|165|365|91|
|[Turbine](turbine)|159|380|135|
|[Eureka](eureka)|159|412|126|
|[Dashboard](dashboard)|159|398|82|
|[M1 Service](m1-service)|156|412|264|
|[M2 Service](m2-service)|156|412|264|
|[M3 Service](m3-service)|155|328|90|

## Examples and Screen Shots
### Run it all locally
_TODO_

### Eureka
* Go to Dashboard `http://localhost:8761`  
<img src="https://cloud.githubusercontent.com/assets/13286393/17682183/c0ee86f8-62fe-11e6-992e-f5fa1ea591f0.png"
     border="0" width="80%" />
* Some REST endpoints are available:
  * Get all apps : `http://localhost:8761/eureka/apps`
  * Get one app : `http://localhost:8761/eureka/apps/M3-SERVICE`
  * See Eureka [operations](https://github.com/Netflix/eureka/wiki/Eureka-REST-operations) (_but unsure which ones are available through Spring_).

### Dashboard
* Go to Dashboard `http://localhost:7980/hystrix`
* Monitor Turbine stream `http://localhost:8989`
* Generate some traffic from your browser
  * `http://localhost:8099/gateway/m1/items/123`
  * `http://localhost:8099/gateway/m2/items/xyz`
* Generate some traffic with this [Python3 Script](generate-traffic.py)
  * `generate-traffic.py 100`
  * It generates an Hystrix fallback every 7 calls (hence the over 10% error rate the dasboard displays).

<img src="https://cloud.githubusercontent.com/assets/13286393/17682185/c100f2c0-62fe-11e6-8297-9ea9a053a49a.png"
     border="0" width="90%" />

### Actuator Data
* Deploy [actuator app](https://github.com/sfogo/spring-actuator-data)  
`mvn package`  
`java -jar target/dependency/webapp-runner.jar --port 7070 target/gs-actuator-service-0.1.0`
* Go to `http://localhost:7070/app/actuate/index.html` (credentials are config / config) and change the actuator URL to one of the demo apps (for instance `http://localhost:8092` or `http://localhost:8099/gateway`)  
_(this is possible because all participants [enable CORS](config-server/src/main/resources/shared/application.yml))_  
<img src="https://cloud.githubusercontent.com/assets/13286393/17682184/c0ef47b4-62fe-11e6-8d04-64282f332ad1.png"
     border="0" width="80%" />
* Environment  
<img src="https://cloud.githubusercontent.com/assets/13286393/17682182/c0ecd52e-62fe-11e6-831e-c5eaa9388fb2.png"
     border="0" width="80%" />
<img src="https://cloud.githubusercontent.com/assets/13286393/17682181/c0e9bbd2-62fe-11e6-80ca-15d57a10e0d4.png"
     border="0" width="80%" />
