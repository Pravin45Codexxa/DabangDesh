package com.app.dabangdeshnews.activity;

import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.APPLOVIN;
import static com.solodroid.ads.sdk.util.Constant.APPLOVIN_MAX;
import static com.solodroid.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;
import static com.solodroid.ads.sdk.util.Constant.WORTISE;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.app.dabangdeshnews.BuildConfig;
import com.app.dabangdeshnews.Config;
import com.app.dabangdeshnews.R;
import com.app.dabangdeshnews.callback.CallbackConfig;
import com.app.dabangdeshnews.callback.CallbackLabel;
import com.app.dabangdeshnews.database.prefs.AdsPref;
import com.app.dabangdeshnews.database.prefs.SharedPref;
import com.app.dabangdeshnews.database.sqlite.DbLabel;
import com.app.dabangdeshnews.model.Ads;
import com.app.dabangdeshnews.model.App;
import com.app.dabangdeshnews.model.Blog;
import com.app.dabangdeshnews.model.CustomCategory;
import com.app.dabangdeshnews.rest.RestAdapter;
import com.app.dabangdeshnews.util.AdsManager;
import com.app.dabangdeshnews.util.Tools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySplash extends AppCompatActivity {

    public static final String TAG = "SplashActivity";
    Call<CallbackConfig> callbackConfigCall = null;
    Call<CallbackLabel> callbackLabelCall = null;
    ImageView imgSplash;
    AdsManager adsManager;
    SharedPref sharedPref;
    AdsPref adsPref;
    App app;
    CustomCategory customCategory;
    Blog blog;
    Ads ads;
    DbLabel dbLabel;
    boolean isForceOpenAds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_splash);
        isForceOpenAds = Config.FORCE_TO_SHOW_APP_OPEN_AD_ON_START;
        dbLabel = new DbLabel(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        adsManager = new AdsManager(this);
        adsManager.initializeAd();

        imgSplash = findViewById(R.id.img_splash);

        if (sharedPref.getIsDarkTheme()) {
            imgSplash.setImageResource(R.drawable.bg_splash_dark);
            Tools.darkNavigation(this);
        } else {
            imgSplash.setImageResource(R.drawable.bg_splash_default);
            Tools.lightNavigation(this);
        }

        Tools.postDelayed(this::requestConfig, Config.DELAY_SPLASH);
    }

    private void requestConfig() {
        if (Config.ACCESS_KEY.contains("XXXXX")) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("App not configured")
                    .setMessage("Please put your Server Key and Rest API Key from settings menu in your admin panel to AppConfig, you can see the documentation for more detailed instructions.")
                    .setPositiveButton(getString(R.string.dialog_option_ok), (dialogInterface, i) -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            String data = Tools.decode(Config.ACCESS_KEY);
            String[] results = data.split("_applicationId_");
            String remoteUrl = results[0];
            String applicationId = results[1];

            if (applicationId.equals(BuildConfig.APPLICATION_ID)) {
                requestAPI(remoteUrl);
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Error")
                        .setMessage("Whoops! invalid access key or applicationId, please check your configuration")
                        .setPositiveButton(getString(R.string.dialog_option_ok), (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
            Log.d(TAG, "Start request config");
        }
    }

    private void requestAPI(String remoteUrl) {
        if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
            if (remoteUrl.contains("https://drive.google.com")) {
                String driveUrl = remoteUrl.replace("https://", "").replace("http://", "");
                List<String> data = Arrays.asList(driveUrl.split("/"));
                String googleDriveFileId = data.get(3);
                callbackConfigCall = RestAdapter.createApiGoogleDrive().getDriveJsonFileId(googleDriveFileId);
                Log.d(TAG, "Request API from Google Drive Share link");
                Log.d(TAG, "Google drive file id : " + data.get(3));
            } else {
                callbackConfigCall = RestAdapter.createApiJsonUrl().getJsonUrl(remoteUrl);
                Log.d(TAG, "Request API from Json Url");
            }
        } else {
            callbackConfigCall = RestAdapter.createApiGoogleDrive().getDriveJsonFileId(remoteUrl);
            Log.d(TAG, "Request API from Google Drive File ID");
        }
        callbackConfigCall.enqueue(new Callback<CallbackConfig>() {
            public void onResponse(@NonNull Call<CallbackConfig> call, @NonNull Response<CallbackConfig> response) {
                CallbackConfig resp = response.body();
                displayApiResults(resp);
            }

            public void onFailure(@NonNull Call<CallbackConfig> call, @NonNull Throwable th) {
                Log.e(TAG, "initialize failed: " + th.getMessage());
                showAppOpenAdIfAvailable();
            }
        });
    }

    private void displayApiResults(CallbackConfig resp) {

        if (resp != null) {
            app = resp.app;
            Log.d("sharedyuuyi","fwewd" + app.privacy_policy_url);
            ads = resp.ads;
            blog = resp.blog;
            customCategory = resp.custom_category;

            sharedPref.saveBlogCredentials(blog.blogger_id, blog.api_key);
            adsManager.saveConfig(sharedPref, app);
            adsManager.saveAds(adsPref, ads);
            adsManager.saveAdsPlacement(adsPref, ads.placement);

            if (!app.status) {
                startActivity(new Intent(getApplicationContext(), ActivityRedirect.class));
                finish();
                Log.d(TAG, "App status is suspended");
            } else {
                if (customCategory.status) {
                    dbLabel.truncateTableCategory(DbLabel.TABLE_LABEL);
                    dbLabel.addListCategory(customCategory.categories, DbLabel.TABLE_LABEL);
                    showAppOpenAdIfAvailable();
                } else {
                    requestLabel();
                }
                Log.d(TAG, "App status is live");
            }
            Log.d(TAG, "initialize success");
        } else {
            Log.d(TAG, "initialize failed");
            showAppOpenAdIfAvailable();
        }
    }

    private void requestLabel() {
        this.callbackLabelCall = RestAdapter.createApiCategory(sharedPref.getBloggerId()).getLabel();
        this.callbackLabelCall.enqueue(new Callback<CallbackLabel>() {
            public void onResponse(@NonNull Call<CallbackLabel> call, @NonNull Response<CallbackLabel> response) {
                CallbackLabel resp = response.body();
                if (resp == null) {
                    showAppOpenAdIfAvailable();
                    return;
                }
                dbLabel.truncateTableCategory(DbLabel.TABLE_LABEL);
                dbLabel.addListCategory(resp.feed.category, DbLabel.TABLE_LABEL);
                showAppOpenAdIfAvailable();
                Log.d(TAG, "Success initialize label with count " + resp.feed.category.size() + " items");
            }

            public void onFailure(@NonNull Call<CallbackLabel> call, @NonNull Throwable th) {
                if (!call.isCanceled()) {

                    showAppOpenAdIfAvailable();
                }
            }
        });
    }

    private void showAppOpenAdIfAvailable() {
        if (isForceOpenAds) {
            if (adsPref.getIsOpenAd()) {
                adsManager.loadAppOpenAd(adsPref.getIsAppOpenAdOnStart(), this::startMainActivity);
            } else {
                startMainActivity();
            }
        } else {
            if (adsPref.getAdStatus() && adsPref.getIsAppOpenAdOnStart()) {
                switch (adsPref.getMainAds()) {
                    case ADMOB:
                        if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                            ((MyApplication) getApplication()).showAdIfAvailable(ActivitySplash.this, this::startMainActivity);
                        } else {
                            startMainActivity();
                        }
                        break;
                    case GOOGLE_AD_MANAGER:
                        if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                            ((MyApplication) getApplication()).showAdIfAvailable(ActivitySplash.this, this::startMainActivity);
                        } else {
                            startMainActivity();
                        }
                        break;
                    case APPLOVIN:
                    case APPLOVIN_MAX:
                        if (!adsPref.getAppLovinAppOpenAdUnitId().equals("0")) {
                            ((MyApplication) getApplication()).showAdIfAvailable(ActivitySplash.this, this::startMainActivity);
                        } else {
                            startMainActivity();
                        }
                        break;
                    case WORTISE:
                        if (!adsPref.getWortiseAppOpenAdUnitId().equals("0")) {
                            ((MyApplication) getApplication()).showAdIfAvailable(ActivitySplash.this, this::startMainActivity);
                        } else {
                            startMainActivity();
                        }
                        break;
                    default:
                        startMainActivity();
                        break;
                }
            } else {
                startMainActivity();
            }
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

}
