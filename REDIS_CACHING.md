# Redis Caching Implementation

## Overview

The review system now includes Redis caching for improved performance and scalability. All review APIs are cached with configurable TTL (Time To Live) settings, and administrators have full control over cache management.

## Features

### 1. Cache Configuration
- **Default TTL**: 1 hour for most caches
- **Bad Reviews Cache**: 30 minutes (more frequent updates)
- **Statistics Cache**: 2 hours (less frequent updates)
- **Summary Cache**: 1 hour

### 2. Cached Endpoints

#### Good Reviews APIs
- `GET /api/reviews` - All reviews with pagination (cached by page, size, sort params)
- `GET /api/reviews/platform/{platform}` - Reviews by platform
- `GET /api/reviews/entity/{entityId}` - Reviews by hotel ID
- `GET /api/reviews/rating` - Reviews by rating range
- `GET /api/reviews/{reviewId}` - Single review by ID
- `GET /api/reviews/statistics` - Review statistics

#### Bad Reviews APIs
- `GET /api/reviews/bad` - All bad review records
- `GET /api/reviews/bad/platform/{platform}` - Bad reviews by platform
- `GET /api/reviews/bad/statistics` - Bad review statistics

#### Summary APIs
- `GET /api/reviews/summary` - Combined review summary (good + bad)

### 3. Cache Management APIs (Admin Only)

#### Cache Invalidation
- `POST /api/admin/cache/invalidate-all` - Invalidate all caches
- `POST /api/admin/cache/invalidate-reviews` - Invalidate reviews cache
- `POST /api/admin/cache/invalidate-bad-reviews` - Invalidate bad reviews cache
- `POST /api/admin/cache/invalidate-stats` - Invalidate statistics cache
- `POST /api/admin/cache/invalidate/{cacheName}` - Invalidate specific cache

#### Cache Information
- `GET /api/admin/cache/info` - Get cache information and statistics
- `GET /api/admin/cache/health` - Cache management health check

## Implementation Details

### 1. Redis Configuration (`RedisConfig.java`)
```java
@Configuration
@EnableCaching
public class RedisConfig {
    public static final String REVIEWS_CACHE = "reviews";
    public static final String BAD_REVIEWS_CACHE = "bad-reviews";
    public static final String REVIEW_STATS_CACHE = "review-stats";
    public static final String REVIEW_SUMMARY_CACHE = "review-summary";
    
    public static final Duration DEFAULT_TTL = Duration.ofHours(1);
}
```

### 2. Cache Service (`CacheService.java`)
- Provides cache invalidation methods
- Cache statistics and information
- Cache existence checks

### 3. Cache Management Controller (`CacheManagementController.java`)
- REST endpoints for admin cache operations
- Error handling and logging
- Health checks

### 4. Cached Controllers (`ReviewController.java`)
- All review endpoints use `@Cacheable` annotations
- Cache keys include method parameters for proper cache separation
- Different cache names for different data types

## Cache Keys

### Reviews Cache
- `all_{page}_{size}_{sortBy}_{sortDir}` - Paginated reviews
- `platform_{platform}` - Platform-specific reviews
- `entity_{entityId}` - Hotel-specific reviews
- `rating_{minRating}_{maxRating}` - Rating range reviews
- `review_{reviewId}` - Single review

### Bad Reviews Cache
- `all_bad` - All bad reviews
- `bad_platform_{platform}` - Platform-specific bad reviews

### Statistics Cache
- `statistics` - Good review statistics
- `bad_statistics` - Bad review statistics

### Summary Cache
- `summary` - Combined review summary

## Admin Dashboard Integration

### Cache Management UI
- **Cache Operations**: Buttons to invalidate specific caches
- **Cache Information**: Display current cache status
- **Cache Configuration**: Table showing TTL settings
- **Cache Benefits**: Information about performance improvements

### JavaScript Functions
- `invalidateCache(cacheType)` - Invalidate specific cache
- `invalidateAllCaches()` - Invalidate all caches with confirmation
- `getCacheInfo()` - Fetch and display cache information
- `showCacheStatus(message, type)` - Show status messages

## Configuration

### Redis Connection (`application.yml`)
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
```

### Cache TTL Settings
- **Reviews**: 1 hour
- **Bad Reviews**: 30 minutes
- **Statistics**: 2 hours
- **Summary**: 1 hour

## Benefits

### 1. Performance
- Reduces database load significantly
- Improves API response times
- Handles concurrent requests efficiently

### 2. Scalability
- Supports more concurrent users
- Reduces database connection pool usage
- Better resource utilization

### 3. Cost Optimization
- Reduces database query costs
- Lower infrastructure requirements
- Better resource efficiency

### 4. User Experience
- Faster page loads
- Responsive API endpoints
- Consistent performance under load

## Monitoring and Management

### 1. Cache Health
- Health check endpoint: `/api/admin/cache/health`
- Cache information endpoint: `/api/admin/cache/info`

### 2. Logging
- All cache operations are logged
- Cache invalidation events tracked
- Error handling with detailed logs

### 3. Admin Controls
- Full cache invalidation control
- Selective cache clearing
- Real-time cache status monitoring

## Usage Examples

### 1. Invalidate Specific Cache
```bash
curl -X POST http://localhost:7070/api/admin/cache/invalidate-reviews
```

### 2. Get Cache Information
```bash
curl http://localhost:7070/api/admin/cache/info
```

### 3. Invalidate All Caches
```bash
curl -X POST http://localhost:7070/api/admin/cache/invalidate-all
```

## Best Practices

### 1. Cache Invalidation
- Use selective invalidation when possible
- Avoid invalidating all caches unless necessary
- Monitor cache hit rates

### 2. TTL Settings
- Set appropriate TTL based on data freshness requirements
- Consider update frequency for different data types
- Balance performance vs. data freshness

### 3. Monitoring
- Regularly check cache health
- Monitor cache hit/miss ratios
- Track cache invalidation patterns

## Troubleshooting

### 1. Cache Not Working
- Check Redis connection
- Verify cache annotations are present
- Check cache key generation

### 2. Cache Invalidation Issues
- Verify admin permissions
- Check cache names match configuration
- Review error logs

### 3. Performance Issues
- Monitor cache hit rates
- Check Redis memory usage
- Verify TTL settings are appropriate

## Future Enhancements

### 1. Cache Analytics
- Cache hit/miss ratio monitoring
- Performance metrics dashboard
- Cache usage patterns analysis

### 2. Advanced Caching
- Cache warming strategies
- Conditional caching based on load
- Distributed cache invalidation

### 3. Monitoring Integration
- Prometheus metrics for cache operations
- Grafana dashboards for cache performance
- Alerting for cache issues 