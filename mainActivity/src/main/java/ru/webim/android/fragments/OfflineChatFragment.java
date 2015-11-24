package ru.webim.android.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.NetworkOnMainThreadException;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import ru.webim.android.adapters.MessagesAdapter;
import ru.webim.android.items.WMChat;
import ru.webim.android.items.WMHistoryChanges;
import ru.webim.android.items.WMMessage;
import ru.webim.android.sdk.OnHistoryResponseListener;
import ru.webim.android.sdk.WMBaseSession;
import ru.webim.android.sdk.WMException;
import ru.webim.android.sdk.WMOfflineSession;
import ru.webim.demo.client.R;

public class OfflineChatFragment extends FragmentWithProgressDialog {
    private static final String TAG = "OfflineChatFragment";
    private static final long REFRESH_PERIOD = 5000;
    private static final boolean ENABLE_ASYNC_REQUESTS = true; // If false will enable sync SDK requests
    private WMChat mChat;
    private WMOfflineSession mWMSession;

    private ListView mListViewChat;
    private MessagesAdapter mMessagesAdapter;
    private ArrayList<WMMessage> mMessages = new ArrayList<WMMessage>();

    private EditText mEditTextMessage;

    private Handler mRefreshHandler = new Handler();
    private Runnable mRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRefreshHandler != null) {
                if (ENABLE_ASYNC_REQUESTS) {
                    getHistory();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getHistorySync();
                        }
                    }).start();
                }
                markChatAsRead();
                mRefreshHandler.postDelayed(this, REFRESH_PERIOD);
            }
        }
    };

    //******************* BEGINNING OF FRAGMENT METHODS *************************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        initViews(rootView);
        return rootView;
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        mRefreshHandler.removeCallbacks(mRefreshRunnable);
        mRefreshHandler = null;
        super.onDetach();
    }
//******************* END OF FRAGMENT METHODS *************************/

    //******************* BEGINNING OF UI METHODS ******************************/
    private void initViews(View rootView) {
        initListView(rootView);
        initEditText(rootView);
        initSendButton(rootView);
    }

    private void fillListView() {
        if (mWMSession != null && mChat != null) {
            mMessages.clear();
            mMessages.addAll(mChat.getMessages());
        }
        if (mMessagesAdapter != null) {
            mMessagesAdapter.notifyDataSetChanged();
        }
    }

    private void initListView(View v) {
        mListViewChat = (ListView) v.findViewById(R.id.listViewChat);
        mListViewChat.setEmptyView(createEmptyTextView());

        mMessagesAdapter = new MessagesAdapter(getActivity(), mMessages, mWMSession.getServerUrl());
        mListViewChat.setAdapter(mMessagesAdapter);
        mMessagesAdapter.notifyDataSetChanged();

        fillListView();
    }

    private View createEmptyTextView() {
        View emptyChatView = View.inflate(getActivity(), R.layout.spinner_empty_view, null);
        ((ViewGroup) mListViewChat.getParent()).addView(emptyChatView);
        ViewGroup.LayoutParams params = emptyChatView.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        emptyChatView.setLayoutParams(params);
        return emptyChatView;
    }

    private void initSendButton(View v) {
        ImageButton sendButton = (ImageButton) v.findViewById(R.id.imageButtonSendMessage);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mEditTextMessage.getText().toString();
                sendMessageAction(message);
            }
        });
        sendButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (ENABLE_ASYNC_REQUESTS) {
                    sendFile();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sendFileSync();
                        }
                    }).start();
                }
                return true;
            }
        });
    }

    private void sendMessageAction(String message) {
        if (!TextUtils.isEmpty(message)) {
            if (ENABLE_ASYNC_REQUESTS) {
                sendMessage(message);
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendMessageSync();
                    }
                }).start();
            }
        }
    }

    private void initEditText(View v) {
        mEditTextMessage = (EditText) v.findViewById(R.id.editTextChatMessage);
    }

    @Override
    protected void refresh() {
        syncWithServer();
    }

    @Override
    protected void discard() {
        deleteChat(mChat);
    }

    private void makeActionWithChanges(WMHistoryChanges changes) {
        if (changes != null) {
            if (!changes.getNewChats().isEmpty()) {
                Log.e(TAG, "New Chats = " + changes.getNewChats().size());
            }

            if (!changes.getMessages().isEmpty()) {
                Log.e(TAG, "New Messages = " + changes.getMessages().size());
            }
            if (!changes.getModifiedChats().isEmpty()) {
                Log.e(TAG, "ModifiedChats = " + changes.getModifiedChats().size());
            }
        }
        if (isVisible()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (WMChat chat : mWMSession.getOfflineChats()) {
                        if (chat.getId().equals(mChat.getId())) {
                            mChat = chat;
                            fillListView();
                            break;
                        }
                    }
                }
            });
        }
    }
