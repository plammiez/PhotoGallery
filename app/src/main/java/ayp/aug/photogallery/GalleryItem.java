package ayp.aug.photogallery;

import android.net.Uri;

/**
 * Created by Waraporn on 8/16/2016.
 */
public class GalleryItem {

    private String mId;
    private String mTitle;
    private String mUrl;
    private String mOwner;

    public void setId(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getName() {
        return getTitle();
    }

    public void setName(String name) {
        setTitle(name);
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof GalleryItem) {
            //is GalleryItem too!
            GalleryItem that = (GalleryItem) o;

            return that.mId != null && mId != null && that.mId.equals(mId);
        }
        return false;
    }

    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    private static final String PHOTO_URL_PREFIX = "https://www.flickr.com/photos/";

    public Uri getPhotoUri() {
        return Uri.parse(PHOTO_URL_PREFIX).buildUpon()  // Return Builder
                .appendPath(mOwner)
                .appendPath(mId)
                .build();                               // Return Uri
    }
}
