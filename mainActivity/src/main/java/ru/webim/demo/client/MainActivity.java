package ru.webim.demo.client;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;

import com.astuetz.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.List;

import ru.webim.android.fragments.OfflineFragment;
import ru.webim.android.fragments.OnlineChatFragment;

public class MainActivity extends ActionBarActivity {
    private List<Fragment> mList = new ArrayList<Fragment>();
    private OnlineChatFragment mOnlineFragment;
    private OfflineFragment mOfflineFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initFragments();
        initViewPager();
    }

    private void initViewPager() {
        ViewPager viewPager = (ViewPager) findViewById(R.id.help_view_pager);
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        viewPager.setAdapter(new HelpViewPagerFragmentAdapter(
                getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(mList.size());
        tabs.setViewPager(viewPager);
    }

    private void initFragments() {
        mOnlineFragment = new OnlineChatFragment();
        mOfflineFragment = new OfflineFragment();
    }

    @Override
    protected void onStart() {
        WebimSDKApplication.setIsInBackground(false);
        super.onStart();
    }

    @Override
    protected void onStop() {
        WebimSDKApplication.setIsInBackground(true);
        super.onStop();
    }

    private class HelpViewPagerFragmentAdapter extends FragmentPagerAdapter {

        public HelpViewPagerFragmentAdapter(FragmentManager fm) {
            super(fm);
            mList.add(mOnlineFragment);
            mList.add(mOfflineFragment);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Online Chat";
                case 1:
                    return "Offline Chats";
                default:
                    return "";
            }
        }

        @Override
        public Fragment getItem(int position) {
            return mList.get(position);
        }

        @Override
        public int getCount() {
            return mList.size();
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
