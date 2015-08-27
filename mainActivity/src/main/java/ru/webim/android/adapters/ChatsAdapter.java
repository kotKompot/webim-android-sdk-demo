package ru.webim.android.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import ru.webim.android.items.WMChat;
import ru.webim.demo.client.R;

public class ChatsAdapter extends BaseAdapter {
    private static final CharSequence DATE_FORMAT = "kk:mm:ss MM/dd/yy";
    private final Context mContext;
    private final List<WMChat> mList;

    public ChatsAdapter(Context context, List<WMChat> chatsList) {
        mContext = context;
        mList = chatsList;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            view = createView();
        }
        updateView(view, mList.get(position));
        return view;
    }

    private View createView() {
        return View.inflate(mContext, android.R.layout.simple_expandable_list_item_1, null);
    }

    private void updateView(View view, WMChat wmChat) {
        TextView headerTextView = (TextView) view.findViewById(android.R.id.text1);
        String dateString = DateFormat.format(DATE_FORMAT, new Date(wmChat.getCreationTs())).toString();
        String lastMessage = "";
        if (wmChat.getMessages() != null && !wmChat.getMessages().isEmpty()) {
            lastMessage = wmChat.getMessages().get(wmChat.getMessages().size() - 1).getMessage();
        }
        headerTextView.setText(mContext.getString(R.string.chat_title, dateString, lastMessage));
    }
}
