FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:21-jre-jammy
RUN adduser --disabled-password -u 1000 java
WORKDIR application
COPY --chown=java:java --from=builder application/dependencies/ ./
RUN true
COPY --chown=java:java --from=builder application/spring-boot-loader/ ./
RUN true
COPY --chown=java:java --from=builder application/snapshot-dependencies/ ./
RUN true
COPY --chown=java:java --from=builder application/application/ ./
USER 1000

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
