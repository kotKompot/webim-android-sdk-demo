package ru.webim.android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ru.webim.android.adapters.MessagesAdapter;
import ru.webim.android.items.WMChat;
import ru.webim.android.items.WMMessage;
import ru.webim.android.items.WMOperator;
import ru.webim.android.items.WMOperatorRate;
import ru.webim.android.sdk.WMBaseSession;
import ru.webim.android.sdk.WMSession;
import ru.webim.demo.client.R;


public class OnlineChatFragment extends FragmentWithProgressDialog {

    private WMSession mWMSession;

    private MessagesAdapter mMessagesAdapter;
    private ArrayList<WMMessage> mMessages = new ArrayList<WMMessage>();

    private EditText mEditTextMessage;

    private AlertDialog mAlertDialogClose;
    private AlertDialog mAlertDialogStart;

    //******************* BEGINNING OF FRAGMENT METHODS *************************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, null);

        initWebimChat(); // Main part of initialization WebimOnlineChat.

        initListView(v);
        initEditText(v);
        initSendButton(v);

        mAlertDialogClose = createCloseDialog();
        mAlertDialogStart = createStartDialog();
        return v;
    }

    @Override
    public void onDestroy() {
        if (mWMSession != null)
            mWMSession.stopSession();
        super.onDestroy();
    }
//******************* END OF FRAGMENT METHODS *************************/


    private void fillListView() {
        if (mWMSession != null && mWMSession.getChat() != null)
            mMessages.addAll(mWMSession.getChat().getMessages());
        if (mMessagesAdapter != null)
            mMessagesAdapter.notifyDataSetChanged();
    }

    private void initListView(View v) {
        ListView listViewChat = (ListView) v.findViewById(R.id.listViewChat);
        listViewChat.setEmptyView(createEmptyTextView(listViewChat));

        mMessagesAdapter = new MessagesAdapter(getActivity(), mMessages, mWMSession.getUrl(), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mWMSession != null && view.getTag() instanceof WMMessage) {
                    WMMessage item = (WMMessage) view.getTag();
                    showRateDialog(item);
                }
            }
        });
        listViewChat.setAdapter(mMessagesAdapter);
        mMessagesAdapter.notifyDataSetChanged();

        fillListView();
    }

    private View createEmptyTextView(ListView listViewChat) {
        View emptyChatView = View.inflate(getActivity(), R.layout.spinner_empty_view, null);
        ((ViewGroup) listViewChat.getParent()).addView(emptyChatView);
        LayoutParams params = emptyChatView.getLayoutParams();
        params.height = LayoutParams.MATCH_PARENT;
        params.width = LayoutParams.MATCH_PARENT;
        emptyChatView.setLayoutParams(params);
        return emptyChatView;
    }

    private void initSendButton(View v) {
        ImageButton sendButton = (ImageButton) v.findViewById(R.id.imageButtonSendMessage);
        sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWMSession == null)
                    return;
                String message = mEditTextMessage.getText().toString();
                sendMessageAction(message);
            }
        });
        sendButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
