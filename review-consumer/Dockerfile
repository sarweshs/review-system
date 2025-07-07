# Runtime image only
FROM eclipse-temurin:17-jre-jammy

# Create app user
RUN addgroup --system app && adduser --system --ingroup app app

# Set working directory
WORKDIR /app

# Copy pre-built JAR
COPY review-consumer/target/review-consumer-*.jar app.jar

# Create logs directory
RUN mkdir -p /logs && chown -R app:app /logs

# Use non-root user
USER app

# Expose app port
EXPOSE 7070

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:7070/actuator/health || exit 1

# Run app with docker profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]