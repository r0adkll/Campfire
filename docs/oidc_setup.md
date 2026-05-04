# OIDC Setup for Campfire

Campfire supports logging in via OpenID Connect (OIDC) using Audiobookshelf's built-in OAuth integration. To make this work, the Campfire redirect URI must be registered in **Audiobookshelf**.

## Redirect URI

```
campfireaudiobooks://oauth
```

This is a custom Android URI scheme. When the OAuth provider redirects back after authentication, Android routes the URI to Campfire automatically via an intent filter.

---

## How to register the redirect URI in Audiobookshelf

Audiobookshelf requires the mobile redirect URI to be explicitly allowed.

1. Log in as an admin and go to **Settings > Authentication**.
2. Under the **OpenID Connect** section, locate the **Mobile Redirect URIs** (or **Allowed Redirect URIs**) field.
3. Add the following URI:
   ```
   campfireaudiobooks://oauth
   ```
4. Save your settings.

> The exact field name may differ slightly across Audiobookshelf versions. Look for a setting that controls which redirect URIs are accepted for mobile or native app clients.

---

## How it works

1. Campfire calls Audiobookshelf's `/auth/openid` endpoint to retrieve the OAuth provider's authorization URL.
2. The user is taken to the OAuth provider's login page in a browser tab.
3. After a successful login, the provider redirects to `campfireaudiobooks://oauth` with an authorization code.
4. Android's intent filter routes that URI back to Campfire.
5. Campfire exchanges the code for access and refresh tokens via Audiobookshelf.
