package com.altla.vision.beacon.manager.presentation.view.activity;

import com.altla.vision.beacon.manager.presentation.application.App;
import com.altla.vision.beacon.manager.R;
import com.altla.vision.beacon.manager.android.SnackBarUtils;
import com.altla.vision.beacon.manager.presentation.di.component.UserComponent;
import com.altla.vision.beacon.manager.presentation.di.module.ActivityModule;
import com.altla.vision.beacon.manager.presentation.BeaconStatus;
import com.altla.vision.beacon.manager.presentation.presenter.ActivityPresenter;
import com.altla.vision.beacon.manager.presentation.view.ActivityView;
import com.altla.vision.beacon.manager.presentation.view.fragment.BeaconEditFragment;
import com.altla.vision.beacon.manager.presentation.view.fragment.BeaconRegisterFragment;
import com.altla.vision.beacon.manager.presentation.view.fragment.BeaconRegisteredFragment;
import com.altla.vision.beacon.manager.presentation.view.fragment.NearbyBeaconFragment;
import com.altla.vision.beacon.manager.presentation.view.fragment.ProjectSwitchFragment;
import com.altla.vision.beacon.manager.presentation.view.fragment.SignInFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ActivityView {

    @Inject
    ActivityPresenter mActivityPresenter;

    @BindView(R.id.fragment_place_holder)
    RelativeLayout mMainLayout;

    private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);

    private static final int REQUEST_CODE_ACCESS_FINE_LOCATION = 1;

    private static final int REQUEST_CODE_RECOVERABLE_AUTH = 2;

    private ActionBarDrawerToggle mToggle;

    private DrawerLayout drawerLayout;

    private UserComponent mUserComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Dagger。
        mUserComponent = App.getApplicationComponent(this)
                .userComponent(new ActivityModule(this));
        mUserComponent.inject(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mToggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mToggle.syncState();
        drawerLayout.addDrawerListener(mToggle);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ButterKnife.bind(this);

        mActivityPresenter.onCreateView(this);
        mActivityPresenter.checkAuthentication(getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        mActivityPresenter.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RECOVERABLE_AUTH) {
            mActivityPresenter.saveToken(getApplicationContext());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // TODO: Android 6 系の Permission 対応。
        if (requestCode != REQUEST_CODE_ACCESS_FINE_LOCATION) {
            LOGGER.warn("Permission denied");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_nearby_beacon:
                new FragmentController(getSupportFragmentManager()).showBeaconScanFragment();
                break;
            case R.id.nav_registered:
                new FragmentController(getSupportFragmentManager()).showBeaconRegisteredFragment();
                break;
            case R.id.nav_switch_project:
                new FragmentController(getSupportFragmentManager()).showProjectSwitchFragment();
                break;
            case R.id.nav_sign_out:
                mActivityPresenter.onSignOut(MainActivity.this);
                break;
            default:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void showSignInFragment() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        FragmentController controller = new FragmentController(manager);
        controller.showSignInFragment();
    }

    @Override
    public void showBeaconScanFragment() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mToggle.setDrawerIndicatorEnabled(true);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        FragmentController fragmentController = new FragmentController(getSupportFragmentManager());
        fragmentController.showBeaconScanFragment();
    }

    @Override
    public void showUserRecoverableAuthDialog(Intent intent) {
        startActivityForResult(intent, REQUEST_CODE_RECOVERABLE_AUTH);
    }

    @Override
    public void showSnackBar(int resId) {
        SnackBarUtils.showShort(mMainLayout, resId);
    }

    public void setDrawerIndicatorEnabled(boolean enabled) {
        // ドロワーの表示制御。
        mToggle.setDrawerIndicatorEnabled(enabled);
//        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void showBeaconRegisterFragment(String type, String hexId, String base64EncodedId) {
        FragmentController fragmentController = new FragmentController(getSupportFragmentManager());
        fragmentController.showBeaconRegisterFragment(type, hexId, base64EncodedId);
    }

    public void showBeaconRegisteredFragment() {
        FragmentController fragmentController = new FragmentController(getSupportFragmentManager());
        fragmentController.showBeaconRegisteredFragment();
    }

    public void showBeaconEditFragment(String name, BeaconStatus beaconStatus) {
        FragmentController fragmentController = new FragmentController(getSupportFragmentManager());
        fragmentController.showBeaconEditFragment(name, beaconStatus);
    }

    public void refreshToken() {
        mActivityPresenter.refreshToken(getApplicationContext());
    }

    public static UserComponent getUserComponent(@NonNull Fragment fragment) {
        return ((MainActivity) fragment.getActivity()).mUserComponent;
    }

    private static class FragmentController {

        private static final String SIGN_IN_FRAGMENT_TAG = SignInFragment.class.getSimpleName();

        private static final String BEACON_REGISTER_FRAGMENT_TAG = BeaconRegisterFragment.class.getSimpleName();

        private static final String NEARBY_BEACON_FRAGMENT_TAG = NearbyBeaconFragment.class.getSimpleName();

        private static final String BEACON_REGISTERED_FRAGMENT_TAG = BeaconRegisteredFragment.class.getSimpleName();

        private static final String BEACON_EDIT_FRAGMENT_TAG = BeaconRegisteredFragment.class.getSimpleName();

        private static final String PROJECT_SWITCH_FRAGMENT_TAG = ProjectSwitchFragment.class.getSimpleName();

        private FragmentManager mFragmentManager;

        public FragmentController(FragmentManager fragmentManager) {
            mFragmentManager = fragmentManager;
        }

        private void showSignInFragment() {
            SignInFragment fragment = SignInFragment.newInstance();
            replaceFragment(R.id.fragment_place_holder, fragment, SIGN_IN_FRAGMENT_TAG);
        }

        private void showBeaconScanFragment() {
            NearbyBeaconFragment fragment = NearbyBeaconFragment.newInstance();
            replaceFragment(R.id.fragment_place_holder, fragment, NEARBY_BEACON_FRAGMENT_TAG);
        }

        private void showBeaconRegisterFragment(String type, String hexId, String base64EncodedId) {
            BeaconRegisterFragment fragment = BeaconRegisterFragment.newInstance(type, hexId, base64EncodedId);
            replaceFragment(R.id.fragment_place_holder, fragment, BEACON_REGISTER_FRAGMENT_TAG);
        }

        private void showBeaconRegisteredFragment() {
            BeaconRegisteredFragment fragment = BeaconRegisteredFragment.newInstance();
            replaceFragment(R.id.fragment_place_holder, fragment, BEACON_REGISTERED_FRAGMENT_TAG);
        }

        private void showBeaconEditFragment(String name, BeaconStatus beaconStatus) {
            BeaconEditFragment beaconEditFragment = BeaconEditFragment.newInstance(name, beaconStatus);
            replaceFragment(R.id.fragment_place_holder, beaconEditFragment, BEACON_EDIT_FRAGMENT_TAG);
        }

        private void showProjectSwitchFragment() {
            ProjectSwitchFragment fragment = ProjectSwitchFragment.newInstance();
            replaceFragment(R.id.fragment_place_holder, fragment, PROJECT_SWITCH_FRAGMENT_TAG);
        }

        private void replaceFragment(@IdRes int containerViewId, Fragment fragment, String tag) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.addToBackStack(tag);
            fragmentTransaction.replace(containerViewId, fragment, tag);
            fragmentTransaction.commit();
        }
    }
}
