package ru.webim.android.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import ru.webim.android.adapters.ChatsAdapter;
import ru.webim.android.items.WMChat;
import ru.webim.android.items.WMHistoryChanges;
import ru.webim.android.items.WMMessage;
import ru.webim.android.sdk.OnHistoryResponseListener;
import ru.webim.android.sdk.WMBaseSession;
import ru.webim.android.sdk.WMException;
import ru.webim.android.sdk.WMOfflineSession;
import ru.webim.demo.client.R;

public class OfflineFragment extends FragmentWithProgressDialog {
    public static final String ACCOUNT_NAME = "demo";
    private static final String TAG = "OfflineFragment";
    private WMOfflineSession mWMOfflineSession;
    private BaseAdapter mChatsAdapter;
    private List<WMChat> mChatsList = new ArrayList<WMChat>();
    private boolean mIsAsync = false;

    //******************* BEGINNING OF FRAGMENT METHODS *************************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.offline_chats_layout, null);
        initListView(v);
        initWebimOfflineSession();
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh();
    }
//******************* END OF FRAGMENT METHODS *************************/

    //******************* BEGINNING OF UI METHODS ******************************/
    private void initListView(View v) {
        ListView listView = (ListView) v.findViewById(R.id.listViewChats);
        initStartChatButton(v);
        mChatsAdapter = new ChatsAdapter(getActivity(), mChatsList);
        listView.setAdapter(mChatsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                WMChat chat = (WMChat) adapterView.getItemAtPosition(position);
                OfflineChatFragment chatFragment = new OfflineChatFragment();
                chatFragment.init(chat, mWMOfflineSession);

                showChatFragment(chatFragment);
            }
        });
    }

    private void initStartChatButton(View v) {
        Button button = (Button) v.findViewById(R.id.buttonStartOfflineChat);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgressDialog();
                mWMOfflineSession.sendMessage("Hello", null, null, null, new WMOfflineSession.OnMessageSendListener() {
                    @Override
                    public void onMessageSend(boolean successful, WMChat chat, WMMessage message, WMBaseSession.WMSessionError error) {
                        if (successful) {
                            getRequestChatList();
                        } else {
                            dismissProgressDialog();
                        }
                    }
                });
            }
        });
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showProgressDialog();
                if (mIsAsync) {
                    mWMOfflineSession.sendFile(bitmapToInputStream(getActivity()), "moon.jpg", "image/jpeg", null, null, null, new WMOfflineSession.OnMessageSendListener() {
                        @Override
                        public void onMessageSend(boolean successful, WMChat chat, WMMessage message, WMBaseSession.WMSessionError error) {
                            if (successful) {
                                getRequestChatList();
                            } else {
                                dismissProgressDialog();
                            }
                        }
                    });
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mWMOfflineSession.sendFileSync(bitmapToInputStream(getActivity()), "moon.jpg", "image/jpeg", null, null, null);
                            } catch (WMException e) {
                                e.printStackTrace();
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getRequestChatList();
                                }
                            });
                        }
                    }).start();
                }
                return true;
            }
        });
    }

    private void showChatFragment(OfflineChatFragment chatFragment) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.root_view, chatFragment).addToBackStack(null);
        ft.commitAllowingStateLoss();
        fm.executePendingTransactions();
    }

    private void updateList() {
        mChatsList.clear();
        mChatsList.addAll(mWMOfflineSession.getOfflineChats());
        mChatsAdapter.notifyDataSetChanged();
    }

    @Override
    protected void refresh() {
        getRequestChatList();
    }

    @Override
    protected void discard() { //Delete all offline chats.
        if (updateWorkaround_PLEASE_DONT_COPY()) { // TODO Don't do anything like this - Workaround for call discard only in "child" fragment.
            refresh();
            return;
        }
        showProgressDialog();
        mWMOfflineSession.deleteChats(mWMOfflineSession.getOfflineChats(), new WMOfflineSession.OnChatDeletedListener() {
            @Override
            public void onChatDeleted(boolean successful, WMBaseSession.WMSessionError error) {
                updateList();
                dismissProgressDialog();
            }
        });
    }

    private boolean updateWorkaround_PLEASE_DONT_COPY() {
        return getFragmentManager().getBackStackEntryCount() > 0;
    }
//******************* END OF UI METHODS ******************************/

    //******************* BEGINNING OF MAIN INIT WEBIM-SDK-OFFLINE-CHATS METHOD ******************************/
    private void initWebimOfflineSession() {
        new Handler().postDelayed(new Runnable() {
            // Hander need for correct init both sessions in current sample (online and offline).
            // If you want to use only offline session you can use it without handler (same for only online session).
            // Also, if you want to use both sessions you can use serial init (example in OnlineChatFragment, method "createSession").
            @Override
            public void run() {
                mWMOfflineSession = new WMOfflineSession(getActivity(), ACCOUNT_NAME, "mobile", "android", true);
                getRequestChatList();
            }
        }, 5000);
    }
//******************* END OF MAIN INIT WEBIM-SDK-OFFLINE-CHATS METHOD ******************************/


    private void getRequestChatList() {
        showProgressDialog();
        if (mWMOfflineSession != null) {
            mWMOfflineSession.getHistoryForced(false, new OnHistoryResponseListener() {

                @Override
                public void onHistoryResponse(boolean successful, WMHistoryChanges changes, WMOfflineSession.WMSessionError errorID) {
                    Log.w(TAG, "onHistoryResponse = " + successful + " error = " + errorID);
                    if (changes != null) {
                        if (!changes.getNewChats().isEmpty()) {
                            Log.d(TAG, "New Chats = " + changes.getNewChats().size());
                        }

                        if (!changes.getMessages().isEmpty()) {
                            Log.d(TAG, "New Messages = " + changes.getMessages().size());
                        }
                        if (!changes.getModifiedChats().isEmpty()) {
                            Log.d(TAG, "ModifiedChats = " + changes.getModifiedChats().size());
                        }
                    }
                    updateList();
                    dismissProgressDialog();
                }
            });
        }
    }
}