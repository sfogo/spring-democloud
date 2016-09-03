FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER Ogo
ADD ./target/m1-service.jar /app.jar
CMD ["java", "-Xmx200m", "-jar", "/app.jar"]
EXPOSE 8091
