FROM adoptopenjdk/openjdk11:alpine-jre

ARG JAR_FILE=target/*-with-dependencies.jar

# Copy the jar in a single directory
COPY ${JAR_FILE} /admin_ui/admin-ui.jar

EXPOSE 8280

# Add that directory to the class path.
ENTRYPOINT ["java", "-jar", "/admin_ui/admin-ui.jar", "8283"]
