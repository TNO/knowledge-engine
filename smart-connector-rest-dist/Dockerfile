FROM adoptopenjdk/openjdk11:jre-11.0.11_9-alpine

ARG JAR_FILE=target/*.jar
ARG DEPENDENCY_DIR=target/dependency/

COPY ${JAR_FILE} /knowledge-engine.jar
COPY ${DEPENDENCY_DIR} /dependency/

ENTRYPOINT ["java", "-cp", "knowledge-engine.jar:/dependency/*", "eu.interconnectproject.knowledge_engine.rest.Main", "8280"]
