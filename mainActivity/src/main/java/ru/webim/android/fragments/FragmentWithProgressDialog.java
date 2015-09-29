package ru.webim.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import ru.webim.demo.client.R;

public abstract class FragmentWithProgressDialog extends Fragment {

    private ProgressDialog mProgressDialog;

    private void initProgressDialog(Context context) {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMessage(getString(R.string.progress_message));
        mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
    }

    public void showProgressDialog() {
        if (mProgressDialog != null
                && !mProgressDialog.isShowing())
            mProgressDialog.show();
    }

    public void dismissProgressDialog() {
        if (mProgressDialog != null
                && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }

    @Override
    public void onAttach(Activity activity) {
        initProgressDialog(activity);
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        dismissProgressDialog();
        super.onDetach();
    }

    public void showRequestDialog(AlertDialog alertDialog) {
        if (!alertDialog.isShowing())
            alertDialog.show();
    }

    //******************* START DIALOGS INIT METHODS ******************************/
    protected AlertDialog createStartDialog(DialogInterface.OnClickListener listener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setMessage(R.string.dialog_alert_start_dialog)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes,listener);
        return dialogBuilder.create();
    }

    protected AlertDialog createCloseDialog(DialogInterface.OnClickListener listener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setMessage(R.string.dialog_alert_close_dialog)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, listener);
        return dialogBuilder.create();
    }
//******************* END DIALOGS INIT METHODS ******************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_discard:
                if (isVisible()) discard();
                return false;
            case R.id.action_refresh:
                if (isVisible()) refresh();
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Deprecated
    public static InputStream bitmapToInputStream(Context context) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.moon);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return new ByteArrayInputStream(stream.toByteArray());
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

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    protected abstract void refresh();

    protected abstract void discard();
}
