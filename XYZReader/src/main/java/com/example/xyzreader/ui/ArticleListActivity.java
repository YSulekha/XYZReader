package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends ActionBarActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private Context context;
    private Bundle mbundle;
    private Bundle mReenterState;
    private static final String EXTRA_START_POSITION = "extra_start_position";
    private static final String EXTRA_CURRENT_POSITION = "extra_current_position";



    private SharedElementCallback callback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if(mReenterState!=null){
                int startPos = mReenterState.getInt(EXTRA_START_POSITION);
                int currentPos = mReenterState.getInt(EXTRA_CURRENT_POSITION);
                if(startPos!=currentPos){
                    String transName = getString(R.string.anim_image)+mRecyclerView.getAdapter().getItemId(currentPos);
                    View sharedElement = mRecyclerView.findViewWithTag(transName);
                    if(sharedElement!=null){
                        names.clear();
                        names.add(transName);
                        sharedElements.clear();
                        sharedElements.put(transName, sharedElement);
                    }
                }
                mReenterState = null;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list_co);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        context =this;

      //  final View toolbarContainerView = findViewById(R.id.toolbar_container);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);
        setExitSharedElementCallback(callback);
        mbundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle();

        if (savedInstanceState == null) {
            refresh();
        }
      //  setupWindowAnimations();
    }
    private void setupWindowAnimations() {
        Fade fade = new Fade();

        fade.setDuration(1000);
        getWindow().setExitTransition(fade);
      /*  Slide slide = new Slide();
        slide.setDuration(1000);
        getWindow().setReturnTransition(slide);*/
      /*  Slide slideTransition = new Slide();
        slideTransition.setSlideEdge(Gravity.LEFT);
        slideTransition.setDuration(500);
        getWindow().setReenterTransition(slideTransition);*/
     //   getWindow().setExitTransition(slideTransition);
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }
    @Override
    public void onActivityReenter(int requestCode, Intent data){
        super.onActivityReenter(requestCode,data);
        mReenterState = new Bundle(data.getExtras());
        int startPos = mReenterState.getInt(EXTRA_START_POSITION);
        int currentPos = mReenterState.getInt(EXTRA_CURRENT_POSITION);
        if(startPos!=currentPos){
            mRecyclerView.scrollToPosition(currentPos);
        }
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                // TODO: figure out why it is necessary to request layout here in order to get a smooth transition.
                mRecyclerView.requestLayout();
                startPostponedEnterTransition();
                return true;
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
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
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final View sharedView = (ImageView)view.findViewById(R.id.thumbnail);
            final View sharedTitleView = (TextView)view.findViewById(R.id.article_title);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                 //   ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(ArticleListActivity.this);
                    sharedView.setTransitionName(getString(R.string.anim_image)+getItemId(vh.getAdapterPosition()));
                    sharedTitleView.setTransitionName("Text Animation"+getItemId(vh.getAdapterPosition()));
                    Pair<View, String> pair1 = Pair.create(sharedView, sharedView.getTransitionName());
                    Pair<View, String> pair2 = Pair.create(sharedTitleView, sharedTitleView.getTransitionName());
                    String transitionName = getString(R.string.anim_image)+getItemId(vh.getAdapterPosition());
                    Log.v("SharedViewD",transitionName);

                    ActivityOptions transitionActivityOptions = ActivityOptions.makeSceneTransitionAnimation(ArticleListActivity.this, pair1,pair2);
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                    intent.putExtra(EXTRA_START_POSITION,vh.getAdapterPosition());
                    startActivity(intent,transitionActivityOptions.toBundle());
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
          /*  holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));*/
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
           float aspect =  mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO);
            if(aspect > 1){
                aspect = 0.8f;
            }
            holder.thumbnailView.setAspectRatio(aspect);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
       public DynamicHeightNetworkImageView thumbnailView;
   //    public NetworkImageView thumbnailView;
        public TextView titleView;
       // public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
         //   thumbnailView = (NetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
         //   subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}
