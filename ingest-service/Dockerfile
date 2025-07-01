FROM eclipse-temurin:21-jre-alpine

# Create app user
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

# Set work directory
WORKDIR /app

# Copy the JAR file
ARG JAR_FILE=target/ingest-service-*.jar
COPY ${JAR_FILE} app.jar

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM optimizations for containers
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"] 