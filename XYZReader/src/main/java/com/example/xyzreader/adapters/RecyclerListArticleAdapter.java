package com.example.xyzreader.adapters;

import android.app.Activity;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.DynamicHeightImageView;
import com.squareup.picasso.Picasso;

/**
 * Created by paskalstoyanov on 11/04/16.
 */
public class RecyclerListArticleAdapter extends RecyclerView.Adapter
        <RecyclerListArticleAdapter.ArticlesViewHolder>
{

    private Cursor mCursor;
    private Activity mContext;



    public class ArticlesViewHolder extends RecyclerView.ViewHolder
    {
        public DynamicHeightImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ArticlesViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }

    public RecyclerListArticleAdapter(Activity context)
    {
        mContext = context;
    }

    @Override
    public ArticlesViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewGroup instanceof RecyclerView)
        {
            int layoutId = R.layout.list_item_article;
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(layoutId, viewGroup, false);
            //view.setFocusable(true);
            return new ArticlesViewHolder(view);
        }
        else
        {
            throw new RuntimeException("Not bound to RecyclerViewSelection");

        }
    }

    @Override
    public void onBindViewHolder(ArticlesViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
        holder.subtitleView.setText(
                DateUtils.getRelativeTimeSpanString(
                        mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString()
                        + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR));
        //holder.thumbnailView.setImageUrl(mCursor.getString(ArticleLoader.Query.THUMB_URL),
        //        ImageLoaderHelper.getInstance(mContext).getImageLoader());


        Picasso.with(holder.thumbnailView.getContext()).
                load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                .placeholder(R.drawable.photo_background_protection)
                .into(holder.thumbnailView);


        holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));


    }

    @Override
    public int getItemCount() {
        if (null == mCursor) return 0;
        return mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
    }

    public Cursor getCursor() {
        return mCursor;
    }


}