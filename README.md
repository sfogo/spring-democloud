# Spring Cloud Demo
- [Overview](#overview)
- [Applications](#applications)
  - [Summary](#summary)
  - [Application level annotations](#application-level-annotations)
  - [Interaction diagram](##interaction-diagram)
- [Client side load balancing](#client-side-load-balancing)
- [Actuator](#actuator)
- [Run without Containers](#run-locally)
  - [Start all the pieces](#start-all-the-pieces)
  - [Eureka](#eureka)
  - [Configuration Server](#configuration-server)
  - [Generate traffic and watch dashboard](#dashboard)
  - [Add instances and load balance](#add-instances)
  - [View Actuator Data](#actuator-data)
- [Run in Docker Containers](#run-in-docker-containers)
  - [Build and run](#build-and-run)
  - [Docker Composition](#composition)

## Overview
This is a simple (_not [secured](http://projects.spring.io/spring-security)_) demo that can be run [with](#run-in-docker-containers) or [without](#run-locally) containers and that showcases a possible (if not typical) microservices [Spring Cloud](http://projects.spring.io/spring-cloud) landscape where :
- Participants (i.e service instances) pull configuration values from a central location ([Configuration Server](https://cloud.spring.io/spring-cloud-config/)) and self-register with a service registry ([Eureka](https://cloud.spring.io/spring-cloud-netflix/)) so that others (i.e. participants) can discover them. A gateway ([Zuul](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_router_and_filter_zuul)) publicly exposes some of the services (Zuul provides generic routing and filtering [capabilities](http://techblog.netflix.com/2013/06/announcing-zuul-edge-service-in-cloud.html)).
<img src="https://cloud.githubusercontent.com/assets/13286393/17674081/df6b0168-62d8-11e6-8803-06682109aa92.png"
     border="0" width="50%" />
- Services exist in multiple registered instances. A client declared as a [Ribbon](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#spring-cloud-ribbon) client gets client-side load balancing between registered instances (_for invocations that are declared load-balanced_). This is typically used when gateway routes incoming calls as well as for regular inter-service communication.
- Service availability is controlled using the [circuit breaker](http://martinfowler.com/bliki/CircuitBreaker.html) pattern whose implementation is [Hystrix](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_circuit_breaker_hystrix_clients). [Turbine](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_turbine) application aggregates invocation statistics for calls that are hystrix-wrapped. These statistics are called hystrix streams (_enabled provided that the application includes a dependency on_ [spring actuator](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_hystrix_metrics_stream)). [Hystrix dashboard](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_circuit_breaker_hystrix_dashboard) pulls aggregated metrics from Turbine for presentation. With the default configuration, Turbine pulls Hystix streams from applications but in this demo, applications channel metrics through [Rabbit MQ](https://www.rabbitmq.com).  
<img src="https://cloud.githubusercontent.com/assets/13286393/17674080/df69be48-62d8-11e6-9b38-8de10b404aee.png"
     border="0" width="40%" />
- Microservices landscape is inherently dynamic but participants must get hold of something **fixed** to be able to start working : you will typically have to choose between a fixed **configuration server** or a fixed **discovery service**. This demo uses the default option ([Config First Bootstrap](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#config-first-bootstrap)) while the other option ([Discovery First Bootstrap](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#discovery-first-bootstrap)) has applications bootstrap with the discovery service to discover the configuration server.  
In this demo, applications are configured to [fail fast](http://projects.spring.io/spring-cloud/spring-cloud.html#config-client-fail-fast) in case configuration server is not available but you can also tell them to keep [trying](http://projects.spring.io/spring-cloud/spring-cloud.html#config-client-retry). See the [**dockerized**](https://docs.docker.com/engine/understanding-docker/) flavor of the [demo](#run-in-docker-containers) where _Spring fail fast_ and Docker [always restart](https://docs.docker.com/engine/reference/run/#restart-policies-restart) policies allow for starting everything without minding about the booting order.

## Applications
### Summary
|Application|Context Path|Port|Comment|
|---|---|---|---|
|[Configuration Server](config-server)|`/`|8888|Management context path is `/admin`|
|[Gateway](gateway)|`/gateway`|8099|Routes `/gateway/m1` to M1 Service<br>Routes `/gateway/m2` to M2 Service|
|[Turbine](turbine)|`/`|8989|Management port 8991|
|[Eureka](eureka)|`/`|8761||
|[Dashboard](dashboard)|`/`|7980|Management port 7981|
|[M1 Service](m1-service)|`/`|8091|`GET /items/{id}` invokes both one outside resource and M3 (see interaction [diagram](#interaction-diagram))|
|[M2 Service](m2-service)|`/`|8092|Same as M1 with M2 tag|
|[M3 Service](m3-service)|`/`|8093|Counter service<br>`POST /counters/{tag}` increments counter<br>`GET /counters/{tag}` gets counter value<br>`GET /counters` retrieves all counters|

_**Notes**_
* All applications have actuator endpoints enabled (either explicitly in `pom.xml` with `spring-boot-starter-actuator` or as a consequence of being something else, e.g Configuration Server).
* [Rabbit MQ](https://www.rabbitmq.com) is running with port `5672`.

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
- Attaches a service to a Java interface and its REST endpoints (the ones you pick) to functions of that interface, making it really straightforward to code REST clients,
- Load balances service invocations,
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

## Run locally
### Start all the pieces
* Rabbit MQ
  * Start rabbit MQ separately (port `5672`)  
For instance on Ubuntu `sudo /etc/init.d/rabbitmq-server start`  
Installation notes are [here](https://www.rabbitmq.com/download.html).
* Applications
  * One option is to `cd` to each application and start them individually with `mvn spring-boot:run`, making sure you start with `config-server` (for fail-fast reasons explained in the [overview](#overview)), then on to `eureka` and other applications.
  * You can use this [run all](run-all.sh) script. It does some _rustic_ waiting and is clueless (other than not starting the next service) about start failures. In a real deployment you rely on options provided by your environment (for instance a combination of Spring `fail fast` and Docker `restart always` options).

```
$ ./run-all.sh 
Starting config-server...
config-server started PID:13325 Log:/tmp/democloud/config-server.pid.13325.txt
Starting eureka...
eureka started PID:13382 Log:/tmp/democloud/eureka.pid.13382.txt
Starting m3-service...
m3-service started PID:13483 Log:/tmp/democloud/m3-service.pid.13483.txt
Starting m2-service...
m2-service started PID:13576 Log:/tmp/democloud/m2-service.pid.13576.txt
Starting m1-service...
m1-service started PID:13649 Log:/tmp/democloud/m1-service.pid.13649.txt
Starting gateway...
gateway started PID:13754 Log:/tmp/democloud/gateway.pid.13754.txt
Starting turbine...
turbine started PID:13845 Log:/tmp/democloud/turbine.pid.13845.txt
Starting dashboard...
dashboard started PID:13926 Log:/tmp/democloud/dashboard.pid.13926.txt
Done.
You can shut it all down with : kill `cat /tmp/democloud/pids.txt`
```

### Eureka
* Go to `http://localhost:8761`  
<img src="https://cloud.githubusercontent.com/assets/13286393/17682183/c0ee86f8-62fe-11e6-992e-f5fa1ea591f0.png"
     border="0" width="80%" />
* Some REST endpoints are available:
  * Get all apps : `http://localhost:8761/eureka/apps`
  * Get one app : `http://localhost:8761/eureka/apps/M3-SERVICE`
  * See Eureka [operations](https://github.com/Netflix/eureka/wiki/Eureka-REST-operations) (_but unsure which ones are available through Spring_).

### Configuration Server
* REST endpoints are available:
  * `http://localhost:8888/m1-service/active/master`
  * `http://localhost:8888/gateway/active/master`
  * See [nomenclature](http://cloud.spring.io/spring-cloud-config/spring-cloud-config.html#_locating_remote_configuration_resources)

### Dashboard
* Go to `http://localhost:7980/hystrix`
* Monitor Turbine stream `http://localhost:8989`
* Generate some traffic from your browser
  * `http://localhost:8099/gateway/m1/items/123`
  * `http://localhost:8099/gateway/m2/items/xyz`
* Generate some traffic with this [Python3 Script](generate-traffic.py)
  * `generate-traffic.py 100`
  * It generates an Hystrix fallback every 7 calls (hence the over 10% error rate the dasboard displays).

<img src="https://cloud.githubusercontent.com/assets/13286393/17682185/c100f2c0-62fe-11e6-8297-9ea9a053a49a.png"
     border="0" width="90%" />

### Add instances
#### M1 Service
* M1 port (`server.port`) is acquired from Configuration Server and that cannot be bypassed unless you disable the bootstrap stage with `spring.cloud.bootstrap.enabled=false`. Once disabled, you can specify a different port (`8191` in this case) as well as other properties that M1 is expecting to see. Eureka and Rabbit MQ locations are provided (_it's actually superfluous because they are the default values anyway_). Start another M1 instance with port `8191` :
```
cd m1-service  
mvn spring-boot:run \
  -Dspring.cloud.bootstrap.enabled=false \
  -Ddemo.message='I am M1 at port 8191' \
  -Ddemo.resource='http://vachement.net/api/items' \
  -Dspring.cloud.config.uri='Not Applicable' \
  -Dspring.application.name=m1-service \
  -Deureka.client.serviceUrl.defaultZone='http://localhost:8761/eureka/' \
  -Dspring.rabbitmq.host=localhost \
  -Dspring.rabbitmq.port=5672 \
  -Dserver.port=8191 > /tmp/democloud/m1-service.port.8191.txt &
```

* Curl home endpoint for both instances
```
curl http://localhost:8191 
{"counter":{"name":"m1-service","value":0},
 "message":"I am M1 at port 8191","config.uri":"Not Applicable"}

curl http://localhost:8091 
{"counter":{"name":"m1-service","value":0},
 "message":"Hi! My name is m1.","config.uri":"http://localhost:8888"}
```

* Curl the gateway twice for m1 and you can see it alternates between M1 instances
```
curl http://localhost:8099/gateway/m1
{"counter":{"name":"m1-service","value":0},
 "message":"I am M1 at port 8191","config.uri":"Not Applicable"}

curl http://localhost:8099/gateway/m1
{"counter":{"name":"m1-service","value":0},
 "message":"Hi! My name is m1.","config.uri":"http://localhost:8888"}
```
* Refresh Eureka `http://localhost:8761`. M1 is now multi-instances.  
<img src="https://cloud.githubusercontent.com/assets/13286393/17723727/3d1b9728-63f1-11e6-8082-455215d96b59.png"
     border="0" width="80%" />

#### M2 Service
* Test file contains a JSON structure, value for `spring.application.json`
```
cd m2-service
cat ../testing/m2-instance-at-8192.txt
{
  "demo":{"message":"M2 Service at port 8192","resource":"http://vachement.net/api/items"},
  "eureka.client.serviceUrl.defaultZone":"http://localhost:8761/eureka/",
  "server":{"port":8192}, 
  "spring":{
    "application":{"name":"m2-service"},
    "rabbitmq":{"host":"localhost","port":5672},
    "cloud.config.uri":"Not Applicable"
  },
  "endpoints":{"cors":{
     "allowedOrigins":"*",
     "allowedMethods":"POST, GET, OPTIONS, DELETE",
     "maxAge":"3600",
     "allowedHeaders":"x-requested-with, authorization"}
  }
}
```
* Flatten JSON structure (hence the sed and tr). Set value for `spring.application.json`
```
mvn spring-boot:run \
  -Dspring.cloud.bootstrap.enabled=false \
  -Dspring.application.json="`cat ../testing/m2-instance-at-8192.txt | sed 's/^[ \t]*//' | tr -d '\n'`"
```
* Check home endpoint
```
curl http://localhost:8192 
{"counter":{"name":"m2-service","value":0},
 "message":"M2 Service at port 8192","config.uri":"Not Applicable"}
```

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

## Run in Docker containers
### Build and run
* Package all modules  
`mvn clean package`
* Enable [Spring Profile](http://docs.spring.io/spring-boot/docs/current/reference/html/howto-properties-and-configuration.html#howto-change-configuration-depending-on-the-environment) for Docker  
`export SPRING_PROFILES_ACTIVE=docker`  
Profile values are used in application configuration files (see [example](m1-service/src/main/resources/bootstrap.yml)), enabling configuration properties to be segegrated by [profile](http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-profile-specific-properties) to work in different environments (_for instance dev vs. prod_). There are multiple [ways](http://docs.spring.io/spring-boot/docs/current/reference/html/howto-properties-and-configuration.html#howto-set-active-spring-profiles) to set a profile active, using an environment variable is just one of them.
* Build Docker images and start containers  
`docker-compose -f ./docker-compose.yml up -d --build`
* All services still go by the [Config First Bootstrap](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#config-first-bootstrap) and the [fail fast](http://projects.spring.io/spring-cloud/spring-cloud.html#config-client-fail-fast) options. No starting order is mandated and therefore the Configuration Server may not yet be ready when a service starts up : it will fail but the `restart: always` option present in `Dockerfile` will restart the container. It may then take a few `Spring fail fast / Docker restart` cycles until the Configuration Server is found at boot time. On my system, it takes at least 3 to 4 minutes for all pieces to be up and running.

### Composition
[Docker Compose file](docker-compose.yml) builds new images except for RabbitMQ whose image is pulled from the [hub](https://hub.docker.com/_/rabbitmq/). Containers internally use the same ports as with the demo without containers (_but they could internally all use the same port_). Only the following pieces are externally exposed :

|Component|Externally|Container|
|---|---|---|
|Configuration Server|`8888`|`8888`|
|Eureka|`8761`|`8761`|
|Gateway|`80`|`8099`|
|Dashboard|`7980`|`7980`|
|Rabbit MQ Console|`15672`|`15672`|
* m1, m2 and m3 services can only be accessed through the gateway.
* Turbine stream at port `8989` is not externally exposed but the [Hystrix dashboard](http://localhost:7980/hystrix) can simply use `http://turbine:8989`. As in `docker` profile sections of configuration files, hostnames [**automatically created**](https://docs.docker.com/compose/networking/) by Docker compostion can be used for inter-container communication.

# Spring Cloud Demo
- [Overview](#overview)
- [Applications](#applications)
  - [Summary](#summary)
  - [Application level annotations](#application-level-annotations)
  - [Interaction diagram](##interaction-diagram)
- [Client side load balancing](#client-side-load-balancing)
- [Actuator](#actuator)
- [Run without Containers](#run-locally)
  - [Start all the pieces](#start-all-the-pieces)
  - [Eureka](#eureka)
  - [Configuration Server](#configuration-server)
  - [Generate traffic and watch dashboard](#dashboard)
  - [Add instances and load balance](#add-instances)
  - [View Actuator Data](#actuator-data)
- [Run in Docker Containers](#run-in-docker-containers)
  - [Build and run](#build-and-run)
  - [Docker Composition](#composition)

## Overview
This is a simple (_not [secured](http://projects.spring.io/spring-security)_) demo that can be run [with](#run-in-docker-containers) or [without](#run-locally) containers and that showcases a possible (if not typical) microservices [Spring Cloud](http://projects.spring.io/spring-cloud) landscape where :
- Participants (i.e service instances) pull configuration values from a central location ([Configuration Server](https://cloud.spring.io/spring-cloud-config/)) and self-register with a service registry ([Eureka](https://cloud.spring.io/spring-cloud-netflix/)) so that others (i.e. participants) can discover them. A gateway ([Zuul](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_router_and_filter_zuul)) publicly exposes some of the services (Zuul provides generic routing and filtering [capabilities](http://techblog.netflix.com/2013/06/announcing-zuul-edge-service-in-cloud.html)).
<img src="https://cloud.githubusercontent.com/assets/13286393/17674081/df6b0168-62d8-11e6-8803-06682109aa92.png"
     border="0" width="50%" />
- Services exist in multiple registered instances. A client declared as a [Ribbon](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#spring-cloud-ribbon) client gets client-side load balancing between registered instances (_for invocations that are declared load-balanced_). This is typically used when gateway routes incoming calls as well as for regular inter-service communication.
- Service availability is controlled using the [circuit breaker](http://martinfowler.com/bliki/CircuitBreaker.html) pattern whose implementation is [Hystrix](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_circuit_breaker_hystrix_clients). [Turbine](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_turbine) application aggregates invocation statistics for calls that are hystrix-wrapped. These statistics are called hystrix streams (_enabled provided that the application includes a dependency on_ [spring actuator](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_hystrix_metrics_stream)). [Hystrix dashboard](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_circuit_breaker_hystrix_dashboard) pulls aggregated metrics from Turbine for presentation. With the default configuration, Turbine pulls Hystix streams from applications but in this demo, applications channel metrics through [Rabbit MQ](https://www.rabbitmq.com).  
<img src="https://cloud.githubusercontent.com/assets/13286393/17674080/df69be48-62d8-11e6-9b38-8de10b404aee.png"
     border="0" width="40%" />
- Microservices landscape is inherently dynamic but participants must get hold of something **fixed** to be able to start working : you will typically have to choose between a fixed **configuration server** or a fixed **discovery service**. This demo uses the default option ([Config First Bootstrap](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#config-first-bootstrap)) while the other option ([Discovery First Bootstrap](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#discovery-first-bootstrap)) has applications bootstrap with the discovery service to discover the configuration server.  
In this demo, applications are configured to [fail fast](http://projects.spring.io/spring-cloud/spring-cloud.html#config-client-fail-fast) in case configuration server is not available but you can also tell them to keep [trying](http://projects.spring.io/spring-cloud/spring-cloud.html#config-client-retry). See the [**dockerized**](https://docs.docker.com/engine/understanding-docker/) flavor of the [demo](#run-in-docker-containers) where _Spring fail fast_ and Docker [always restart](https://docs.docker.com/engine/reference/run/#restart-policies-restart) policies allow for starting everything without minding about the booting order.

## Applications
### Summary
|Application|Context Path|Port|Comment|
|---|---|---|---|
|[Configuration Server](config-server)|`/`|8888|Management context path is `/admin`|
|[Gateway](gateway)|`/gateway`|8099|Routes `/gateway/m1` to M1 Service<br>Routes `/gateway/m2` to M2 Service|
|[Turbine](turbine)|`/`|8989|Management port 8991|
|[Eureka](eureka)|`/`|8761||
|[Dashboard](dashboard)|`/`|7980|Management port 7981|
|[M1 Service](m1-service)|`/`|8091|`GET /items/{id}` invokes both one outside resource and M3 (see interaction [diagram](#interaction-diagram))|
|[M2 Service](m2-service)|`/`|8092|Same as M1 with M2 tag|
|[M3 Service](m3-service)|`/`|8093|Counter service<br>`POST /counters/{tag}` increments counter<br>`GET /counters/{tag}` gets counter value<br>`GET /counters` retrieves all counters|

_**Notes**_
* All applications have actuator endpoints enabled (either explicitly in `pom.xml` with `spring-boot-starter-actuator` or as a consequence of being something else, e.g Configuration Server).
* [Rabbit MQ](https://www.rabbitmq.com) is running with port `5672`.

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
- Attaches a service to a Java interface and its REST endpoints (the ones you pick) to functions of that interface, making it really straightforward to code REST clients,
- Load balances service invocations,
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

## Run locally
### Start all the pieces
* Rabbit MQ
  * Start rabbit MQ separately (port `5672`)  
For instance on Ubuntu `sudo /etc/init.d/rabbitmq-server start`  
Installation notes are [here](https://www.rabbitmq.com/download.html).
* Applications
  * One option is to `cd` to each application and start them individually with `mvn spring-boot:run`, making sure you start with `config-server` (for fail-fast reasons explained in the [overview](#overview)), then on to `eureka` and other applications.
  * You can use this [run all](run-all.sh) script. It does some _rustic_ waiting and is clueless (other than not starting the next service) about start failures. In a real deployment you rely on options provided by your environment (for instance a combination of Spring `fail fast` and Docker `restart always` options).

```
$ ./run-all.sh 
Starting config-server...
config-server started PID:13325 Log:/tmp/democloud/config-server.pid.13325.txt
Starting eureka...
eureka started PID:13382 Log:/tmp/democloud/eureka.pid.13382.txt
Starting m3-service...
m3-service started PID:13483 Log:/tmp/democloud/m3-service.pid.13483.txt
Starting m2-service...
m2-service started PID:13576 Log:/tmp/democloud/m2-service.pid.13576.txt
Starting m1-service...
m1-service started PID:13649 Log:/tmp/democloud/m1-service.pid.13649.txt
Starting gateway...
gateway started PID:13754 Log:/tmp/democloud/gateway.pid.13754.txt
Starting turbine...
turbine started PID:13845 Log:/tmp/democloud/turbine.pid.13845.txt
Starting dashboard...
dashboard started PID:13926 Log:/tmp/democloud/dashboard.pid.13926.txt
Done.
You can shut it all down with : kill `cat /tmp/democloud/pids.txt`
```

### Eureka
* Go to `http://localhost:8761`  
<img src="https://cloud.githubusercontent.com/assets/13286393/17682183/c0ee86f8-62fe-11e6-992e-f5fa1ea591f0.png"
     border="0" width="80%" />
* Some REST endpoints are available:
  * Get all apps : `http://localhost:8761/eureka/apps`
  * Get one app : `http://localhost:8761/eureka/apps/M3-SERVICE`
  * See Eureka [operations](https://github.com/Netflix/eureka/wiki/Eureka-REST-operations) (_but unsure which ones are available through Spring_).

### Configuration Server
* REST endpoints are available:
  * `http://localhost:8888/m1-service/active/master`
  * `http://localhost:8888/gateway/active/master`
  * See [nomenclature](http://cloud.spring.io/spring-cloud-config/spring-cloud-config.html#_locating_remote_configuration_resources)

### Dashboard
* Go to `http://localhost:7980/hystrix`
* Monitor Turbine stream `http://localhost:8989`
* Generate some traffic from your browser
  * `http://localhost:8099/gateway/m1/items/123`
  * `http://localhost:8099/gateway/m2/items/xyz`
* Generate some traffic with this [Python3 Script](generate-traffic.py)
  * `generate-traffic.py 100`
  * It generates an Hystrix fallback every 7 calls (hence the over 10% error rate the dasboard displays).

<img src="https://cloud.githubusercontent.com/assets/13286393/17682185/c100f2c0-62fe-11e6-8297-9ea9a053a49a.png"
     border="0" width="90%" />

### Add instances
#### M1 Service
* M1 port (`server.port`) is acquired from Configuration Server and that cannot be bypassed unless you disable the bootstrap stage with `spring.cloud.bootstrap.enabled=false`. Once disabled, you can specify a different port (`8191` in this case) as well as other properties that M1 is expecting to see. Eureka and Rabbit MQ locations are provided (_it's actually superfluous because they are the default values anyway_). Start another M1 instance with port `8191` :
```
cd m1-service  
mvn spring-boot:run \
  -Dspring.cloud.bootstrap.enabled=false \
  -Ddemo.message='I am M1 at port 8191' \
  -Ddemo.resource='http://vachement.net/api/items' \
  -Dspring.cloud.config.uri='Not Applicable' \
  -Dspring.application.name=m1-service \
  -Deureka.client.serviceUrl.defaultZone='http://localhost:8761/eureka/' \
  -Dspring.rabbitmq.host=localhost \
  -Dspring.rabbitmq.port=5672 \
  -Dserver.port=8191 > /tmp/democloud/m1-service.port.8191.txt &
```

* Curl home endpoint for both instances
```
curl http://localhost:8191 
{"counter":{"name":"m1-service","value":0},
 "message":"I am M1 at port 8191","config.uri":"Not Applicable"}

curl http://localhost:8091 
{"counter":{"name":"m1-service","value":0},
 "message":"Hi! My name is m1.","config.uri":"http://localhost:8888"}
```

* Curl the gateway twice for m1 and you can see it alternates between M1 instances
```
curl http://localhost:8099/gateway/m1
{"counter":{"name":"m1-service","value":0},
 "message":"I am M1 at port 8191","config.uri":"Not Applicable"}

curl http://localhost:8099/gateway/m1
{"counter":{"name":"m1-service","value":0},
 "message":"Hi! My name is m1.","config.uri":"http://localhost:8888"}
```
* Refresh Eureka `http://localhost:8761`. M1 is now multi-instances.  
<img src="https://cloud.githubusercontent.com/assets/13286393/17723727/3d1b9728-63f1-11e6-8082-455215d96b59.png"
     border="0" width="80%" />

#### M2 Service
* Test file contains a JSON structure, value for `spring.application.json`
```
cd m2-service
cat ../testing/m2-instance-at-8192.txt
{
  "demo":{"message":"M2 Service at port 8192","resource":"http://vachement.net/api/items"},
  "eureka.client.serviceUrl.defaultZone":"http://localhost:8761/eureka/",
  "server":{"port":8192}, 
  "spring":{
    "application":{"name":"m2-service"},
    "rabbitmq":{"host":"localhost","port":5672},
    "cloud.config.uri":"Not Applicable"
  },
  "endpoints":{"cors":{
     "allowedOrigins":"*",
     "allowedMethods":"POST, GET, OPTIONS, DELETE",
     "maxAge":"3600",
     "allowedHeaders":"x-requested-with, authorization"}
  }
}
```
* Flatten JSON structure (hence the sed and tr). Set value for `spring.application.json`
```
mvn spring-boot:run \
  -Dspring.cloud.bootstrap.enabled=false \
  -Dspring.application.json="`cat ../testing/m2-instance-at-8192.txt | sed 's/^[ \t]*//' | tr -d '\n'`"
```
* Check home endpoint
```
curl http://localhost:8192 
{"counter":{"name":"m2-service","value":0},
 "message":"M2 Service at port 8192","config.uri":"Not Applicable"}
```

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

## Run in Docker containers
### Build and run
* Package all modules  
`mvn clean package`
* Enable [Spring Profile](http://docs.spring.io/spring-boot/docs/current/reference/html/howto-properties-and-configuration.html#howto-change-configuration-depending-on-the-environment) for Docker  
`export SPRING_PROFILES_ACTIVE=docker`  
Profile values are used in application configuration files (see [example](m1-service/src/main/resources/bootstrap.yml)), enabling configuration properties to be segegrated by [profile](http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-profile-specific-properties) to work in different environments (_for instance dev vs. prod_). There are multiple [ways](http://docs.spring.io/spring-boot/docs/current/reference/html/howto-properties-and-configuration.html#howto-set-active-spring-profiles) to set a profile active, using an environment variable is just one of them.
* Build Docker images and start containers  
`docker-compose -f ./docker-compose.yml up -d --build`
* All services still go by the [Config First Bootstrap](http://cloud.spring.io/spring-cloud-static/spring-cloud.html#config-first-bootstrap) and the [fail fast](http://projects.spring.io/spring-cloud/spring-cloud.html#config-client-fail-fast) options. No starting order is mandated and therefore the Configuration Server may not yet be ready when a service starts up : it will fail but the `restart: always` option present in `Dockerfile` will restart the container. It may then take a few `Spring fail fast / Docker restart` cycles until the Configuration Server is found at boot time. On my system, it takes at least 3 to 4 minutes for all pieces to be up and running.

### Composition
[Docker Compose file](docker-compose.yml) builds new images except for RabbitMQ whose image is pulled from the [hub](https://hub.docker.com/_/rabbitmq/). Containers internally use the same ports as with the demo without containers (_but they could internally all use the same port_). Only the following pieces are externally exposed :
|Component|Externally|Container|
|---|---|---|
|Configuration Server|`8888`|`8888`|
|Eureka|`8761`|`8761`|
|Gateway|`80`|`8099`|
|Dashboard|`7980`|`7980`|
|Rabbit MQ Console|`15672`|`15672`|
* m1, m2 and m3 services can only be accessed through the gateway.
* Turbine stream at port `8989` is not externally exposed but the [Hystrix dashboard](http://localhost:7980/hystrix) can simply use `http://turbine:8989`. As in `docker` profile sections of configuration files, hostnames [**automatically created**](https://docs.docker.com/compose/networking/) by Docker compostion can be used for inter-container communication.

### Examples
`curl http://localhost/gateway/m1/items/123-abc-456`
<br/>
`{"item":"123-abc-456","server":"vachement.net","time":{"millis":1472933106,"text":"2016-09-03T13:05:06-07:00","day":"Sat","week":"35"},"counter":{"name":"m1-service","value":283},"message":"Hi! My name is m1."}`

`curl http://localhost/gateway/m2/items/321-xyz-123`
<br/>
`{"item":"321-xyz-123","server":"vachement.net","time":{"millis":1472933209,"text":"2016-09-03T13:06:49-07:00","day":"Sat","week":"35"},"counter":{"name":"m2-service","value":283},"message":"Hi! My name is m2."}`

`curl http://localhost/gateway/m3/counters`
<br/>
`[{"name":"m2-service","value":283},{"name":"m1-service","value":283}]`

