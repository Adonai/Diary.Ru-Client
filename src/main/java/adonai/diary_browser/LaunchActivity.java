package adonai.diary_browser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class LaunchActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences mSharedPrefs = getApplicationContext().getSharedPreferences(Utils.mPrefsFile, MODE_PRIVATE);

        if (!mSharedPrefs.getString(Utils.KEY_USERNAME, "").isEmpty() &&
                !mSharedPrefs.getString(Utils.KEY_PASSWORD, "").isEmpty() &&
                mSharedPrefs.getBoolean(Utils.KEY_KEEP_AUTH, true))

            startActivity(new Intent(this, DiaryListActivity.class));
        else
            startActivity(new Intent(this, AuthorizationForm.class));

        finish();
    }


}