//                sendImageAction();
                sendFileAction();
                return false;
            }
        });
    }

    private void sendMessageAction(String message) {
        if (!TextUtils.isEmpty(message)) {
            requestSendMessage(message, new WMSession.OnMessageSendListener() {
                @Override
                public void onMessageSend(boolean success) { // This callback is just approve of success request. It can be null.
                    // See sessionDidReceiveMessage in WMSessionDelegate
                    Log.i("onMessageSend", "success - " + success);
                    mEditTextMessage.getText().clear();
                }
            });
        }
    }

    @Deprecated
    private void sendImageAction() {
        requestSendImage(bitmapToInputStream(getActivity()), new WMSession.OnImageUploadListener() {
            @Override
            public void onImageUploaded(boolean successful) {
                Log.i("on ImageUploaded", "success - " + successful);
            }
        });
    }

    private void sendFileAction() {
        File file = generateFile();
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            requestSendFile(fileInputStream, file.getName(), getMimeType(Uri.fromFile(file).toString()), new WMSession.OnFileUploadListener() {
                @Override
                public void onFileUploaded(boolean successful) {
                    Log.i("onFileUploaded", "success - " + successful);
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public File generateFile() {
        File file = new File(getActivity().getFilesDir(), "test_text_file.txt");
        try {
            FileWriter writer = new FileWriter(file);
            writer.append("Test txt file for Webim client demo app");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private void initEditText(View v) {
        mEditTextMessage = (EditText) v.findViewById(R.id.editTextChatMessage);
        mEditTextMessage.addTextChangedListener(new TextWatcher() {
            public boolean mIsTyping;
            public WMSession.OnSendTypingStatusListener mListener = new WMSession.OnSendTypingStatusListener() {
                @Override
                public void onTypingStatusSend(boolean successful) {
                    Log.i("onTypingStatusSend", "success - " + successful);
                }
            };
            Handler mHandler = new Handler();
            Runnable mStopPrintRunnable = new Runnable() {
                @Override
                public void run() {
                    mIsTyping = false;
                    requestSendTypingStatus(false, mListener);
                }
            };

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mHandler.removeCallbacks(mStopPrintRunnable);
                mHandler.postDelayed(mStopPrintRunnable, 5000);
                if (!mIsTyping) {
                    mIsTyping = true;
                    requestSendTypingStatus(true, mListener);
                }
            }
        });
    }

    public void setOperatorParams(WMOperator operatorItem) {
        if (operatorItem != null) {
            String avatar = operatorItem.getAvatar();
            String name = operatorItem.getFullname();
            Toast.makeText(getActivity(), getActivity().getString(R.string.toast_operator_connected, name), Toast.LENGTH_LONG).show();
            Log.i("Online Chat", "Operator avatar url - " + avatar);
        } else {
            Toast.makeText(getActivity(), getString(R.string.toast_no_operator), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void refresh() {
        requestUpdateWithCompletionBlock(new WMSession.WMRefreshFinishDelegate() {
            @Override
            public void sessionDidRefresh() {
                Log.i("sessionDidRefresh", "Last data was received");
            }
        });
    }

    @Override
    protected void discard() {
        showRequestDialog(mAlertDialogClose);
    }


    private void showRateDialog(final WMMessage item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Rate " + item.getSenderName() + " ?");
        View view = View.inflate(getActivity(), R.layout.rating_bar, null);
        final RatingBar bar = (RatingBar) view.findViewById(R.id.ratingBar);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        dialog.show();
        bar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, final float v, boolean b) {
                dialog.dismiss();
                requestRateOperator(item, v, new WMSession.OnRateOperationListener() {
                    @Override
                    public void onRateOperator(boolean successful) {
                        Log.d("onRateOperator()", "successful = [" + successful + "]");
                        if (getActivity() != null && successful) {
                            Toast.makeText(getActivity(), "You set " + (int) v + " stars to " + item.getSenderName(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }


//******************* END OF UI METHODS ******************************/

    //******************* BEGINNING OF WEBIM-SDK-ONLINE-CHATS INTERACTION METHODS ******************************/
    private void setMessages(List<WMMessage> list) {
        this.mMessages.clear();
        mMessages.addAll(list);
        if (mMessagesAdapter != null)
            mMessagesAdapter.notifyDataSetChanged();
    }

    private void addMessage(WMMessage message) {
        mMessages.add(message);
        if (mMessagesAdapter != null)
            mMessagesAdapter.notifyDataSetChanged();
    }

    private void clearMessages() {
        mMessages.clear();
        if (mMessagesAdapter != null)
            mMessagesAdapter.notifyDataSetChanged();
    }

    private void start() {
        if (mWMSession != null)
            mWMSession.startSession();
    }

    private void requestStartChat(WMSession.OnInitListener listener) {
        if (mWMSession != null)
            mWMSession.startChat(listener);
    }

    private void requestCloseDialog(WMSession.OnCloseDialogByVisitorListener listener) {
        if (mWMSession != null)
            mWMSession.closeChat(listener);
    }

    private void requestSendMessage(String message, WMSession.OnMessageSendListener listener) {
        if (mWMSession != null)
            mWMSession.sendMessage(message, listener);
    }

    // Request Full data about session from creation. No need to call in standard workflow.
    private void startFullUpdate() {
        if (mWMSession != null)
            mWMSession.fullUpdate();
    }

    @Deprecated
    private void requestSendImage(InputStream inputStream, WMSession.OnImageUploadListener listener) {
        if (mWMSession != null)
            mWMSession.sendImage(inputStream, WMBaseSession.WMChatAttachmentImageType.WMChatAttachmentImageJPEG, listener);
    }

    private void requestSendFile(InputStream inputStream, String name, String mime, WMSession.OnFileUploadListener listener) {
        if (mWMSession != null)
            mWMSession.sendFile(inputStream, name, mime, listener);
    }

    // Request Last data about session. No need to call in standard workflow.
    private boolean requestUpdateWithCompletionBlock(WMSession.WMRefreshFinishDelegate delegate) {
        if (mWMSession != null)
            return mWMSession.refreshSessionWithCompletionBlock(delegate);
        return false;
    }

    private void requestRateOperator(WMMessage item, float v, WMSession.OnRateOperationListener listener) {
        if (mWMSession != null)
            mWMSession.rateOperator(item.getSenderId(), WMOperatorRate.fromValue((int) (v - 3)), listener);
    }

    private void requestSendTypingStatus(boolean isTyping, WMSession.OnSendTypingStatusListener listener) {
        if (mWMSession != null)
            mWMSession.sendTypingStatus(isTyping, listener);
    }

//******************* END OF WEBIM-SDK-ONLINE-CHATS INTERACTION METHODS ******************************/


    //******************* BEGINNING OF MAIN INIT WEBIM-SDK-ONLINE-CHATS METHOD ******************************/
    private void initWebimChat() {
        WMSession.WMSessionDelegate delegate = new WMSession.WMSessionDelegate() {

            private String TAG = "WMSessionDelegate";

            private void restoreChat(WMSession sessionItem) {
                if (sessionItem.hasChat()) {
                    setMessages(sessionItem.getChat().getMessages());
                    setOperatorParams(sessionItem.getChat().getOperator());
                } else {
                    clearMessages();
                    setOperatorParams(null);
                    requestStartChat(new WMSession.OnInitListener() {
                        @Override
                        public void onInit(boolean successful) {
                            Log.i(TAG, "onInit(...)");
                        }
                    });
                }
            }

            @Override
            public void sessionDidReceiveFullUpdate(WMSession session) {
                Log.i(TAG, "onFullUpdate(...) - " + session.getPageId());
                restoreChat(session);
            }

            @Override
            public void sessionDidChangeSessionStatus(WMSession session) {
                WMSession.WMSessionState sessionState = session.getState();
                if (sessionState.equals(WMSession.WMSessionState.WMSessionStateOfflineMessage)) {
                    Toast.makeText(getActivity(), R.string.error_no_operators, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void sessionDidStartChat(WMSession session, WMChat chat) {
                if (chat != null) {
                    switch (chat.getState()) {
                        case WMChatStateClosedByOperator:
                        case WMChatStateChatting:
                        case WMChatStateQueue:
                        case WMChatStateInvitation:
                            setMessages(chat.getMessages());
                            setOperatorParams(chat.getOperator());
                            break;

                        default:
                            clearMessages();
                            break;
                    }
                } else {
                    clearMessages();
                    setOperatorParams(null);
                }
            }

            @Override
            public void sessionDidChangeChatStatus(WMSession session) {
                switch (session.getChat().getState()) {
                    case WMChatStateClosed:
                    case WMChatStateClosedByVisitor:
                        Toast.makeText(getActivity(), getString(R.string.toast_chat_closed), Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Toast.makeText(getActivity(), getString(R.string.toast_chat_opened), Toast.LENGTH_LONG).show();
                        break;
                }
                dismissProgressDialog();
            }

            @Override
            public void sessionDidUpdateOperator(WMSession session, WMOperator chatOperator) {
                Log.i(TAG, "onOperator(...)");
                setOperatorParams(chatOperator);
            }

            @Override
            public void sessionDidReceiveMessage(WMSession session, WMMessage message) {
                Log.i(TAG, "onMessage(...) - " + message.getMessage());
                addMessage(message);
            }

            @Override
            public void sessionDidReceiveError(WMSession wmSession, WMBaseSession.WMSessionError wmSessionError) {
                Log.e(TAG, "onError() - " + wmSessionError);
                String errorMessage;
                switch (wmSessionError) {
                    case WMSessionErrorGCMError:
                        Toast.makeText(getActivity(), getString(R.string.toast_gsm_error), Toast.LENGTH_LONG).show();
                        break;
                    case WMSessionErrorAccountBlocked:
                        errorMessage = getString(R.string.error_account_blocked);
                        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
                        break;
                    case WMSessionErrorNetworkError:
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                            }
                        });
                    default:
                        break;
                }
            }

        };
        createSession(delegate);
    }

    private void createSession(WMSession.WMSessionDelegate delegate) {
        mWMSession = new WMSession(getActivity(), "webimru069", "mobile", delegate);
        start();
    }
//******************* END OF MAIN INIT WEBIM-SDK-ONLINE-CHATS METHOD ******************************/

    //******************* BEGINNING OF DIALOGS INIT METHODS ******************************/
    private AlertDialog createStartDialog() {
        return createStartDialog(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showProgressDialog();
                requestStartChat(new WMSession.OnInitListener() {
                    @Override
                    public void onInit(boolean b) { // This callback is just approve of success request. It can be null.
                        // See sessionDidChangeChatStatus in WMSessionDelegate
                        Log.i("onInit", "chat opened");
                    }
                });
            }
        });
    }

    private AlertDialog createCloseDialog() {
        return createCloseDialog(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showProgressDialog();
                requestCloseDialog(new WMSession.OnCloseDialogByVisitorListener() {

                    @Override
                    public void onCloseDialogByVisitor(boolean successful) { // This callback is just approve of success request. It can be null.
                        // See sessionDidChangeChatStatus in WMSessionDelegate
                        Log.i("onCloseDialogByVisitor", "chat closed");
                        showRequestDialog(mAlertDialogStart);
                    }
                });
            }
        });
    }
//******************* END OF DIALOGS INIT METHODS ******************************/
}