# Account system — Phase 3

## Provider model

Nebula defines provider types for Microsoft, Ely.by, and local/offline profiles. Provider implementations must return non-secret account metadata separately from credentials.

## Credential handling

- `SecureTokenStore` encrypts access and refresh tokens with an AES-GCM key held by Android Keystore.
- Tokens are not part of `AccountSession` and must never be printed to logs, crash reports, analytics, or UI.
- OAuth implementations must use the provider's supported authorization flow, state validation, PKCE where supported, and exact redirect URI registration.
- Client applications must not embed confidential OAuth client secrets.
- Local/offline profiles are not authenticated online accounts and may not access services requiring Microsoft authentication.

## Current status

The encrypted token store and local-profile generator are implemented. Microsoft and Ely.by network/OAuth flows are interfaces only and are not yet connected. No online sign-in is claimed to work in this milestone.
