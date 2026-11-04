# Account system

## Implemented

- Provider-neutral account metadata in `AccountSession`.
- Local/offline profile creation with Minecraft-compatible offline UUID generation.
- Local profile names are stored as non-secret app preferences and can be removed.
- AES-GCM credential storage backed by Android Keystore for future online providers.

## Not yet implemented

Microsoft and Ely.by OAuth flows are not connected. Local/offline profiles are not authenticated online accounts and cannot access services that require Microsoft authentication.

## Security requirements

- Keep access and refresh tokens out of AccountSession, UI, logs, crash reports, and analytics.
- OAuth must validate state, use PKCE where supported, and use exact registered redirect URIs.
- Never embed confidential OAuth client secrets in the APK.
- Use HTTPS and validate provider responses before persisting credentials.
