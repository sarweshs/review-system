
# 🛡️ Keycloak Setup Guide

This guide walks you through the complete setup of Keycloak with OAuth2 login, client credentials, role-based access control, and Spring Boot application integration.

---

## 🔥 1. Start Keycloak (if not running)

```bash
docker run -p 8080:8080 quay.io/keycloak/keycloak:24.0.4 start-dev
```

- Access: [http://localhost:8080](http://localhost:8080)
- Admin Console: [http://localhost:8080/admin](http://localhost:8080/admin)

---

## 🏰 2. Create Realm

- Go to **Realms → Create**
- Name: `review-realm`

---

## 🧩 3. Create Client

- Go to **Clients → Create**
   - **Client ID:** `review-dashboard-client`
   - **Client Protocol:** `openid-connect`
   - **Root URL:** `http://localhost:8081`
- Click **Save**

### In Settings:

- Enable **Standard Flow**
- **Valid Redirect URIs:**
  ```
  http://localhost:8081/login/oauth2/code/*
  ```

---

## 4. Create Roles (Optional)

---

## 5. Create Users

Repeat the following for each user.

### Example: Create User `john`

- Go to **Users → Create**
   - **Username:** `john`
   - **Enabled:** `true`

- Go to **Credentials** tab
   - Set **Password:** `password`
   - Disable **Temporary**
   - Click **Set Password**

- Assign roles if needed.

---

## 6. Update `application.yml` (if secret or realm differs)

---

## 🔐 How to Get/Set the Client Secret in Keycloak

- Log into Keycloak Admin Console: [http://localhost:8080/admin](http://localhost:8080/admin)
- Log in with your admin credentials.
- Select your realm (e.g., `review-realm`)
- Go to **Clients** from the left menu.
- Click on your client (e.g., `review-dashboard-client`)
- Go to the **Credentials** tab.

### Ensure:

- **Client Authentication:** `ON`

Copy the **Client Secret** and use it in your `application.yml`.

---

## ✅ How to Find the Client Secret in Keycloak 24+

- From the left menu, go to **Clients**
- Click on your client (e.g., `review-dashboard-client`)
- Click the **Credentials** tab near the top (next to Settings, Roles, etc.)
- The client secret will appear under **Client Authentication → Client Secret**
- Copy or regenerate if needed

### 🛠 If “Client Secret” is Not Showing:

- Your client might be set to **public** instead of **confidential**

To fix:

- Go to **Settings** tab of your client
- Scroll to **Access Settings**
- Make sure **Client Authentication** is enabled (`ON`)
- Save the changes
- Return to **Credentials** tab — the secret should now be visible

---

# ✅ Complete Keycloak Setup Guide

## 1. Access Admin Console

- URL: [http://localhost:8080/admin](http://localhost:8080/admin)
- Login with: `admin / admin`

## 2. Create or Reset Realm

- Delete existing `review-realm` if necessary
- Click **Create Realm**
   - Name: `review-realm`
- Click **Create**

## 3. Create the OAuth2 Client

- Go to **Clients → Create**
   - **Client ID:** `review-dashboard-client`
   - **Client Protocol:** `openid-connect`
   - **Root URL:** `http://localhost:8081`
- Click **Save**

## 4. Configure Client Settings

- In **Settings** tab:
   - **Access Type:** `confidential`
   - **Valid Redirect URIs:** `http://localhost:8081/login/oauth2/code/*`
   - **Web Origins:** `http://localhost:8081`
- Click **Save**

- In **Credentials** tab:
   - Copy the **Client Secret**

---

## 5. Create Roles

- Go to **Roles → Create**
   - Role Name: `admin`
   - Role Name: `user`
- Click **Save** after each

---

## 6. Create Users

### Admin User:

- Go to **Users → Create**
   - Username: `admin`
   - Email: `admin@example.com`
   - First Name: `Admin`
   - Last Name: `User`
- Click **Save**
- Go to **Credentials** tab:
   - Set password: `admin123`
   - Turn OFF "Temporary"
   - Click **Set Password**
- Go to **Role Mappings** tab:
   - Assign the `admin` role

### Regular User:

- Go to **Users → Create**
   - Username: `user`
   - Email: `user@example.com`
   - First Name: `Regular`
   - Last Name: `User`
- Click **Save**
- Go to **Credentials** tab:
   - Set password: `user123`
   - Turn OFF "Temporary"
   - Click **Set Password**
- Go to **Role Mappings** tab:
   - Assign the `user` role

---

## 7. Update Application Configuration

Check and update your application config with the **correct client secret**.  
For example, if the secret has changed, update this in your `application.yaml`:

```yaml
client-secret: NEW_SECRET
```

---

## 8. Get the New Client Secret

- Go to the client settings
- Click on the **Credentials** tab
- Copy the new **Client Secret**
- Update your application's config

---

## 9. Test the Setup

### Test Admin Access:

- Visit: [http://localhost:8081](http://localhost:8081)
- Click **Login with Keycloak**
- Login with: `admin / admin123`
- You should be redirected to `/admin` page

### Test User Access:

- Login with: `user / user123`
- You should be redirected to `/user` page

---

## 10. Security Configuration

- `/admin/**` requires `ROLE_admin`
- `/user` requires `ROLE_user`
- Other routes require authentication

---

# 🔧 Fix Keycloak Role Configuration

## 1. Check Client Scope Configuration

- Go to **Clients → review-dashboard-client → Client Scopes**
- Ensure `roles` is in the **Assigned Default Client Scopes**
- If not, move it from Optional → Default

---

## 2. Configure Client Scope Mapper

- Go to **Client Scopes → roles → Mappers**
- Click **Create**:
   - Name: `realm roles`
   - Mapper Type: `Realm Role`
   - Token Claim Name: `realm_access.roles`
   - Add to ID token: `ON`
   - Add to access token: `ON`
   - Add to userinfo: `ON`
- Click **Save**

---

## 3. Alternative: Use Built-in Mapper

- Go to **Clients → review-dashboard-client → Mappers**
- Click **Create**
   - Same configuration as above
- Click **Save**

---

## 4. Check User Role Assignment

- Go to **Users → admin → Role Mappings**
- Ensure `admin` role is in **Assigned Roles**
- If not, select it from **Available Roles** and click **Add Selected**

---

## 5. Update Application Configuration (if needed)

Ensure roles and client config are in sync with your app setup.
