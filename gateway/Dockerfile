FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER Ogo
ADD ./target/gateway.jar /app.jar
CMD ["java", "-Xmx200m", "-jar", "/app.jar"]
EXPOSE 8099
