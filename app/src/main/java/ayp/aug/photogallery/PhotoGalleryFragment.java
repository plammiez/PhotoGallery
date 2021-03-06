package ayp.aug.photogallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.LruCache;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Waraporn on 8/16/2016.
 */
public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG = "PhotoGalleryFragment";
    private String mBigUrl;

    /**
     * Method for make sure isPhotoGalleryFragment.
     *
     * @return
     */
    public static PhotoGalleryFragment newInstance() {
        Bundle args = new Bundle();
        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView mRecyclerView;
    private FlickrFetcher mFlickrFetcher;
    private PhotoGalleryAdapter mAdapter;
//    private List<GalleryItem> mItems;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloaderThread;
    private FetcherTask mFetcherTask;
    private String mSearchKey;

    // Cache
    private LruCache<String, Bitmap> mMemoryCache;
    // Memory
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    // Use 1/8th of the available memory for this memory cache.
    final int cacheSize = maxMemory / 8;

    /**
     *
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        Intent intent = PollService.newIntent(getActivity());
        getActivity().startService(intent);

        PollService.setServiceAlarm(getActivity(), true);

        Log.d(TAG, "Memory size = " + maxMemory + " K ");

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        // Move from onCreateView
        mFlickrFetcher = new FlickrFetcher();
        mFetcherTask = new FetcherTask();
        new FetcherTask().execute(); //run another thread.

        Handler responseUIHandler = new Handler();
        ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder> listener =
                new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail, String url) {

                        if (null == mMemoryCache.get(url)) {
                            mMemoryCache.put(url, thumbnail);
                        }
                        Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                        target.bindDrawable(drawable);
                    }
                };

        mThumbnailDownloaderThread = new ThumbnailDownloader<>(responseUIHandler);
        mThumbnailDownloaderThread.setmThumbnailDownloaderListener(listener);
        mThumbnailDownloaderThread.start();
        mThumbnailDownloaderThread.getLooper();

        Log.i(TAG, "Start background thread");
    }

    /**
     *
     *
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.list_menu_refresh, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQuery(mSearchKey, false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Query text submitted: " + query);
                mSearchKey = query;
                loadPhoto();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Query text changed: " + newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setQuery(mSearchKey, false);
            }
        });

        //render polling
        MenuItem mnuPolling = menu.findItem(R.id.mnu_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            mnuPolling.setTitle(R.string.stop_polling);
        } else {
            mnuPolling.setTitle(R.string.start_polling);
        }
    }

    /**
     *
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh: loadPhoto();
                return true;

            case R.id.menu_clear_search: mSearchKey = null;
                loadPhoto();
                return true;

            case R.id.mnu_toggle_polling:
                Log.d(TAG, "Start intent service");
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
//        Intent i = PollService.newIntent(getActivity());
//        getActivity().startService(i);
                Log.d(TAG, (shouldStartAlarm ? " Start " : " Stop ") + " Intent Service ");
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu(); //refresh menu
                return true;

            case R.id.mnu_manual_check:
                Intent pollIntent = PollService.newIntent(getActivity());
                getActivity().startService(pollIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadPhoto() {
        if (mFetcherTask == null || !mFetcherTask.isRunning()) {
            mFetcherTask = new FetcherTask();

            if (mSearchKey != null) {
                mFetcherTask.execute(mSearchKey);
            } else {
                mFetcherTask.execute();
            }
        }
    }

    /**
     *
     *
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        mThumbnailDownloaderThread.quit();
        Log.i(TAG, "Stop background thread");
    }

    /**
     *
     *
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mThumbnailDownloaderThread.clearQueue();
    }

    /**
     *
     *
     */
    @Override
    public void onPause() {
        super.onPause();
        PhotoGalleryPreference.setStoredSearchKey(getActivity(), mSearchKey);
    }

    /**
     *
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        String searchKey = PhotoGalleryPreference.getStoredSearchKey(getActivity());

        if (searchKey != null) {
            mSearchKey = searchKey;
        }
    }

    /**
     *
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mRecyclerView = (RecyclerView) v.findViewById(R.id.photo_gallery_recycle_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

//        if (!mFetcherTask.isRunning()) {
//            mFetcherTask = new FetcherTask();
//            mFetcherTask.execute();
//        }
        mSearchKey = PhotoGalleryPreference.getStoredSearchKey(getActivity());
        loadPhoto();
        Log.d(TAG, "On create complete : ----- Loaded key -----" + mSearchKey);
//        mRecyclerView.setAdapter(new PhotoGalleryAdapter(itemList));
        return v;
    }

    /**
     *
     *
     */
    class PhotoHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener, View.OnClickListener {

        ImageView mPhoto;

        public PhotoHolder(View itemView) {
            super(itemView);

//            mText = (TextView) itemView;
            mPhoto = (ImageView) itemView.findViewById(R.id.image_photo);
            mPhoto.setOnClickListener(this);

            itemView.setOnCreateContextMenuListener(this);
        }

//        public void bindGalleryItem(GalleryItem galleryItem) {
//            mText.setText(galleryItem.getTitle());
//        }

        /**
         *
         *
         * @param drawable
         */
        public void bindDrawable(@NonNull Drawable drawable) {
            mPhoto.setImageDrawable(drawable);
        }

        public void setBigUrl(String bigUrl) {
            mBigUrl = bigUrl;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            MenuItem menuItem = menu.add(R.string.open_by_url);
            menu.setHeaderTitle(mBigUrl);
            menuItem.setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return false;
        }

        @Override
        public void onClick(View v) {

        }
    }

    /**
     *
     *
     */
    class PhotoGalleryAdapter extends RecyclerView.Adapter<PhotoHolder> {

        List<GalleryItem> mGalleryItemList;

        PhotoGalleryAdapter(List<GalleryItem> galleryItems) {
            mGalleryItemList = galleryItems;
        }

        /**
         *
         *
         * @param parent
         * @param viewType
         * @return
         */
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getActivity()).inflate(
                    R.layout.item_photo, parent, false);

            return new PhotoHolder(v);
        }

        /**
         *
         *
         * @param holder
         * @param position
         */
        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
