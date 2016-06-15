package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.List;
import java.util.Map;

import static com.example.xyzreader.R.id.pager;
import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_CURRENT_ALBUM_POSITION;
import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_CURRENT_TRANSITION_NAME;
import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_STARTING_ALBUM_POSITION;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;


    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;

    private Toolbar mToolbar;

    private static final String STATE_CURRENT_PAGE_POSITION = "state_current_page_position";
    private static final String STATE_CURRENT_TRANSITION_NAME = "state_current_transition_name";

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mIsReturning) {
                ImageView sharedElement = mCurrentDetailsFragment.getThumbnailImage();
                if (sharedElement == null)
                {
                    // If shared element is null, then it has been scrolled off screen and
                    // no longer visible. In this case we cancel the shared element transition by
                    // removing the shared element from the shared elements map.
                    names.clear();
                    sharedElements.clear();
                } else {
                    /* As the staggered grid adapter is using Picasso to load the images.
                     * This poses a problem- the animation will be broken or postponed until
                     * Picasso loads the images.
                     * If the shared element was the thumbnail, then it is possible to pass it
                     * as a shared element because it will be loaded in memory for both views.
                     * In the current setup, the same image is loaded once as a thumbnail and once
                     * as a full-bleed image for the detail view. They are different objects and need
                     * to be loaded from Picasso.
                     *
                     * For that reason the return shared element animation is removed. Alternatively
                     * if the positions are the same, the shared element can be passed as it is unlikely
                     * that the thumbnail would be removed from memory.*/
                    //if (mStartingPosition != mCurrentPosition) {
                    // If the user has swiped to a different ViewPager page, then we need to
                    // remove the old shared element and replace it with the new shared element
                    // that should be transitioned instead.
                    names.clear();

                    //names.add(sharedElement.getTransitionName());
                    sharedElements.clear();
                    //sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                }
            }
        }
    };


    private long mStartID;
    private int mCurrentPosition;
    private int mStartingPosition;
    private String mTransitionName;
    private boolean mIsReturning;
    ArticleDetailFragment mCurrentDetailsFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_article_detail);
        postponeEnterTransition();
        setEnterSharedElementCallback(mCallback);


        mToolbar = (Toolbar) this.findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mStartingPosition = getIntent().getIntExtra(EXTRA_STARTING_ALBUM_POSITION, 0);

        if (savedInstanceState == null)
        {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartID = ItemsContract.Items.getItemId(getIntent().getData());

                mCurrentPosition = mStartingPosition;
            }
        } else {
            mTransitionName  = savedInstanceState.getString(STATE_CURRENT_TRANSITION_NAME);
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_PAGE_POSITION);
        }

        getLoaderManager().initLoader(0, null, this);

        mPager = (ViewPager) findViewById(pager);
        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mCurrentPosition = position;
                mTransitionName = mPagerAdapter.getTransitionName(position);
            }
        });

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE_POSITION, mCurrentPosition);
        outState.putString(STATE_CURRENT_TRANSITION_NAME,mTransitionName);
    }

    @Override
    public void finishAfterTransition() {
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(EXTRA_CURRENT_TRANSITION_NAME, mTransitionName);
        data.putExtra(EXTRA_STARTING_ALBUM_POSITION, mStartingPosition);
        data.putExtra(EXTRA_CURRENT_ALBUM_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent intent = NavUtils.getParentActivityIntent(this);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(EXTRA_STARTING_ALBUM_POSITION, mStartingPosition);
                intent.putExtra(EXTRA_CURRENT_ALBUM_POSITION, mCurrentPosition);
                setResult(RESULT_OK, intent);
                NavUtils.navigateUpTo(this, intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartID > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast())
            {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartID) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartID = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }


    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentDetailsFragment = (ArticleDetailFragment) object;
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID),  mCurrentPosition, mStartingPosition);
        }

        public String getTransitionName(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getString(ArticleLoader.Query.TITLE);
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }



}
