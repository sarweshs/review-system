# Webhook Security Implementation

This document describes the security implementation for the storage event webhook endpoints.

## Overview

The Review Producer implements API key authentication for all storage event webhook endpoints to ensure secure communication between MinIO and the producer service.

## Security Features

### 1. API Key Authentication

- **Header-based authentication**: Uses `X-API-Key` header (configurable)
- **Stateless**: No session management required
- **Simple**: Easy to implement in MinIO and other storage providers

### 2. Endpoint Protection

- **Protected endpoints**: All `/api/producer/storage/**` endpoints require authentication
- **Public endpoints**: Health check and metrics endpoints remain public
- **Backward compatibility**: Other producer endpoints remain accessible

### 3. Security Configuration

The security is configured in `WebhookSecurityConfig.java` and uses Spring Security with a custom filter.

## Configuration

### Environment Variables

```bash
# Required: Set a secure API key
export WEBHOOK_API_KEY="your-secure-api-key-here"

# Optional: Custom header name (defaults to X-API-Key)
export WEBHOOK_API_KEY_HEADER="X-API-Key"
```

### Application Properties

```properties
# Webhook API key for authentication
webhook.api.key=your-secure-api-key-here

# Custom header name (optional, defaults to X-API-Key)
webhook.api.key.header=X-API-Key
```

## Implementation Details

### Security Filter

The `ApiKeyAuthenticationFilter` intercepts requests to storage endpoints and validates the API key:

1. **Request interception**: Only applies to `/api/producer/storage/**` paths
2. **Header extraction**: Extracts API key from configured header
3. **Validation**: Compares against configured API key
4. **Authentication**: Sets Spring Security context on success
5. **Error handling**: Returns 401 Unauthorized on failure

### Security Configuration

The `WebhookSecurityConfig` configures Spring Security:

- **CSRF disabled**: Not needed for API endpoints
- **Stateless sessions**: No session management
- **Endpoint protection**: Specific paths require authentication
- **Custom filter**: Uses API key authentication filter

## MinIO Integration

### Webhook Configuration

Configure MinIO to include the API key in webhook requests:

```bash
# Configure webhook with authentication
mc admin config set myminio notify_webhook:review-producer \
  endpoint="http://review-producer:8080/api/producer/storage/event/minio" \
  enable="on" \
  header="X-API-Key:your-api-key-here"

# Apply the configuration
mc admin service restart myminio
```

### Alternative: Using mc command

```bash
# Add webhook notification with authentication
mc event add myminio/review-data arn:minio:webhook::1:review-producer \
  --suffix .jl --events put \
  --header "X-API-Key:your-api-key-here"
```

## Testing

### Valid Request

```bash
curl -X POST http://localhost:7070/api/producer/storage/event/minio \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here" \
  -d @examples/minio-event-example.json
```

### Invalid API Key

```bash
curl -X POST http://localhost:7070/api/producer/storage/event/minio \
  -H "Content-Type: application/json" \
  -H "X-API-Key: invalid-key" \
  -d @examples/minio-event-example.json
```

Response: `401 Unauthorized` with message `{"error": "Invalid API key"}`

### Missing API Key

```bash
curl -X POST http://localhost:7070/api/producer/storage/event/minio \
  -H "Content-Type: application/json" \
  -d @examples/minio-event-example.json
```

Response: `401 Unauthorized` with message `{"error": "Missing API key"}`

### Public Endpoint (No Auth Required)

```bash
curl -X GET http://localhost:7070/api/producer/health
```

Response: `200 OK` with health status

## Security Best Practices

### 1. API Key Management

- **Use strong keys**: Generate cryptographically secure random keys
- **Rotate regularly**: Change API keys periodically
- **Environment variables**: Store keys in environment variables, not in code
- **Access control**: Limit who has access to API keys

### 2. Network Security

- **HTTPS**: Use HTTPS in production environments
- **Firewall**: Restrict access to webhook endpoints
- **IP whitelisting**: Consider IP-based access control for additional security

### 3. Monitoring

- **Log authentication failures**: Monitor for unauthorized access attempts
- **Rate limiting**: Implement rate limiting to prevent abuse
- **Audit logs**: Log all webhook requests for audit purposes

## Troubleshooting

### Common Issues

1. **401 Unauthorized**: Check API key configuration and header name
2. **403 Forbidden**: Verify endpoint path matches security configuration
3. **Missing header**: Ensure MinIO is configured to send the API key header

### Debug Mode

Enable debug logging for security components:

```properties
logging.level.com.reviewproducer.config=DEBUG
logging.level.org.springframework.security=DEBUG
```

### Testing Authentication

Use the provided test script to verify authentication:

```bash
./test-storage-events.sh
```

This script tests:
- Valid API key authentication
- Invalid API key rejection
- Missing API key rejection
- Public endpoint access 