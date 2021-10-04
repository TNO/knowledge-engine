FROM adoptopenjdk/openjdk11:alpine-jre

ARG JAR_FILE=target/*.jar
ARG DEPENDENCY_DIR=target/dependency/

# Copy all jars in a single directory
COPY ${DEPENDENCY_DIR} /knowledge_directory/
COPY ${JAR_FILE} /knowledge_directory/

# Add that directory to the class path.
ENTRYPOINT ["java", "-cp", "/knowledge_directory/*", "eu.knowledge.engine.knowledgedirectory.Main", "8282"]
