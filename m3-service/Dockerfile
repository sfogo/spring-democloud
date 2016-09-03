FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER Ogo
ADD ./target/m3-service.jar /app.jar
CMD ["java", "-Xmx200m", "-jar", "/app.jar"]
EXPOSE 8093
