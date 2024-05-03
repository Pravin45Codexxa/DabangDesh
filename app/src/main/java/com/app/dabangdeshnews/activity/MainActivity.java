package com.app.dabangdeshnews.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.viewpager.widget.ViewPager;

import com.app.dabangdeshnews.BuildConfig;
import com.app.dabangdeshnews.Config;
import com.app.dabangdeshnews.R;
import com.app.dabangdeshnews.database.prefs.AdsPref;
import com.app.dabangdeshnews.database.prefs.SharedPref;
import com.app.dabangdeshnews.util.AdsManager;
import com.app.dabangdeshnews.util.AppBarLayoutBehavior;
import com.app.dabangdeshnews.util.Constant;
import com.app.dabangdeshnews.util.RtlViewPager;
import com.app.dabangdeshnews.util.Tools;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.solodroid.ads.sdk.format.AppOpenAd;
import com.solodroid.push.sdk.provider.OneSignalPush;

public class MainActivity extends AppCompatActivity implements DefaultLifecycleObserver {

    private static final String TAG = "MainActivity";
    private ViewPager viewPager;
    private RtlViewPager viewPagerRTL;
    private AppUpdateManager appUpdateManager;
    private long exitTime = 0;
    BottomNavigationView navigation;
    Toolbar toolbar;
    TextView titleToolbar;
    CardView lytSearchBar;
    LinearLayout searchBar;
    ImageButton btnSearch;
    ImageView btnMoreOptions;
    SharedPref sharedPref;
    AdsPref adsPref;
    CoordinatorLayout parentView;
    AdsManager adsManager;
    Tools tools;
    View lyt_dialog_exit;
    LinearLayout lyt_panel_view;
    LinearLayout lyt_panel_dialog;
    OneSignalPush.Builder onesignal;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        tools = new Tools(this);



        if (Config.ENABLE_NEW_APP_DESIGN) {
            setContentView(R.layout.activity_main_new);
        } else {
            setContentView(R.layout.activity_main);
        }
        if (Config.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
            ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
            adsPref.setIsOpenAd(true);
        }
        Tools.setNavigation(this);

        onesignal = new OneSignalPush.Builder(this);
        onesignal.requestNotificationPermission();

        adsManager = new AdsManager(this);
        adsManager.initializeAd();
        adsManager.updateConsentStatus();
        adsManager.loadAppOpenAd(adsPref.getIsAppOpenAdOnResume());
        adsManager.loadBannerAd(adsPref.getIsBannerHome());
        adsManager.loadInterstitialAd(adsPref.getIsInterstitialPostList(), adsPref.getInterstitialAdInterval());

        sharedPref.resetPostToken();
        sharedPref.resetPageToken();

        initView();
        initExitDialog();

        Tools.notificationOpenHandler(this, getIntent());

