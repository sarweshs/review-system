# Review Dashboard

This is a Spring Boot 3.2.6 + Thymeleaf application integrated with Keycloak for authentication.

## 🔧 Requirements

- Java 17+
- Maven
- Keycloak running at http://localhost:8080

## Setup Keycloak
1. Start Keycloak (if not running)
   bash
   Copy
   Edit
   docker run -p 8080:8080 quay.io/keycloak/keycloak:24.0.4 start-dev
   Access: http://localhost:8080
   Admin Console: http://localhost:8080/admin

2. Create Realm
   Go to Realms → Create

Name: review-realm

3. Create Client
   Clients → Create

Client ID: review-dashboard-client

Client Protocol: openid-connect

Root URL: http://localhost:8081

Save

In settings:

Enable Standard Flow

Valid Redirect URIs: http://localhost:8081/login/oauth2/code/*

4. Create Roles (optional)
5. Create Users
   Repeat the following for each user:

Example: Create User john
Go to Users → Create

Username: john

Enabled: true

Go to Credentials tab

Set Password: password

Disable "Temporary"

Assign roles if needed.

Repeat for other users.

6. Update application.yml if secret or realm differs.

## 🚀 Run

```bash
mvn spring-boot:run

