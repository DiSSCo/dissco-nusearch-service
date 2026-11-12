FROM eclipse-temurin:25-alpine AS builder
WORKDIR /builder
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# We need the jre-noble for the gbif name-parser-rust
# The Ubuntu base already has user 1000 ("ubuntu"), so no extra user is created.
FROM eclipse-temurin:25-jre-noble
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
USER 1000

# We need the enable-native-access for the gbif name-parser-rust library
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "application.jar"]
