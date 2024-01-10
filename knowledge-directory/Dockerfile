FROM eclipse-temurin:21-alpine
LABEL org.opencontainers.image.source https://github.com/tno/knowledge-engine
LABEL org.opencontainers.image.description="Knowledge Engine: Knowledge Directory"
LABEL org.opencontainers.image.licenses=Apache-2.0

ARG JAR_FILE=target/*-with-dependencies.jar

# Copy the jar in a single directory
COPY ${JAR_FILE} /knowledge_directory/knowledge-directory.jar

EXPOSE 8282

# Add that directory to the class path.
ENTRYPOINT ["java", "-jar", "/knowledge_directory/knowledge-directory.jar", "8282"]
