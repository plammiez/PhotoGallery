package ayp.aug.photogallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.support.v4.app.Fragment;

/**
 * Created by Waraporn on 9/5/2016.
 */
public class PhotoMapActivity extends SingleFragmentActivity {

    private static final String KEY_LOCATION = "GA1";
    private static final String KEY_GALLERY_ITEM = "GA2";
    private static final String KEY_BITMAP = "GA3";

    protected static Intent newIntent(Context context, Location location,
                                      Location galleryItemLoc, Bitmap bitmap) {
        Intent i = new Intent(context, PhotoMapActivity.class);
        i.putExtra(KEY_LOCATION, location);
        i.putExtra(KEY_GALLERY_ITEM, galleryItemLoc);
        i.putExtra(KEY_BITMAP, bitmap);
        return i;
    }
    @Override
    protected Fragment onCreateFragment() {
        if (getIntent() != null) {
            Location location = getIntent().getParcelableExtra(KEY_LOCATION);
            Location galleryLoc = getIntent().getParcelableExtra(KEY_GALLERY_ITEM);
            Bitmap bitmap = getIntent().getParcelableExtra(KEY_BITMAP);

            return PhotoMapFragment.newInstance(location, galleryLoc, bitmap);
        }
        return PhotoMapFragment.newInstance();
    }
}