        if (!BuildConfig.DEBUG) {
            appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
            checkUpdate();
            inAppReview();
        }

    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        Tools.postDelayed(() -> {
            if (AppOpenAd.isAppOpenAdLoaded) {
                adsManager.showAppOpenAd(adsPref.getIsAppOpenAdOnResume());
            }
        }, 100);
    }

    public void showInterstitialAd() {
        adsManager.showInterstitialAd();
    }

    public void showSnackBar(String msg) {
        Snackbar.make(parentView, msg, Snackbar.LENGTH_SHORT).show();
    }

    public void initView() {
        parentView = findViewById(R.id.tab_coordinator_layout);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        toolbar = findViewById(R.id.toolbar);
        titleToolbar = findViewById(R.id.title_toolbar);
        lytSearchBar = findViewById(R.id.lyt_search_bar);
        searchBar = findViewById(R.id.search_bar);
        btnSearch = findViewById(R.id.btn_search);
        btnMoreOptions = findViewById(R.id.btn_more_options);

        if (Config.ENABLE_NEW_APP_DESIGN) {
            setupNewToolbar();
        } else {
            setupToolbar();
        }

        navigation = findViewById(R.id.navigation);
        navigation.getMenu().clear();
        if (sharedPref.getIsShowPageMenu()) {
            navigation.inflateMenu(R.menu.navigation_default);
        } else {
            navigation.inflateMenu(R.menu.navigation_no_page);
        }
        navigation.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        if (sharedPref.getIsDarkTheme()) {
            navigation.setBackgroundColor(ContextCompat.getColor(this, R.color.color_dark_bottom_navigation));
        } else {
            navigation.setBackgroundColor(ContextCompat.getColor(this, R.color.color_light_bottom_navigation));
        }

        viewPager = findViewById(R.id.viewpager);
        viewPagerRTL = findViewById(R.id.viewpager_rtl);
        if (sharedPref.getIsEnableRtlMode()) {
            tools.setupViewPagerRTL(this, viewPagerRTL, navigation, toolbar, titleToolbar, sharedPref);
        } else {
            tools.setupViewPager(this, viewPager, navigation, toolbar, titleToolbar, sharedPref);
        }

        if (!Tools.isConnect(this)) {
            if (sharedPref.getIsShowPageMenu()) {
                if (sharedPref.getIsEnableRtlMode()) {
                    viewPagerRTL.setCurrentItem(3);
                } else {
                    viewPager.setCurrentItem(3);
                }
            } else {
                if (sharedPref.getIsEnableRtlMode()) {
                    viewPagerRTL.setCurrentItem(2);
                } else {
                    viewPager.setCurrentItem(2);
                }
            }
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_search) {
            Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
            startActivity(intent);
            adsManager.destroyBannerAd();
            return true;
        } else if (menuItem.getItemId() == R.id.action_more) {
            Intent intent;
            if (Config.ENABLE_NEW_APP_DESIGN) {
                intent = new Intent(getApplicationContext(), ActivitySettingsNew.class);
            } else {
                intent = new Intent(getApplicationContext(), ActivitySettings.class);
            }
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        if (sharedPref.getIsEnableRtlMode()) {
            if (viewPagerRTL.getCurrentItem() != 0) {
                viewPagerRTL.setCurrentItem((0), true);
            } else {
                exitApp();
            }
        } else {
            if (viewPager.getCurrentItem() != 0) {
                viewPager.setCurrentItem((0), true);
            } else {
                exitApp();
            }
        }
    }

    public void exitApp() {
        if (sharedPref.getIsEnableExitDialog()) {
            if (lyt_dialog_exit.getVisibility() != View.VISIBLE) {
                showDialog(true);
            }
        } else {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                showSnackBar(getString(R.string.press_again_to_exit));
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                destroyBannerAd();
                destroyAppOpenAd();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adsManager.resumeBannerAd(adsPref.getIsBannerHome());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyBannerAd();
        destroyAppOpenAd();
    }

    public void destroyBannerAd() {
        adsManager.destroyBannerAd();
    }

    public void destroyAppOpenAd() {
        if (Config.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
            adsManager.destroyAppOpenAd(adsPref.getIsAppOpenAdOnResume());
            ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
        }
        Constant.isAppOpen = false;
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }
    private void inAppReview() {
        if (sharedPref.getInAppReviewToken() <= 3) {
            sharedPref.updateInAppReviewToken(sharedPref.getInAppReviewToken() + 1);
        } else {
            ReviewManager manager = ReviewManagerFactory.create(this);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    manager.launchReviewFlow(MainActivity.this, reviewInfo).addOnFailureListener(e -> {
                    }).addOnCompleteListener(complete -> {
                                Log.d(TAG, "In-App Review Success");
                            }
                    ).addOnFailureListener(failure -> {
                        Log.d(TAG, "In-App Review Rating Failed");
                    });
                }
            }).addOnFailureListener(failure -> Log.d("In-App Review", "In-App Request Failed " + failure));
        }
        Log.d(TAG, "in app review token : " + sharedPref.getInAppReviewToken());
    }

    private void checkUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdateFlow(appUpdateInfo);
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdateFlow(appUpdateInfo);
            }
        });
    }

    private void startUpdateFlow(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, Constant.IMMEDIATE_APP_UPDATE_REQ_CODE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.IMMEDIATE_APP_UPDATE_REQ_CODE) {
            if (resultCode == RESULT_CANCELED) {
                showSnackBar("Update canceled");
            } else if (resultCode == RESULT_OK) {
                showSnackBar("Update success!");

            } else {
                showSnackBar("Update Failed!");
                checkUpdate();
            }
        }
    }


    public void initExitDialog() {

        lyt_dialog_exit = findViewById(R.id.lyt_dialog_exit);
        lyt_panel_view = findViewById(R.id.lyt_panel_view);
        lyt_panel_dialog = findViewById(R.id.lyt_panel_dialog);

        if (sharedPref.getIsDarkTheme()) {
            lyt_panel_view.setBackgroundColor(getResources().getColor(R.color.color_dialog_background_dark_overlay));
            lyt_panel_dialog.setBackgroundResource(R.drawable.bg_rounded_dark);
        } else {
            lyt_panel_view.setBackgroundColor(getResources().getColor(R.color.color_dialog_background_light));
            lyt_panel_dialog.setBackgroundResource(R.drawable.bg_rounded_default);
        }

        lyt_panel_view.setOnClickListener(view -> {
            //empty state
        });

        LinearLayout nativeAdView = findViewById(R.id.native_ad_view);
        Tools.setNativeAdStyle(this, nativeAdView, adsPref.getNativeAdStyleExitDialog());
        adsManager.loadNativeAd(adsPref.getIsNativeExitDialog(), adsPref.getNativeAdStyleExitDialog());

        Button btnCancel = findViewById(R.id.btn_cancel);
        Button btnExit = findViewById(R.id.btn_exit);

        FloatingActionButton btnRate = findViewById(R.id.btn_rate);
        FloatingActionButton btnShare = findViewById(R.id.btn_share);

        btnCancel.setOnClickListener(view -> showDialog(false));

        btnExit.setOnClickListener(view -> {
            showDialog(false);
            Tools.postDelayed(() -> {
                finish();
                destroyBannerAd();
                destroyAppOpenAd();
            }, 300);
        });

        btnRate.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)));
            showDialog(false);
        });

        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_share) + "\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
            intent.setType("text/plain");
            startActivity(intent);
            showDialog(false);
        });
    }

    private void showDialog(boolean show) {
        if (show) {
            lyt_dialog_exit.setVisibility(View.VISIBLE);
            slideUp(findViewById(R.id.dialog_card_view));
            ObjectAnimator.ofFloat(lyt_dialog_exit, View.ALPHA, 0.1f, 1.0f).setDuration(300).start();
            Tools.fullScreenMode(this, true);
        } else {
            slideDown(findViewById(R.id.dialog_card_view));
            ObjectAnimator.ofFloat(lyt_dialog_exit, View.ALPHA, 1.0f, 0.1f).setDuration(300).start();
            Tools.postDelayed(() -> {
                lyt_dialog_exit.setVisibility(View.GONE);
                Tools.fullScreenMode(this, false);
                Tools.setNavigation(this);
            }, 300);
        }
    }

    public void slideUp(View view) {
        view.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(0, 0, findViewById(R.id.main_content).getHeight(), 0);
        animate.setDuration(300);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    public void slideDown(View view) {
        TranslateAnimation animate = new TranslateAnimation(0, 0, 0, findViewById(R.id.main_content).getHeight());
        animate.setDuration(300);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    private void setupToolbar() {
        Tools.setupToolbar(this, toolbar, getString(R.string.app_name), false);
    }

    private void setupNewToolbar() {
        if (sharedPref.getIsDarkTheme()) {
            lytSearchBar.setCardBackgroundColor(ContextCompat.getColor(this, R.color.color_dark_search_bar));
            btnSearch.setColorFilter(ContextCompat.getColor(this, R.color.color_dark_icon), PorterDuff.Mode.SRC_IN);
            titleToolbar.setTextColor(ContextCompat.getColor(this, R.color.color_dark_icon));
        } else {
            lytSearchBar.setCardBackgroundColor(ContextCompat.getColor(this, R.color.color_light_search_bar));
            btnSearch.setColorFilter(ContextCompat.getColor(this, R.color.color_light_icon), PorterDuff.Mode.SRC_IN);
            titleToolbar.setTextColor(ContextCompat.getColor(this, R.color.color_light_text));
        }

        searchBar.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), ActivitySearch.class));
            destroyBannerAd();
        });

        btnSearch.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), ActivitySearch.class));
            destroyBannerAd();
        });

