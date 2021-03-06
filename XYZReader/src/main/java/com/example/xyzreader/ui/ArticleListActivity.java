package com.example.xyzreader.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewTreeObserver;

import com.example.xyzreader.R;
import com.example.xyzreader.adapters.RecyclerListArticleAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.sync.XYZReaderSyncAdapter;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */


/** For the transitions, the examples inside the SunshineApp and the following gitrepo were used:
 * https://github.com/alexjlockwood/activity-transitions
 * The latter is more specific to the SharedElementTransition as it gave me a good understanding how
 * to deal with the passes between the RecyclerView<->DetailActivity<->ViewPager<->DetailFragment
 *
 * A number of iterations were made on the toolbar for the detail activity. At the end, the preferred
 * choice was to utilize the toolbar with NavUtils inside the DetailActivity.
 *
 * The return SharedElementTransition is removed as to prevent the animation from breaking,
 * when the returning position from the ViewPager doesn't match the currently loaded elements
 * inside the RecyclerView and to avoid postponing the animation until Picasso loads the thumbnails.
 * The preferred choice was to leave the return animation only for the times when the user returns from
 * the same article that he selected from the RecyclerView.
 * With the current setup, another option would be to have the thumbnail present in the DetailFragment.
 * */

public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    public RecyclerListArticleAdapter mRecyclerListArticleAdapter;
    private RecyclerView mRecyclerView;
    private Bundle mTmpReenterState;
    private static boolean mIsDetailsActivityStarted;
    private Activity mActivity;

    public static final String EXTRA_STARTING_ALBUM_POSITION = "extra_starting_item_position";
    static final String EXTRA_CURRENT_ALBUM_POSITION = "extra_current_item_position";
    static final String EXTRA_CURRENT_TRANSITION_NAME = "extra_current_transition_name";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        // Set the toolbar logo.
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setLogo(R.drawable.logo);
        mActivity = this;

        setExitSharedElementCallback(mCallback);


        //final View toolbarContainerView = findViewById(R.id.toolbar_container);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

        mRecyclerListArticleAdapter = new RecyclerListArticleAdapter(this);

        mRecyclerView.setAdapter(mRecyclerListArticleAdapter);



        getLoaderManager().initLoader(0, null, this);
        XYZReaderSyncAdapter.initializeSyncAdapter(this);

        if (savedInstanceState == null)
        {
            XYZReaderSyncAdapter.syncImmediately(this);
        }


    }

    private void refresh() {
        XYZReaderSyncAdapter.syncImmediately(mActivity);
    }

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mTmpReenterState != null) {

                String transitionName = mTmpReenterState.getString(EXTRA_CURRENT_TRANSITION_NAME);
                int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ALBUM_POSITION);
                int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ALBUM_POSITION);
                if (startingPosition != currentPosition) {
                    // If startingPosition != currentPosition the user must have swiped to a
                    // different page in the DetailsActivity. We must update the shared element
                    // so that the correct one falls into place.

                    View newSharedElement = mRecyclerView.findViewWithTag(transitionName);
                    if (newSharedElement != null)
                    {
                        names.clear();
                        names.add(transitionName);
                        sharedElements.clear();
                        sharedElements.put(transitionName, newSharedElement);
                    }
                }

                mTmpReenterState = null;
            } else {
                // If mTmpReenterState is null, then the activity is exiting.
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null) {
                    names.add(navigationBar.getTransitionName());
                    sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                }
                if (statusBar != null) {
                    names.add(statusBar.getTransitionName());
                    sharedElements.put(statusBar.getTransitionName(), statusBar);
                }
            }
        }
    };

    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        mTmpReenterState = new Bundle(data.getExtras());
        int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ALBUM_POSITION);
        int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ALBUM_POSITION);
        if (startingPosition != currentPosition)
        {
            mRecyclerView.smoothScrollToPosition(currentPosition);
        }
        supportPostponeEnterTransition();
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                mRecyclerView.requestLayout();
                supportStartPostponedEnterTransition();
                return true;
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(XYZReaderSyncAdapter.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data.getExtras()!=null)
        {
            mTmpReenterState = new Bundle(data.getExtras());
            int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ALBUM_POSITION);
            int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ALBUM_POSITION);
            if (startingPosition != currentPosition)
            {
                mRecyclerView.smoothScrollToPosition(currentPosition);
            }

        }
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (XYZReaderSyncAdapter.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(XYZReaderSyncAdapter.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mRecyclerListArticleAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    public static boolean GetmIsDetailsActivityStarted()
    {
        return mIsDetailsActivityStarted;
    }

    public static void SetmIsDetailsActivityStarted(boolean started)
    {
        mIsDetailsActivityStarted = started;
    }


}