//            holder.bindGalleryItem(mGalleryItemList.get(position));
            Drawable smileyDrawable = ResourcesCompat
                    .getDrawable(getResources(), R.drawable.face, null);

            GalleryItem galleryItem = mGalleryItemList.get(position);
            Log.d(TAG, "bind position #" + position + ", url: " + galleryItem.getUrl());

            holder.bindDrawable(smileyDrawable);

            if(mMemoryCache.get(galleryItem.getUrl()) != null) {
                Bitmap bitmap = mMemoryCache.get(galleryItem.getUrl());
                holder.bindDrawable(new BitmapDrawable(getResources(), bitmap));
            } else {
                mThumbnailDownloaderThread.queueThumbnailDownload(holder, galleryItem.getUrl());
            }
        }

        /**
         *
         *
         * @return
         */
        @Override
        public int getItemCount() {
            return mGalleryItemList.size();
        }
    }

    /**
     *
     *
     */
    class FetcherTask extends AsyncTask<String, Void, List<GalleryItem>> {

        boolean running = false;

        /**
         *
         *
         * @param params
         * @return
         */
        @Override
        protected List<GalleryItem> doInBackground(String... params) {

            synchronized (this) {
                running = true;
            }

            try {
                Log.d(TAG, "Fetcher task finish");
                List<GalleryItem> itemList = new ArrayList<>();

                if (params.length > 0) {
                    mFlickrFetcher.searchPhotos(itemList, params[0]);
                } else {
                    mFlickrFetcher.getRecentPhotos(itemList);
                }

                Log.d(TAG, "Fetcher task finish");
                return itemList;
            } finally {
                synchronized (this) {
                    running = false;
                }
            }
        }

        boolean isRunning() {
            return running;
        }

//        @Override
//        protected void onProgressUpdate(Void... values) {
//            super.onProgressUpdate(values);
//        }

        /**
         *
         *
         * @param galleryItems
         */
        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mAdapter = new PhotoGalleryAdapter(galleryItems);
            mRecyclerView.setAdapter(mAdapter);
//                mRecyclerView.setAdapter(new PhotoGalleryAdapter(mItems));
            String formatString = getResources().getString(R.string.photo_progress_loaded);
            Snackbar.make(mRecyclerView, formatString, Snackbar.LENGTH_SHORT).show();
        }
    }
}
