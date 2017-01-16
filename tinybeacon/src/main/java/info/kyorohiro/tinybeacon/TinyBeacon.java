package info.kyorohiro.tinybeacon;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TinyBeacon {

    private List<TinyIBeaconInfo> mFoundIBeacon = new LinkedList<TinyIBeaconInfo>();
    private ScanCallback mCurrentScanCallback = null;
    private BluetoothManager mBluetoothManager = null;
    private BluetoothLeScanner mScanner = null;
    private BluetoothLeAdvertiser mAdvertiser = null;
    private android.bluetooth.le.AdvertiseCallback mAdvertiserCallback = null;

    // --
    // Advertiser
    // --
    public static class AdvertiseCallbackParam {
    }

    public static interface AdvertiseCallback {
        void onStartFailure(int errorCode);

        void onStartSuccess(AdvertiseCallbackParam param);
    }

    public synchronized void startAdvertiseIBeacon(Context context, byte[] uuid, int major, int minor, int txPower, final AdvertiseCallback callback) throws Exception {
        if (mAdvertiser != null && mAdvertiserCallback != null) {
            throw new Exception("already run");
        }
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        mAdvertiser = adapter.getBluetoothLeAdvertiser();
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(false);
        dataBuilder.setIncludeDeviceName(false);
        dataBuilder.addManufacturerData(0x4c, TinyIBeaconPacket.makeIBeaconAdvertiseData(uuid, major, minor, txPower));

        try {
            mAdvertiser.startAdvertising(builder.build(), dataBuilder.build(), mAdvertiserCallback = new android.bluetooth.le.AdvertiseCallback() {
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    super.onStartSuccess(settingsInEffect);
                    callback.onStartSuccess(new AdvertiseCallbackParam());
                }

                @Override
                public void onStartFailure(int errorCode) {
                    super.onStartFailure(errorCode);
                    callback.onStartFailure(errorCode);
                }
            });
        } catch (Exception e) {
            callback.onStartFailure(-999);
        }
    }

    public synchronized void stopAdvertiseIBeacon() {
        BluetoothLeAdvertiser _advertiser = mAdvertiser;
        android.bluetooth.le.AdvertiseCallback _advertiserCallback = mAdvertiserCallback;
        mAdvertiser = null;
        mAdvertiserCallback = null;
        if (_advertiser != null && _advertiserCallback != null) {
            _advertiser.stopAdvertising(_advertiserCallback);
        }
    }


    // --
    // Scanner
    // --
    public synchronized void startLescan(Context context) {
        if (mCurrentScanCallback != null) {
            return;
        }
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        mScanner = adapter.getBluetoothLeScanner();
        //
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
//        settingsBuilder.setCallbackType(0xff);
        settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        MyCallback callback = new MyCallback(this);
        final ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
        {
            byte[] manData = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            byte[] mask = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            filterBuilder.setManufacturerData(76, manData, mask);
        }
        List l = new LinkedList() {{
            add(filterBuilder.build());
        }};
        mScanner.startScan(l, settingsBuilder.build(), callback);
        mCurrentScanCallback = callback;

    }

    public synchronized void stopLescan() {
        BluetoothLeScanner _scanner = mScanner;
        ScanCallback _callback = mCurrentScanCallback;
        mScanner = null;
        mCurrentScanCallback = null;
        if (_callback != null) {
            _scanner.stopScan(_callback);
        }
    }

    public List<TinyIBeaconInfo> getFoundedIBeacon() {
        return mFoundIBeacon;
    }

    public String getFoundedIBeaconAsJSONText() throws JSONException {
        JSONObject ret = new JSONObject();
        List<JSONObject> t = new LinkedList<JSONObject>();
        for (TinyIBeaconInfo e : mFoundIBeacon) {
            t.add(e.toJsonString());
        }
        ret.put("founded", new JSONArray(t));
        ret.put("time", TinyIBeaconInfo.getTime());
        return ret.toString();
    }

    public void clearFoundedIBeacon() throws JSONException {
        mFoundIBeacon.clear();
    }


    //
    //
    //
    static class MyCallback extends ScanCallback {
        TinyBeacon mParent = null;

        MyCallback(TinyBeacon parent) {
            mParent = parent;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //android.util.Log.v("beacon", "###SA## manu:" + result.getDevice().getType());
            //android.util.Log.v("beacon", "###SA## manu:" +result.getScanRecord().getManufacturerSpecificData());
            //android.util.Log.v("beacon", "###SA## scanResult type:" + callbackType + " ,result: " + result.toString());
            long t = TinyIBeaconInfo.getTime();//System.currentTimeMillis();
            List<TinyAdPacket> ad = TinyAdPacket.parseScanRecord(result.getScanRecord().getBytes());
            for (TinyAdPacket a : ad) {
                if (TinyIBeaconPacket.isIBeacon(a)) {
                    //   android.util.Log.v("KY", "uuid:" + TinyIBeaconPacket.getUUIDHexString(a) + ", major:" + TinyIBeaconPacket.getMajorAsIBeacon(a) + ", minor:" + TinyIBeaconPacket.getMinorAsIBeacon(a) + ",crssi:" + TinyIBeaconPacket.getCalibratedRSSIAsIBeacon(a));
                    TinyIBeaconInfo i = TinyIBeaconInfo.containes(mParent.mFoundIBeacon, a);
                    if (null == i) {
                        TinyIBeaconInfo ex = new TinyIBeaconInfo(a, result.getRssi(), t);
                        mParent.mFoundIBeacon.add(ex);
                    } else {
                        i.update(result.getRssi(), t);
                    }
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            StringBuilder builder = new StringBuilder();
            for (ScanResult r : results) {
                builder.append("re " + r + ",b:" + r.getScanRecord().getBytes()+", rssi:"+r.getRssi()+"\n");
            }
            android.util.Log.v("beacon", "###S## batchScanResult type:" + builder.toString());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            android.util.Log.v("beacon", "###S## scanFailed errorCode:" + errorCode);
        }
    }
}
