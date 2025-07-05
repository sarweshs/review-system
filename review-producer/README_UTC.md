# UTC Timezone Configuration

This application is configured to use UTC (Coordinated Universal Time) consistently throughout all date/time operations for better reliability and consistency across different timezones.

## Configuration Changes Made

### 1. Database Configuration
- **PostgreSQL**: Using `TIMESTAMP WITH TIME ZONE` columns
- **Hibernate**: Configured with `jdbc.time_zone=UTC` and `timezone=UTC`
- **JPA**: Set to use UTC timezone

### 2. Application Code
- **ReviewSourceService**: All timestamp conversions use `ZoneOffset.UTC`
- **ReviewSource Model**: `setLastProcessed()` method uses UTC
- **BadReviewRecord Model**: `createdAt` uses `LocalDateTime.now(ZoneOffset.UTC)`
- **MetricsService**: Uses `Instant.now()` (already in UTC)

### 3. Database Migrations
- **V2__create_review_sources_table.sql**: Uses `TIMESTAMP WITH TIME ZONE`
- **V3__create_bad_review_records_table.sql**: Uses `TIMESTAMP WITH TIME ZONE`

## Running the Application with UTC

### Option 1: JVM Parameter (Recommended)
```bash
mvn spring-boot:run -Duser.timezone=UTC
```

### Option 2: Environment Variable
```bash
export TZ=UTC
mvn spring-boot:run
```

### Option 3: Docker (if using containers)
```dockerfile
ENV TZ=UTC
ENV JAVA_OPTS="-Duser.timezone=UTC"
```

## Benefits of Using UTC

1. **Consistency**: All timestamps are stored and compared in the same timezone
2. **Reliability**: No daylight saving time issues
3. **Global Operations**: Works correctly across different geographical locations
4. **Debugging**: Easier to correlate logs and database records
5. **Scalability**: No timezone conversion issues when scaling across regions

## Log Output Examples

With UTC configuration, you'll see logs like:
```
Updated last processed timestamp for source: MinIO review data to 2025-07-04T17:25:09.431Z UTC
Filtered 3 files created after 2025-07-04T17:00:00Z UTC (last processed timestamp)
```

## Database Timestamps

All database timestamps are stored with timezone information:
- `last_processed_timestamp`: `TIMESTAMP WITH TIME ZONE`
- `created_at`: `TIMESTAMP WITH TIME ZONE`

This ensures that timestamps are always interpreted correctly regardless of the application server's timezone.

## Testing

The application includes tests to verify UTC handling:
```bash
mvn test -Dtest=UTCTimezoneTest
```

## Migration Notes

If you have existing data with timestamps in a different timezone, you may need to migrate the data. The new UTC configuration will ensure all future timestamps are stored correctly. 