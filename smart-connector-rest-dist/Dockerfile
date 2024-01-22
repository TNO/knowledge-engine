FROM eclipse-temurin:21-alpine
LABEL org.opencontainers.image.source https://github.com/tno/knowledge-engine
LABEL org.opencontainers.image.description="Knowledge Engine: Smart Connector (with HTTP API)"
LABEL org.opencontainers.image.licenses=Apache-2.0

ARG JAR_FILE=target/*-with-dependencies.jar

# Copy the jar in a single directory
COPY ${JAR_FILE} /knowledge_engine/knowledge-engine.jar

# Make sure files/folders needed by the processes are accessable when they run under the nobody user
RUN chown -R nobody /knowledge_engine

# Switch to use a non-root user from here on
# Use uid of nobody user (65534) because kubernetes expects numeric user when applying pod security policies
USER 65534

EXPOSE 8280

# Add that directory to the class path.
ENTRYPOINT ["java", "-jar", "/knowledge_engine/knowledge-engine.jar", "8280"]
