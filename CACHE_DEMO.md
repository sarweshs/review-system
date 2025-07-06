# Demonstrating Cache Behavior in Postman

This guide shows how to demonstrate Redis cache behavior in the Review System using Postman.

---

## 1. Pick a Cached Endpoint
Use a summary/statistics endpoint that is cached:
- `GET {{baseUrl}}/api/reviews/summary`
- `GET {{baseUrl}}/api/reviews/statistics`
- `GET {{baseUrl}}/api/bad-review-records/statistics`

## 2. (Optional) Invalidate the Cache
To show the difference between cache miss and hit, first clear the cache:
- `POST {{baseUrl}}/api/admin/cache/invalidate-all`
  - Or use a more specific endpoint, e.g. `.../invalidate-reviews` or `.../invalidate-bad-reviews`.

## 3. Make the First Request (Cache Miss)
- Send a GET request to the cached endpoint (e.g., `/api/reviews/summary`).
- Observe the response time (may be slower).
- In backend logs, you should see a DB query or a log like `CACHE MISS`.

## 4. Make the Same Request Again (Cache Hit)
- Send the same GET request again.
- The response should be faster.
- In backend logs, you should NOT see a DB query or should see a log like `CACHE HIT`.

## 5. (Optional) Invalidate Cache Again and Repeat
- Clear the cache again.
- Repeat the GET request to see the cache miss/hit cycle.

---

## How to Explain in a Demo
- "The first request after a cache clear is a cache miss and hits the database. Subsequent requests are served from Redis cache, which is faster and reduces DB load."
- "If I change the data or clear the cache, the next request will again be a cache miss."

---

## Tips for Postman
- Use the "Send" button multiple times on the same request.
- Use the "Console" in Postman to see response times.
- Optionally, show backend logs to highlight cache hits/misses.

---

**You are ready to demo caching in your review system!** 