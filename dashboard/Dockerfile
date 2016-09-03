FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER Ogo
ADD ./target/dashboard.jar /app.jar
CMD ["java", "-Xmx200m", "-jar", "/app.jar"]
EXPOSE 7979 7980
