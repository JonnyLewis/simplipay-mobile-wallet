# Mobile wallet Auth0 configuration

The shared mobile auth flow uses Auth0's public native-app endpoints:

1. `POST /dbconnections/signup` creates the database-connection user.
2. `POST /oauth/token` with the password-realm grant verifies the credentials and returns the Auth0 claims/token.
3. Only after Auth0 succeeds does the app create the matching Fineract self-service user/client and continue using the existing Fineract session.

The configured application is `demo-wallet-ios` in the `simplipay.eu.auth0.com` tenant. `auth0/mobile-wallet-native.json` is the declarative record of that configuration. It must remain Native, have no client secret, and enable the Password Realm grant. The database connection must allow the application. Create the API audience in the JSON if it does not already exist.

Copy the resulting public client ID into `core/network/src/commonMain/kotlin/org/mifospay/core/network/config/Auth0Config.kt`. Do not copy the portal client ID and do not add a client secret to the mobile app.

The current Fineract username is the wallet account reference. The password is verified by Auth0 first; Fineract then issues its normal session token for existing wallet APIs. This preserves the current Fineract contract while making Auth0 the identity authority.
