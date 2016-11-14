package com.altla.vision.beacon.manager.presentation.presenter;

import com.altla.vision.beacon.manager.R;
import com.altla.vision.beacon.manager.presentation.presenter.mapper.NearbyBeaconModelMapper;
import com.altla.vision.beacon.manager.presentation.presenter.model.NearbyBeaconModel;
import com.altla.vision.beacon.manager.presentation.view.NearbyBeaconItemView;
import com.altla.vision.beacon.manager.presentation.view.NearbyBeaconView;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.support.annotation.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public final class NearbyBeaconPresenter extends BasePresenter<NearbyBeaconView> implements BeaconConsumer, AuthFailure {

    private static final Logger LOGGER = LoggerFactory.getLogger(NearbyBeaconPresenter.class);

    private NearbyBeaconModelMapper mNearbyBeaconModelMapper = new NearbyBeaconModelMapper();

    private final List<NearbyBeaconModel> mNearbyBeaconModels = new ArrayList<>();

    private Context mContext;

    private BeaconManager mBeaconManager;

    private boolean mIsScanning;

    @Inject
    NearbyBeaconPresenter(Context context) {
        mContext = context;
    }

    @Override
    public void onResume() {
        getView().showTitle(R.string.title_beacon_scan);
    }

    @Override
    public void onStop() {
        if (mBeaconManager != null) {
            mBeaconManager.unbind(this);
        }
        mCompositeSubscription.unsubscribe();

        mIsScanning = false;
    }

    public void checkBleEnabled(Activity activity) {
        BluetoothManager bluetoothManager =
                (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            LOGGER.warn("Bluetooth is disable");
            getView().showBleEnabledActivity();
        } else {
            mBeaconManager = BeaconManager.getInstanceForApplication(mContext);
        }
    }

    public void setBeaconManager() {
        mBeaconManager = BeaconManager.getInstanceForApplication(mContext);
        mBeaconManager.bind(this);
    }

    public void onCreateItemView(NearbyBeaconItemView nearbyBeaconItemView) {
        NearbyBeaconItemPresenter nearbyBeaconItemPresenter = new NearbyBeaconItemPresenter();
        nearbyBeaconItemPresenter.onCreateItemView(nearbyBeaconItemView);
        nearbyBeaconItemView.setItemPresenter(nearbyBeaconItemPresenter);
    }

    public int getItemCount() {
        return mNearbyBeaconModels.size();
    }

    public void onRefresh() {
        if (mIsScanning) {
            return;
        }

        if (mBeaconManager == null) {
            getView().showSnackBar(R.string.error_ble_disable);
        } else {
            getView().removeAllItems(mNearbyBeaconModels.size());

            mNearbyBeaconModels.clear();
            mIsScanning = true;

            mBeaconManager.bind(this);

            // In consideration of the battery, so as only to scan only a certain period of time by the timer.
            ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
            executor.schedule(() -> {
                // To stop the scan after 5 seconds.
                mBeaconManager.unbind(this);
                mIsScanning = false;

                if (mNearbyBeaconModels.size() == 0) {
                    getView().showSnackBar(R.string.message_not_found);
                }
            }, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onBeaconServiceConnect() {

        // Start range scan.
        mBeaconManager.addRangeNotifier((beacons, region) -> {
            if (beacons.size() > 0) {
                Subscription subscription = Observable.from(beacons)
                        .filter(beacon -> {
                            int serviceUuid = beacon.getServiceUuid();
                            int beaconTypeCode = beacon.getBeaconTypeCode();
                            return serviceUuid == 0xfeaa && beaconTypeCode == 0x00 || serviceUuid == 0xfeaa && beaconTypeCode == 0x30 || serviceUuid == -1 && beaconTypeCode == 533;
                        }).map(filteredBeacon -> mNearbyBeaconModelMapper.map(filteredBeacon))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(scannedModel -> {
                            for (NearbyBeaconModel model : mNearbyBeaconModels) {
                                if (model.mHexId.equals(scannedModel.mHexId)) {
                                    return;
                                }
                            }

                            mNearbyBeaconModels.add(scannedModel);
                            getView().updateItems();
                        }, e -> {
                            LOGGER.error("Failed to scan beacons", e);
                            getView().showSnackBar(R.string.error_scan_beacons);
                        });
                mCompositeSubscription.add(subscription);
            }
        });

        try {
            // Start ranging any beacons(iBeacon/Eddystone/AltBeacon). To see App class.
            mBeaconManager.startRangingBeaconsInRegion(new Region("uniqueId", null, null, null));
        } catch (RemoteException e) {
            LOGGER.error("Failed to start ranging beacons", e);
            getView().showSnackBar(R.string.error_process);
        }
    }

    @Override
    public Context getApplicationContext() {
        return mContext;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        mContext.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return mContext.bindService(intent, serviceConnection, i);
    }

    @Override
    public void refreshToken() {
        getView().refreshToken();
    }

    public final class NearbyBeaconItemPresenter extends BaseItemPresenter<NearbyBeaconItemView> {

        @Override
        public void onBind(@IntRange(from = 0) int position) {
            getItemView().showItem(mNearbyBeaconModels.get(position));
        }

        public void onItemClick(NearbyBeaconModel model) {
            getView().showBeaconRegisterFragment(model.mType.getValue(), model.mHexId, model.mBase64EncodedId);
        }
    }
}
