package ru.webim.android.fragments;

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

import java.util.ArrayList;

import ru.webim.android.adapters.MessagesAdapter;
import ru.webim.android.items.WMChat;
import ru.webim.android.items.WMHistoryChanges;
import ru.webim.android.items.WMMessage;
import ru.webim.android.sdk.WMBaseSession;
import ru.webim.android.sdk.WMException;
import ru.webim.android.sdk.WMOfflineSession;
import ru.webim.demo.client.R;

public class OfflineChatFragment extends FragmentWithProgressDialog {
    private static final String TAG = "OfflineChatFragment";
    private static final long REFRESH_PERIOD = 10000;
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
                getHistory();
                markChatAsRead();
                mRefreshHandler.postDelayed(this, REFRESH_PERIOD);
            }
        }
    };

    //******************* BEGINNING OF FRAGMENT METHODS *************************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, null);
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

        mMessagesAdapter = new MessagesAdapter(getActivity(), mMessages, mWMSession.getAccountName());
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
                sendImage();
                return true;
            }
        });
    }

    private void sendMessageAction(String message) {
        if (!TextUtils.isEmpty(message)) {
            sendMessage(message);
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
//******************* END OF UI METHODS ******************************/

    //******************* BEGINNING OF WEBIM-SDK-OFFLINE-CHATS INTERACTION METHODS ******************************/
    private void getHistory() {
        mWMSession.getHistoryForced(false, new WMOfflineSession.OnHistoryResponseListener() {

            @Override
            public void onHistoryResponse(boolean successful, WMHistoryChanges changes, WMBaseSession.WMSessionError errorID) {
                Log.w(TAG, "onHistoryResponse");
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
                ;
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

    private void sendImage() {
        mWMSession.sendImage(bitmapToInputStream(getActivity()), WMOfflineSession.WMChatAttachmentImageType.WMChatAttachmentImageJPEG, mChat, null, null, new WMOfflineSession.OnMessageSendListener() {
            @Override
            public void onMessageSend(boolean successful, WMChat chat, WMMessage mwssage, WMOfflineSession.WMSessionError error) {
                Log.d("Image", "onMessageSend " + successful + " " + error);
            }
        });
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
                getHistory();
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
        } catch (WMException wmException) {
            wmException.printStackTrace();
        }
    }

    public void sendMessageSync() throws NetworkOnMainThreadException {
        try {
            WMMessage message = mWMSession.sendMessageSync("Sync Hello", mChat, null, null);
        } catch (WMException wmException) {
            wmException.printStackTrace();
        }
    }

    public void sendImageSync() throws NetworkOnMainThreadException {
        try {
            WMMessage changes = mWMSession.sendImageSync(bitmapToInputStream(getActivity()),
                    WMBaseSession.WMChatAttachmentImageType.WMChatAttachmentImageJPEG, mChat, null, null);
        } catch (WMException wmException) {
            wmException.printStackTrace();
        }
    }
//******************* END OF WEBIM-SDK-OFFLINE-CHATS SYNC INTERACTION METHODS ******************************/
}

