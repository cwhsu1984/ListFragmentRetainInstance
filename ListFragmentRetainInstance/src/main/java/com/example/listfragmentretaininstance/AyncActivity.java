package com.example.listfragmentretaininstance;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class AyncActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use UiFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, new UiFragment()).commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public static class UiFragment extends Fragment {
        RetainFragment mWorkFragment;
        ListView mListView;
        boolean mReady = false;

        final Handler mHandler = new Handler();

        // Runnable to update result of ListView.
        final Runnable mUpdateResults = new Runnable() {
            public void run() {
                // Sometimes, the WorkFragment tries to update when UIFragment is not ready, and hence, getActivity()
                // retruns null. Use mReady to make sure that the activity is ready for UI update.
                if (!mReady)
                    return;
                mListView.setAdapter(
                        new ArrayAdapter<String>(
                                getActivity(),
                                android.R.layout.simple_expandable_list_item_1,
                                mWorkFragment.getData()));
            }
        };

        // Update UI function.
        public void updateUI() {
            mHandler.post(mUpdateResults);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup contrainer,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_retain_instance, contrainer, false);
            mListView = (ListView) v.findViewById(R.id.listView);
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            mReady = true;
             // Find WorkFragment.
            FragmentManager fm = getFragmentManager();
            mWorkFragment = (RetainFragment) fm.findFragmentByTag("work");

            // Create a new WorkFragment if it doesn't exist, otherwise, update UI.
            if (mWorkFragment == null) {
                mWorkFragment = new RetainFragment();
                mWorkFragment.setTargetFragment(this, 0);
                fm.beginTransaction().add(mWorkFragment, "work").commit();
            } else {
                updateUI();
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            mReady = false;
        }
    }

    public static class RetainFragment extends Fragment {
        MyTask mTask = new MyTask();

        public ArrayList<String> getData() {
            return mTask.getData();
        }

        class MyTask extends AsyncTask<Void, Void, Void> {
            String data[] = {"111", "222", "333", "444", "555", "666", "777", "888", "999"};
            ArrayList<String> mock = new ArrayList<String>();
            int i = 0;

            public ArrayList<String> getData() {
                return mock;
            }

            @Override
            protected Void doInBackground(Void... arg) {
                try {
                    while (mock.size() < data.length) {
                        synchronized (this) {
                            wait(1000);
                        }
                        mock.add(data[i++]);
                        publishProgress();
                    }
                } catch (InterruptedException e) {
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Void... arg) {
                // Tell UiFragment to update UI.
                UiFragment fragment = (UiFragment) getTargetFragment();
                fragment.updateUI();
            }
        }

        @Override
        public void onCreate(Bundle savedInstance) {
            super.onCreate(savedInstance);
            // Set fragment to be retained across activity-recreation.
            setRetainInstance(true);

            mTask.execute();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mTask.cancel(true);
        }
    }
}
