FROM adoptopenjdk/openjdk11:alpine-jre

ARG JAR_FILE=target/*.jar
ARG DEPENDENCY_DIR=target/dependency/

# Copy all jars in a single directory
COPY ${DEPENDENCY_DIR} /knowledge_engine/
COPY ${JAR_FILE} /knowledge_engine/

# Add that directory to the class path.
ENTRYPOINT ["java", "-cp", "/knowledge_engine/*", "eu.knowledge.engine.rest.Main", "8280"]
