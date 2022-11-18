FROM adoptopenjdk/openjdk11:jre
LABEL org.opencontainers.image.source https://github.com/tno/knowledge-engine
LABEL org.opencontainers.image.description="Knowledge Engine: Admin UI"
LABEL org.opencontainers.image.licenses=Apache-2.0

ARG JAR_FILE=target/*-with-dependencies.jar

# Copy the jar in a single directory
COPY ${JAR_FILE} /admin_ui/admin-ui.jar

EXPOSE 8280

# Add that directory to the class path.
ENTRYPOINT ["java", "-jar", "/admin_ui/admin-ui.jar", "8283"]
