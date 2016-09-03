FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER Ogo
ADD ./target/turbine.jar /app.jar
CMD ["java", "-Xmx200m", "-jar", "/app.jar"]
EXPOSE 8989 8990
