FROM quay.io/keycloak/keycloak:24.0

# Install curl for health checks
USER root
RUN microdnf install curl && microdnf clean all
USER keycloak

# Expose the health check endpoint
EXPOSE 8080

# Default command
CMD ["start-dev"]
