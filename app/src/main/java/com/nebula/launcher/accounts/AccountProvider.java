package com.nebula.launcher.accounts;

import android.app.Activity;

/** Provider boundary. OAuth implementations must use system-browser/verified flows and PKCE. */
public interface AccountProvider {
    AccountSession.Provider getType();
    void beginSignIn(Activity activity, Callback callback);

    interface Callback {
        void onSuccess(AccountSession session, String accessToken, String refreshToken);
        void onError(String safeMessage);
    }
}
