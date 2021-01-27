package edu.cs4730.androidbeaconlibrarydemo2;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private BeaconManager beaconManager;
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUESTS = 1;
    HomeFragment homeFrag;
    RangeFragment rangeFrag;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        if (item.getItemId() == R.id.navigation_home) {
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.container, homeFrag)
                                    .commit();
                            return true;
                        } else if (item.getItemId() == R.id.navigation_notifications) {
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.container, rangeFrag)
                                    .commit();
                            return true;
                        }
                        return false;
                    }

                }
        );

        logthis("App Starting", 0);
        //check for permissions and start the beacons.
        beaconManager = BeaconManager.getInstanceForApplication(this);
        //added eddystone, since I'm moving from google's beacons to altbeacon.  RedBeacon can broadcast both.
        // Detect the main identifier (UID) frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        // Detect the telemetry (TLM) frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
        // Detect the URL frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));


        homeFrag = new HomeFragment();
        rangeFrag = new RangeFragment();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, homeFrag)
                .commit();

    }


    @Override
    protected void onPause() {
        super.onPause();
        beaconManager.unbind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (allPermissionsGranted()) {
            logthis("Starting beaconManager", 0);
            beaconManager.bind(this);
        } else {
            getRuntimePermissions();
        }
    }

    /**
     * helper method to send strings to both the logcat and to a textview call logger.
     */
    private void logthis(String item, int which) {
        Log.v(TAG, item);
        if (which == 1) { //home
            if (homeFrag != null)
                homeFrag.logthis(item);
        } else if (which == 2) //range?
            if (rangeFrag != null)
                rangeFrag.logthis(item);
    }

    /**
     * This is the beacon "listener" piece.  called from the "this" in bind/unbind.
     */

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllMonitorNotifiers();
        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {

                logthis("I just saw an beacon.", 1);
                // all the region variables appear to be null, because I set them that way?
                logthis("uniqueID is " + region.getUniqueId(), 1);
                //should always be "myMonitoringUniqueId"  see start below.
//                logthis("bluetooth addreess is " + region.getBluetoothAddress());
//                logthis("string? is " + region.toString());
//                logthis(
//                        "I detected a beacon in the region with namespace id " + region.getId1() +
//                                " and instance id: " + region.getId2()
//                );
//                logthis("id3 is " + region.getId3());
            }

            @Override
            public void didExitRegion(Region region) {
                logthis("I no longer see an beacon", 1);

            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                logthis("I have just switched from seeing/not seeing beacons: " + state, 1);

            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
            logthis("beacon monitor listener has been added.", 2);
        } catch (RemoteException e) {
            logthis("FAILED beacon monitor listener has been added." + e.toString(), 2);
        }

        //Now add a ranged beacon info
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                logthis("didRangeBeaconsInRegion called with beacon count:  " + beacons.size(), 2);
                for (Beacon beacon : beacons) {
                    if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                        // This is a Eddystone-UID frame
                        Identifier namespaceId = beacon.getId1();
                        Identifier instanceId = beacon.getId2();
                        logthis("I see a beacon transmitting namespace id: " + namespaceId +
                                " and instance id: " + instanceId +
                                " approximately " + beacon.getDistance() + " meters away.", 2);

                        // Do we have telemetry data?
                        if (beacon.getExtraDataFields().size() > 0) {
                            long telemetryVersion = beacon.getExtraDataFields().get(0);
                            long batteryMilliVolts = beacon.getExtraDataFields().get(1);
                            long pduCount = beacon.getExtraDataFields().get(3);
                            long uptime = beacon.getExtraDataFields().get(4);

                            logthis(
                                    "The above beacon is sending telemetry version " + telemetryVersion +
                                            ", has been up for : " + uptime + " seconds" +
                                            ", has a battery level of " + batteryMilliVolts + " mV" +
                                            ", and has transmitted " + pduCount + " advertisements.", 2);
                            logthis("string " + beacon.toString(),2);
                        }

                    } else if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x10) {
                        // This is a Eddystone-URL frame
                        String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                        logthis("I see a beacon transmitting a url: " + url +
                                " approximately " + beacon.getDistance() + " meters away.", 2);
                    } else {
                        //no clue what we found here.

                        logthis("found a beacon, (not eddy) " + beacon.toString() + " and is approximately " + beacon.getDistance() + " meters away", 2);
                    }

                }
            }
        });


    }

    /**
     * below is all the pieces to get the permissions setup correctly and basically have nothing to do with beacons
     * See the manifest file for all the permissions this app is using.
     */

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            logthis("Starting beaconManager", 0);
            beaconManager.bind(this);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }


}