//        titleToolbar.setText(getString(R.string.app_name));

        if (Config.SET_LAUNCHER_IMAGE_AS_HOME_TOP_RIGHT_ICON) {
            btnMoreOptions.setImageResource(R.mipmap.ic_launcher);
            int padding = getResources().getDimensionPixelSize(R.dimen.padding_small);
            btnMoreOptions.setPadding(padding, padding, padding, padding);
        } else {
            btnMoreOptions.setImageResource(R.drawable.ic_settings);
            int padding = getResources().getDimensionPixelSize(R.dimen.padding_medium);
            btnMoreOptions.setPadding(padding, padding, padding, padding);
            if (sharedPref.getIsDarkTheme()) {
                btnMoreOptions.setColorFilter(ContextCompat.getColor(this, R.color.color_dark_icon), PorterDuff.Mode.SRC_IN);
            } else {
                btnMoreOptions.setColorFilter(ContextCompat.getColor(this, R.color.color_light_icon), PorterDuff.Mode.SRC_IN);
            }
        }
        btnMoreOptions.setOnClickListener(view -> {
            Intent intent;
            if (Config.ENABLE_NEW_APP_DESIGN) {
                intent = new Intent(getApplicationContext(), ActivitySettingsNew.class);
            } else {
                intent = new Intent(getApplicationContext(), ActivitySettings.class);
            }
            startActivity(intent);

        });
    }

}

