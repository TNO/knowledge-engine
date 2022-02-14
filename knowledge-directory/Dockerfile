FROM adoptopenjdk/openjdk11:alpine-jre

ARG JAR_FILE=target/*-with-dependencies.jar

# Copy the jar in a single directory
COPY ${JAR_FILE} /knowledge_directory/knowledge-directory.jar

EXPOSE 8282

# Add that directory to the class path.
ENTRYPOINT ["java", "-jar", "/knowledge_directory/knowledge-directory.jar", "8282"]
