package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.uiutil.DynamicHeightImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;


/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final String ARG_THUMBNAIL_IMAGE_POSITION = "arg_starting_thumbnail_image_position";
    private static final String ARG_STARTING_THUMBNAIL_IMAGE_POSITION = "arg_starting_thumbnail_image_position";


    private Cursor mCursor;
    private long mItemId;
    private long mStartingThumbnailPosition;
    private long mThumbnailPosition;

    private boolean mIsTransitioning;
    private long mBackgroundImageFadeMillis;

    private View mRootView;
    private DynamicHeightImageView mPhotoView;
    private boolean mIsCard = false;
    private Toolbar mToolbar;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    private final Callback mImageCallback = new Callback() {
        @Override
        public void onSuccess() {
            startPostponedEnterTransition();
        }

        @Override
        public void onError() {
            startPostponedEnterTransition();
        }
    };

    public static ArticleDetailFragment newInstance(long itemId,int position, int startingPosition) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putLong(ARG_THUMBNAIL_IMAGE_POSITION, position);
        arguments.putLong(ARG_STARTING_THUMBNAIL_IMAGE_POSITION, startingPosition);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID) &&
                getArguments().containsKey(ARG_STARTING_THUMBNAIL_IMAGE_POSITION))
        {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
            mThumbnailPosition = getArguments().getLong(ARG_THUMBNAIL_IMAGE_POSITION);
            mStartingThumbnailPosition = getArguments().getLong(ARG_STARTING_THUMBNAIL_IMAGE_POSITION);
            mIsTransitioning = savedInstanceState == null && mThumbnailPosition == mStartingThumbnailPosition;
            mBackgroundImageFadeMillis = getResources().getInteger(
                    R.integer.fragment_details_background_image_fade_millis);

        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);

        setHasOptionsMenu(true);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        /*  In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
            the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
            fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
            we do this in onActivityCreated.
        */
        getLoaderManager().initLoader(0, null, this);
        setHasOptionsMenu(true);



         /* Add the AppCompatActivity code for ActionBar.
            More specifically- up navigation.
            The fragment will rely on the activity to use the AppBar functions.
            However there is no Toolbar on the Activity xml, thus we pass the Activity
            functions to the fragment with ((AppCompatActivity) getActivity()) on onCreateView
          */

          /*
          ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
             if(((AppCompatActivity) getActivity()).getSupportActionBar() != null)
             {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
             }
           */

         /*The previous solution works, however only when at least one fragment has been switched
           in the Viewpager. To use the ColapsingToolbar and be able to swipe left<->right
           as well as up<-> down directly directly, this solution seems to work best.
           If the toolbar is added in the same layout as the ViewPager, then the
           expanded toolbar can't be swiped left<->right.
           mToolbar.setNavigationIcon(ContextCompat.getDrawable(getActivity(),R.drawable.ic_arrow_back));
           */


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        mRootView = inflater.inflate(R.layout.fragment_detail_start, container, false);



        mPhotoView = (DynamicHeightImageView) mRootView.findViewById(R.id.photo);


        getActivity().findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();

        return mRootView;
    }

    private ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);

        if (mCursor != null)
        {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            //Add url links to the HTML style of text for the body.
            // sample- http://jtomlinson.blogspot.co.uk/2010/03/textview-and-html.html
            bodyView.setMovementMethod(LinkMovementMethod.getInstance());
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#000000'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));



            mPhotoView.setTransitionName(mCursor.getString(ArticleLoader.Query.TITLE));

            //Picasso.with(getActivity()).load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
             //       .into(mPhotoView);

            mPhotoView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

            RequestCreator albumImageRequest = Picasso.with(getActivity())
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL));

            if (mIsTransitioning) {
                albumImageRequest.noFade();
            }

            albumImageRequest.into(mPhotoView, mImageCallback);


        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    private void startPostponedEnterTransition() {
        if (mThumbnailPosition == mStartingThumbnailPosition) {
            mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                    // TODO: Find a solution for the occasional crash when getActivity is called while swiping
                    getActivity().startPostponedEnterTransition();
                    return true;
                }
            });
        }
    }

    /**
     * Returns the shared element that should be transitioned back to the previous Activity,
     * or null if the view is not visible on the screen.
     */
    @Nullable
    ImageView getThumbnailImage() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView)) {
            return mPhotoView;
        }
        return null;
    }

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }
}
