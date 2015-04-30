# Webim SDK sample

A sample for represent interaction with [Webim Mobile SDK].

### Version
1.3.6

### Installation
Add to your build.gradle dependencies:
```
compile 'com.webim.sdk:webimclientsdkandroid:1.3.6'
compile 'com.google.code.gson:gson:2.3'
```

### Usage
WebimSdk contains 2 types of session:
 - Online chat (WMSession) 
 - Offline chats (WMOfflineSession) 

If a user needs an immediate response from an operator, please, use Online chat.

If a user just leave request, and an operator can answer later (e.g. in one week), please, use Offline chats.

### Development
#####Online chat
Online chats are simpler for implementation. You can see example in **OnlineChatFragment**.
All you need is:
 - Create _WMSession.WMSessionDelegate_.
 - Overwrite event methods _(e.g. sessionDidChangeSessionStatus, sessionDidStartChat, sessionDidReceiveMessage and others)_
 - Create _WMSession_ using _Android Context_, Webim _account_ and _location_ (please, contact **Webim Support** for more info about this params), and _WMSessionDelegate_. 
 - Call _startSession()_ from _WMSession_ object.

After this steps, you will receive events in WMSessionDelegate and can perform any actions with WMObjects(WMChat, WMOperator, WMMessage and e.t.c.).

For interaction with Online Chat uses other methods from _WMSession_ _(e.g. startChat, sendMessage, sendImage, closeChat and others)._

If you want to stop communication with Webim service, please, call _stopSession_ (e.g onStop() or onDestroy()).

#####Offline Chats
WMOfflineSession can contain 0+ chats.

So, default imp is containing 2 Views
- Chats Chooser View (**OfflineFragment**) 
- Chat View (**OfflineChatFragment**)

Creation of WMOfflineSession is the same as WMSession, but it doesn't contain global delegate.
 - To receive cached data use method _getOfflineChats()_
 - To receive last data from server use _getHistoryForced()_ (boolean param **forced** means that data will be updated from beginning of the session. Please use cached data with **forced = false** - this will reduce internet traffic, and request only last changes from server.)
 - Each request has its own callback (_sendMessage_, _sendImage_, _deleteCaht_ and _e.t.c._), so you can interact with them separately.
 
Offline chat can create messages and chats locally, without an internet connection. For sync data with Webim Service please use method _sendUnsentRequests_ from _WMOfflineSession_.

###Push Notification
By default, online and offline sessions have GCM interaction.

For enable push-notification system, please add default realization (see android samples and **GcmIntentService** with **GcmBroadcastReceiver**).

To make WebimSdk collaborate with push call static method _WMSession.onPushMessage_ (see **GcmIntentService**).

### Todo's
 - Add info about "Synchronized requests" in WMOfflineChats.

License
-------
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[Webim Mobile SDK]:https://webim.ru/help/mobile-sdk/android-sdk-howto/