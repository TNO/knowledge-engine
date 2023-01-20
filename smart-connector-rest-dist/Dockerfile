FROM adoptopenjdk/openjdk11:jre
LABEL org.opencontainers.image.source https://github.com/tno/knowledge-engine
LABEL org.opencontainers.image.description="Knowledge Engine: Smart Connector (with HTTP API)"
LABEL org.opencontainers.image.licenses=Apache-2.0

ARG JAR_FILE=target/*-with-dependencies.jar

# Copy the jar in a single directory
COPY ${JAR_FILE} /knowledge_engine/knowledge-engine.jar

EXPOSE 8280

# Add that directory to the class path.
ENTRYPOINT ["java", "-jar", "/knowledge_engine/knowledge-engine.jar", "8280"]
