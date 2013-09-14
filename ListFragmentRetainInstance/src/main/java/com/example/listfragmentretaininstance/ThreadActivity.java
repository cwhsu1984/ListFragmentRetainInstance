package com.example.listfragmentretaininstance;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class ThreadActivity extends FragmentActivity {

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
        RetainedFragment mWorkFragment;
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
            mWorkFragment = (RetainedFragment) fm.findFragmentByTag("work");

            // Create a new WorkFragment if it doesn't exist, otherwise, update UI.
            if (mWorkFragment == null) {
                mWorkFragment = new RetainedFragment();
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

    public static class RetainedFragment extends Fragment {
        String data[] = {"AAA", "BBB", "CCC", "DDD", "EEE", "FFF", "GGG", "HHH", "III", "JJJ", "KKK"};
        ArrayList<String> mock = new ArrayList<String>();
        boolean mReady = false;
        boolean mQuiting = false;

        // Get mcok data.
        public ArrayList<String> getData() {
            return mock;
        }

        final Thread mThread = new Thread() {
            @Override
            public void run() {
                int i = 0;
                while(true) {
                    synchronized (this) {
                        // When activity is not ready or all the data is added to the mock.
                        while (!mReady || mock.size() >= data.length) {
                            // See if we need to quit or keep waiting.
                            if (mQuiting) {
                                return;
                            }
                            try {
                                wait();
                            } catch (InterruptedException e) {
                            }
                        }
                        // Add data to mock and update UI.
                        mock.add(data[i++]);
                        UiFragment fragment = (UiFragment) getTargetFragment();
                        fragment.updateUI();
                    }

                    synchronized (this) {
                        try {
                            wait(3000);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        };

        @Override
        public void onCreate(Bundle savedInstance) {
            super.onCreate(savedInstance);

            // Set fragment to be retained across activity-recreation.
            setRetainInstance(true);

            mThread.start();
        }

        @Override
        public void onActivityCreated(Bundle savedInstance) {
            super.onActivityCreated(savedInstance);
            synchronized (mThread) {
                mReady = true;
                mThread.notify();
            }
        }

        @Override
        public void onDestroy() {
            synchronized (mThread) {
                mReady = false;
                mQuiting = true;
                mThread.notify();
            }
            super.onDestroy();
        }
    }
}
