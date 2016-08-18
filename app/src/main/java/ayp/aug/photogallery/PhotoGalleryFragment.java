package ayp.aug.photogallery;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Waraporn on 8/16/2016.
 */
public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

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

    // Cache
    private LruCache<String, Bitmap> mMemoryCache;
    // Memory
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    // Use 1/8th of the available memory for this memory cache.
    final int cacheSize = maxMemory / 8;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        Log.d(TAG, "Memory sixe = " + maxMemory + " K ");

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

    @Override
    public void onDestroy() {
        super.onDestroy();

        mThumbnailDownloaderThread.quit();
        Log.i(TAG, "Stop background thread");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mThumbnailDownloaderThread.clearQueue();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mRecyclerView = (RecyclerView) v.findViewById(R.id.photo_gallery_recycle_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        if (!mFetcherTask.isRunning()) {
            mFetcherTask = new FetcherTask();
            mFetcherTask.execute();
        }
//        mRecyclerView.setAdapter(new PhotoGalleryAdapter(itemList));
        return v;
    }

    class PhotoHolder extends RecyclerView.ViewHolder {

        ImageView mPhoto;

        public PhotoHolder(View itemView) {
            super(itemView);

//            mText = (TextView) itemView;
            mPhoto = (ImageView) itemView.findViewById(R.id.image_photo);
        }

//        public void bindGalleryItem(GalleryItem galleryItem) {
//            mText.setText(galleryItem.getTitle());
//        }

        public void bindDrawable(@NonNull Drawable drawable) {
            mPhoto.setImageDrawable(drawable);
        }
    }

    class PhotoGalleryAdapter extends RecyclerView.Adapter<PhotoHolder> {

        List<GalleryItem> mGalleryItemList;

        PhotoGalleryAdapter(List<GalleryItem> galleryItems) {
            mGalleryItemList = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getActivity()).inflate(
                    R.layout.item_photo, parent, false);

            return new PhotoHolder(v);
        }

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

        @Override
        public int getItemCount() {
            return mGalleryItemList.size();
        }
    }

    class FetcherTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        int total;
        boolean running = false;

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {

            synchronized (this) {
                running = true;
            }

            try {
                Log.d(TAG, "Fetcher task finish");
                List<GalleryItem> itemList = new ArrayList<>();

                mFlickrFetcher.fetchItems(itemList);
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

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            String formatString = getResources().getString(R.string.photo_progress_loaded);
            Snackbar.make(mRecyclerView, formatString, Snackbar.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mAdapter = new PhotoGalleryAdapter(galleryItems);
            mRecyclerView.setAdapter(mAdapter);
//                mRecyclerView.setAdapter(new PhotoGalleryAdapter(mItems));
        }
    }
}
