/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.webim.demo.client;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

import ru.webim.android.sdk.WMSession;

public class GcmInstanceIDListenerService extends InstanceIDListenerService {

    public static final String ACTION_TOKEN_REFRESH = "action.token.refresh";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        WMSession.removePushToken(this);

        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        Intent intent = new Intent(ACTION_TOKEN_REFRESH);
        sendBroadcast(intent);
    }
    // [END refresh_token]
}