//******************* END OF UI METHODS ******************************/

    //******************* BEGINNING OF WEBIM-SDK-OFFLINE-CHATS INTERACTION METHODS ******************************/
    private void getHistory() {
        mWMSession.getHistoryForced(false, new OnHistoryResponseListener() {
            @Override
            public void onHistoryResponse(boolean successful, WMHistoryChanges changes, WMBaseSession.WMSessionError errorID) {
                Log.w(TAG, "onHistoryResponse");
                makeActionWithChanges(changes);
            }
        });
    }

    @Deprecated
    private void sendImage() {
        mWMSession.sendImage(bitmapToInputStream(getActivity()), WMOfflineSession.WMChatAttachmentImageType.WMChatAttachmentImageJPEG, mChat, null, null, new WMOfflineSession.OnMessageSendListener() {
            @Override
            public void onMessageSend(boolean successful, WMChat chat, WMMessage mwssage, WMOfflineSession.WMSessionError error) {
                Log.d("Image", "onMessageSend " + successful + " " + error);
            }
        });
    }

    private void sendFile() {
        File file = generateFile();
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(file);
            mWMSession.sendFile(fileInputStream, file.getName(), getMimeType(Uri.fromFile(file).toString()), mChat, null, null, new WMOfflineSession.OnMessageSendListener() {
                @Override
                public void onMessageSend(boolean successful, WMChat chat, WMMessage mwssage, WMOfflineSession.WMSessionError error) {
                    Log.d("Image", "onMessageSend " + successful + " " + error);
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void markChatAsRead() {
        mWMSession.markChatAsRead(mChat, new WMOfflineSession.OnChangeReadedByVisitorListener() {

            @Override
            public void onChangeReadedByVisitor(boolean successful, WMBaseSession.WMSessionError errorID) {
                Log.d(TAG, "markChatAsRead " + successful + " " + errorID);
            }
        });
    }

    private void syncWithServer() {
        Log.i(TAG, "syncWithServer");
        mWMSession.sendUnsentRequests(new WMOfflineSession.OnSyncListener() {

            @Override
            public void onMessageSend(boolean arg0, WMChat arg1, WMMessage arg2, WMBaseSession.WMSessionError arg3) {
                Log.d(TAG, "onMessageSend");
            }

            @Override
            public void onChatIdChanged(String arg0, String arg1) {
                Log.d(TAG, "onChatIdChanged");
            }
        });
    }

    private void sendMessage(String message) {
        mWMSession.sendMessage(message, mChat, null, null, new WMOfflineSession.OnMessageSendListener() {

            @Override
            public void onMessageSend(boolean successful, WMChat chat, WMMessage mwssage, WMBaseSession.WMSessionError error) {
                mMessages.add(mwssage);
                mMessagesAdapter.notifyDataSetChanged();
                mEditTextMessage.getText().clear();
                if (ENABLE_ASYNC_REQUESTS) {
                    getHistory();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getHistorySync();
                        }
                    }).start();
                }
                Log.e(TAG, "onMessageSend");
            }
        });
    }

    private void deleteChat(WMChat chat) {
        showProgressDialog();
        mWMSession.deleteChat(chat, new WMOfflineSession.OnChatDeletedListener() {

            @Override
            public void onChatDeleted(boolean successful, WMBaseSession.WMSessionError error) {
                Log.i(TAG, "onChatDeleted");
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
                dismissProgressDialog();
            }
        });
    }
//******************* END OF WEBIM-SDK-OFFLINE-CHATS INTERACTION METHODS ******************************/

    public void init(WMChat chat, WMOfflineSession wmSession) {
        mChat = chat;
        mWMSession = wmSession;
        mRefreshHandler.postDelayed(mRefreshRunnable, REFRESH_PERIOD);
    }

//******************* BEGINING OF WEBIM-SDK-OFFLINE-CHATS SYNC INTERACTION METHODS ******************************/

    public void getHistorySync() throws NetworkOnMainThreadException {
        try {
            WMHistoryChanges changes = mWMSession.getHistoryForcedSync(false);
            makeActionWithChanges(changes);
        } catch (WMException wmException) {
            wmException.printStackTrace();
        }
    }

    public void sendMessageSync() throws NetworkOnMainThreadException {
        try {
            WMMessage message = mWMSession.sendMessageSync("Sync Hello", mChat, null, null);
            //Add message to UI without waiting of History changes
        } catch (WMException wmException) {
            wmException.printStackTrace();
        }
    }

    @Deprecated
    public void sendImageSync() throws NetworkOnMainThreadException {
        try {
            WMMessage message = mWMSession.sendImageSync(bitmapToInputStream(getActivity()),
                    WMBaseSession.WMChatAttachmentImageType.WMChatAttachmentImageJPEG, mChat, null, null);
            //Add message to UI without waiting of History changes
        } catch (WMException wmException) {
            wmException.printStackTrace();
        }
    }

    public void sendFileSync() throws NetworkOnMainThreadException {
        try {
            File file = generateFile();
            FileInputStream fileInputStream = new FileInputStream(file);
            WMMessage message = mWMSession.sendFileSync(fileInputStream, file.getName(), getMimeType(Uri.fromFile(file).toString()), mChat, null, null);
            //Add message to UI without waiting of History changes
        } catch (WMException wmException) {
            wmException.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
//******************* END OF WEBIM-SDK-OFFLINE-CHATS SYNC INTERACTION METHODS ******************************/
}

