package ayp.aug.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;

/**
 * Created by Waraporn on 9/5/2016.
 */
public class PhotoMapFragment extends SupportMapFragment {

    private static final String KEY_LOCATION = "GA1";
    private static final String KEY_GALLERY_ITEM = "GA2";
    private static final String KEY_BITMAP = "GA3";
    private static final String TAG = "PhotoMapFragment";

    public static PhotoMapFragment newInstance(Location location,
                                               Location galleryItemLoc, String url) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_LOCATION, location);
        args.putParcelable(KEY_GALLERY_ITEM, galleryItemLoc);
        args.putString(KEY_BITMAP, url);
        PhotoMapFragment fragment = new PhotoMapFragment();
        fragment.setArguments(args);
        return fragment;
    }
    private GoogleMap mGoogleMap;
    private Location mLocation;
    private Location mLocationGallery;
    private String mUrl;
    private Bitmap mBitmap;
    private MarkerFetcherTask mFetchTask;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setHasOptionsMenu(true);

        if (getArguments() != null) {
            mLocation = getArguments().getParcelable(KEY_LOCATION);
            mLocationGallery = getArguments().getParcelable(KEY_GALLERY_ITEM);
            mUrl = getArguments().getString(KEY_BITMAP);
        }

        if (mUrl != null) {
            Log.d(TAG, "Get URL : " + mUrl);
        }

        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mGoogleMap = googleMap;
                if (mUrl == null) {
                    updateMapUI();
                } else {
                    mFetchTask = new MarkerFetcherTask();
                    mFetchTask.execute(mUrl);
                }
            }
        });
    }

    private void updateMapUI() {
        mGoogleMap.clear();

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        if (mLocation != null) {
            Log.d(TAG, "Found location from my location");
            plotMarker(mLocation, builder);
        }

        if (mLocationGallery != null) {
            Log.d(TAG, "Found location from gallery item location");

            if (mBitmap == null) {
                plotMarker(mLocationGallery, builder);
            } else {
                plotMarker(mLocationGallery, builder, mBitmap);
            }

        }

        int margin = getResources().getDimensionPixelSize(R.dimen.map_inset_margin);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(
                builder.build(), margin);

        mGoogleMap.animateCamera(cameraUpdate);
    }

    private void plotMarker(Location location, final LatLngBounds.Builder builder) {
        Log.d(TAG, "Plot Location = " + location);
        LatLng itemPoint = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions itemMarkerOptions = new MarkerOptions().position(itemPoint);
        mGoogleMap.addMarker(itemMarkerOptions);
        builder.include(itemPoint);
//        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(itemPoint, 15));
    }

    private void plotMarker(Location location, final LatLngBounds.Builder builder,
                            final Bitmap bitmap) {
        Log.d(TAG, "Plot Location = " + location);
        LatLng itemPoint = new LatLng(location.getLatitude(), location.getLongitude());
        BitmapDescriptor itemBitmap = BitmapDescriptorFactory.fromBitmap(bitmap);
        MarkerOptions itemMarkerOptions = new MarkerOptions()
                .position(itemPoint)
                .icon(itemBitmap);
        mGoogleMap.addMarker(itemMarkerOptions);
        builder.include(itemPoint);
//        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(itemPoint, 15));
    }

    private class MarkerFetcherTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            // Fetch Photo
            String url = params[0];

            FlickrFetcher flickrFetcher = new FlickrFetcher();
            try {
                byte[] imageBytes = flickrFetcher.getUrlBytes(url);
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                return bitmap;
            } catch (IOException e) {
                Log.e(TAG, "Error in IO", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mBitmap = bitmap;
            updateMapUI();
            mFetchTask = null;
        }
    }
}
