package de.robv.android.xposed.installer;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Arijeet on 10/30/2017.
 */

public class PermissionManagerFragment extends Fragment{

    private TextView mTxtLog;
    private ScrollView mSVLog;
    private HorizontalScrollView mHSVLog;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_logs, container, false);
        mTxtLog = (TextView) v.findViewById(R.id.txtLog);
        mTxtLog.setTextIsSelectable(true);
        mSVLog = (ScrollView) v.findViewById(R.id.svLog);
        mHSVLog = (HorizontalScrollView) v.findViewById(R.id.hsvLog);
        try {
            PermissionManagerUtil.logFile.createNewFile(); //create log file if donot exsist
        } catch (IOException e) {
            e.printStackTrace();
        }
        readLogs();
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_perms, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_config){
            startActivity(new Intent(getActivity(), PermissionListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void readLogs() {
        new LogsReader().execute(PermissionManagerUtil.logFile);
        mSVLog.post(new Runnable() {
            @Override
            public void run() {
                mSVLog.scrollTo(0, mTxtLog.getHeight());
            }
        });
        mHSVLog.post(new Runnable() {
            @Override
            public void run() {
                mHSVLog.scrollTo(0, 0);
            }
        });
    }

    private class LogsReader extends AsyncTask<File, Integer, String> {

        private static final int MAX_LOG_SIZE = 1000 * 1024; // 1000 KB
        private MaterialDialog mProgressDialog;

        private long skipLargeFile(BufferedReader is, long length) throws IOException {
            if (length < MAX_LOG_SIZE)
                return 0;

            long skipped = length - MAX_LOG_SIZE;
            long yetToSkip = skipped;
            do {
                yetToSkip -= is.skip(yetToSkip);
            } while (yetToSkip > 0);

            int c;
            do {
                c = is.read();
                if (c == -1)
                    break;
                skipped++;
            } while (c != '\n');

            return skipped;

        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = new MaterialDialog.Builder(getActivity()).content(R.string.loading).progress(true, 0).show();
        }

        @Override
        protected String doInBackground(File... log) {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);

            StringBuilder llog = new StringBuilder(15 * 10 * 1024);
            try {
                File logfile = log[0];
                BufferedReader br;
                br = new BufferedReader(new FileReader(logfile));
                long skipped = skipLargeFile(br, logfile.length());
                if (skipped > 0) {
                    llog.append("-----------------\n");
                    llog.append("Log too long");
                    llog.append("\n-----------------\n\n");
                }

                char[] temp = new char[1024];
                int read;
                while ((read = br.read(temp)) > 0) {
                    llog.append(temp, 0, read);
                }
                br.close();
            } catch (IOException e) {
                llog.append("Cannot read log");
                llog.append(e.getMessage());
            }

            return llog.toString();
        }

        @Override
        protected void onPostExecute(String llog) {
            mProgressDialog.dismiss();
            mTxtLog.setText(llog);

            if (llog.length() == 0)
                mTxtLog.setText(R.string.log_is_empty);
        }

    }

}
