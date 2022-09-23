FROM adoptopenjdk/openjdk11:jre

ARG JAR_FILE=target/*-with-dependencies.jar

# Copy the jar in a single directory
COPY ${JAR_FILE} /knowledge_engine/knowledge-engine.jar

EXPOSE 8280

# Add that directory to the class path.
ENTRYPOINT ["java", "-jar", "/knowledge_engine/knowledge-engine.jar", "8280"]
