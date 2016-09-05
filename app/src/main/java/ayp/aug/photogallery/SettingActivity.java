package ayp.aug.photogallery;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Waraporn on 9/5/2016.
 */
public class SettingActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context c){
        return new Intent(c, SettingActivity.class);
    }

    @Override
    protected Fragment onCreateFragment() {
        return PhotoSettingFragment.newInstance();
    }
}
