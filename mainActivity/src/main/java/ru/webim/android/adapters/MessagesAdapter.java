package ru.webim.android.adapters;

import java.util.ArrayList;

import ru.webim.android.items.WMMessage;
import ru.webim.android.items.WMMessage.WMMessageKind;
import ru.webim.android.sdk.WMOfflineSession;
import ru.webim.demo.client.R;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;

public class MessagesAdapter extends BaseAdapter {
    private final String mAccountName;
    private LayoutInflater mInflater;
	private ArrayList<WMMessage> mList = new ArrayList<WMMessage>();
	private Context mContext;

	public MessagesAdapter(Context context, ArrayList<WMMessage> arrayList, String accountName) {
		mContext = context;
		mList = arrayList;
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAccountName = accountName;
	}

    @Override
	public View getView(final int position, final View convertView,	final ViewGroup parent) {
		final WMMessage currentItem = mList.get(position);
		int layoutId = getLayout(currentItem.getType());
		
		View view = mInflater.inflate(layoutId, parent, false);
		TextView messageTextView = (TextView) view.findViewById(R.id.textViewMessage);
		
		switch (currentItem.getType()) {
		case WMMessageKindFileFromOperator:
		case WMMessageKindFileFromVisitor:
            String url = currentItem.getParams(); //GET IMAGE FORM ONLINE CHAT
            if (TextUtils.isEmpty(url)) { //This mean Image is in offline chat
                try {
                    url = WMOfflineSession.getUrlFromMessage(currentItem, mAccountName); //GET IMAGE FORM OFFLINE CHAT
                } catch (JSONException e) {
                    e.printStackTrace(); //Wrong Image Uri.
                }
            }
            setMessageAsLink(view, messageTextView, currentItem.getMessage(), url);
			break;
		default:
			messageTextView.setText(currentItem.getMessage());
			break;
		}
		switch (currentItem.getStatus()) {
		case Sent:
			messageTextView.setTextColor(Color.BLACK);
			break;
		case NotSent:
			messageTextView.setTextColor(Color.RED);
			break;
		case IsSending:
			messageTextView.setTextColor(Color.BLUE);
			break;
		}
		return view;
	}

	private void setMessageAsLink(final View view, final TextView textView, final String text, final String url) {
		textView.setText( Html.fromHtml(mContext.getResources().getString(R.string.file_send) +"<a href=\""+url+"\">"+text+"</a>"));
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		ImageView redirect = (ImageView) view.findViewById(R.id.imageViewOpenFile);
		redirect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                if (url!=null) {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    mContext.startActivity(intent);
                }
			}
		});
	}

	private int getLayout(WMMessageKind wmMessageKind) {
		int layoutId = R.layout.item_system_message;
		switch (wmMessageKind) {
		case WMMessageKindOperator:
			layoutId = R.layout.item_remote_message;
			break;
		case WMMessageKindVisitor:
			layoutId = R.layout.item_local_message;
			break;
		case WMMessageKindInfo:
			layoutId = R.layout.item_system_message;
			break;
		case WMMessageKindOperatorBusy:
			layoutId = R.layout.item_time_message;
			break;
		case WMMessageKindFileFromVisitor:
			layoutId = R.layout.item_send_file;
			break;
		case WMMessageKindFileFromOperator:
			layoutId = R.layout.item_recieve_file;
			break;
		default:
			layoutId = R.layout.item_system_message;
			break;
		}
		return layoutId;
	}

	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		try {
			return mList.get(position);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}