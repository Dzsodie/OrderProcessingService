FROM ubuntu:latest
LABEL authors="zsuzsannamakara"
FROM openjdk:17
WORKDIR /app
COPY target/order-service.jar order-processing-service.jar
ENTRYPOINT ["java", "-jar", "order-processing-service.jar"]