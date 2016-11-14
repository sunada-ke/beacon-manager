package com.altla.vision.beacon.manager.presentation;

public enum BeaconPrefix {

    IBEACON("beacons/1!"), EDDYSTONE_UID("beacons/3!"), EDDYSTONE_EID("beacons/4!"), ALTBEACON("beacons/5!");

    private String mValue;

    BeaconPrefix(String value) {
        mValue = value;
    }

    public String getValue() {
        return mValue;
    }
}
