package com.garate.tara_j;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.garate.tara_j.directionHelpers.FetchURL;
import com.garate.tara_j.directionHelpers.TaskLoadedCallback;
import com.garate.tara_j.model.RoutesActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.io.IOException;
import java.util.List;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, TaskLoadedCallback {
    /* TODO (Priority): Add Drawer - done
                        Add fragments - 50%
                        Calculate fare and fare view
       TODO: Modify Polyline color
             Modify Camera view after setting LatLng bounds (origin, destination)
             Refine polyline on LatLng change
    */

    //CLASS DECLARATION
    public static GoogleMap mMap;
    public static LatLng origin = null;
    public static LatLng destination = null;
    public static Marker markerDestination = null;
    public static Marker markerOrigin = null;
    public static TextView searchDestination;
    public static TextView searchOrigin;
    public static String nameOrigin;
    public static String nameDestination;
    public Button buttonOrigin;
    public Button buttonDestination;
    private boolean isFirstTime;
    public static LatLng latLng;
    LocationManager locationManager;

    // DRAWER DECLARATIONS
    public Button getButtonDrawer;

    // DIRECTIONS API DECLARATIONS
    Button getButtonDirection;
    private Polyline currentPolyline;

    //FOR LOG PURPOSES
    private static final String TAG = "RoutesActivity";

    //0 for markerOrigin & 1 for markerDestination
    public static int forMarkerOriginFlag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialize Places. For simplicity, the API key is hard-coded. In a production
        // environment we recommend using a secure mechanism to manage API keys.
        String apiKey = getString(R.string.google_maps_key);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }

        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);

        //INITIALIZE BUTTONS
        buttonOrigin = findViewById(R.id.buttonOrigin);
        buttonDestination = findViewById(R.id.buttonDestination);
        getButtonDirection = findViewById(R.id.buttonTara);
        getButtonDrawer = findViewById(R.id.buttonDrawer);

        buttonOrigin.setOnClickListener(v -> {
            openOrigin();
        });
        buttonDestination.setOnClickListener(v -> {
            openDestination();
        });
        getButtonDirection.setOnClickListener(v -> new FetchURL(MapsActivity.this).execute(getUrl(markerOrigin.getPosition(), markerDestination.getPosition(), "transit"), "transit"));

        //INITIALIZE TEXTS
        searchOrigin = findViewById(R.id.origin_name);
        searchDestination = findViewById(R.id.destination_name);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // DRAWER STUFF
        drawerLayout();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Marker
        isFirstTime = true;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Log.d("mylog", "Added Markers");

        //FIRE permissions
        Dexter.withActivity(this).withPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(new MultiplePermissionsListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                // CHECK if all permissions are GRANTED
                if (report.areAllPermissionsGranted()) {
                    ShowDefault();
                    Geolocation();
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setCompassEnabled(false);
                }
                else {
                    showSettingsDialog();
                    ShowDefault();
                }

                // CHECK if any permission is PERMANENTLY DENIED
                if (report.isAnyPermissionPermanentlyDenied()) {
                    showSettingsDialog();
                    ShowDefault();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest(); //PROMPT for the permission
            }
        }).withErrorListener(error -> Toast.makeText(getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show()).onSameThread().check(); //CHECKS FOR ERROR

        //ADD LONG CLICK LISTENERS THAT CAN ALSO SET FOR ORIGIN AND DESTINATION
        LongClickListeners();
    }

    @Override
    protected void onRestart() {
    //TODO Auto-generated method stub
        super.onRestart();
        if (origin != null && !"Pin location".equals(nameOrigin)) {
            GetNameOrigin();
        }

        if (destination != null && !"Pin location".equals(nameDestination)) {
            GetNameDestination();
        }
    }

    private void GetNameOrigin() {
        if (nameOrigin != null) {
            searchOrigin.setText(nameOrigin);
        }
    }

    private void GetNameDestination() {
        if (nameDestination != null) {
            searchDestination.setText(nameDestination);
        }
    }

    //METHOD for the GEOLOCATION
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void Geolocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //CHECK if network provider is ENABLED
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                @SuppressWarnings("MoveFieldAssignmentToInitializer")
                @Override
                public void onLocationChanged(Location location) {

                    //GET latitude
                    double latitude = location.getLatitude();
                    //GET longitude
                    double longitude = location.getLongitude();
                    //INSTANTIATE LatLng class
                    latLng = new LatLng(latitude, longitude);
                    //INSTANTIATE Geocoder class
                    Geocoder geocoder = new Geocoder(getApplicationContext());

                    try {
                        List<Address> adressList = geocoder.getFromLocation(latitude, longitude, 1);
                        String str = adressList.get(0).getSubLocality() + ", ";
                        str += adressList.get(0).getLocality() + ", ";
                        str += adressList.get(0).getCountryName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }
        //GPS Backup
        else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    //GET latitude
                    double latitude = location.getLatitude();
                    //GET longitude
                    double longitude = location.getLongitude();
                    //INSTANTIATE LatLng class
                    latLng = new LatLng(latitude, longitude);
                    //INSTANTIATE Geocoder clas
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    try {
                        List<Address> adressList = geocoder.getFromLocation(latitude, longitude, 1);
                        String str = adressList.get(0).getSubLocality()+ ", ";
                        str += adressList.get(0).getLocality() + ", ";
                        str += adressList.get(0).getCountryName();
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(latLng).title(str));
//                        if (isFirstTime == true) {
//                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f));
//                        }
//                        isFirstTime = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }
    }

    private void LongClickListeners() {
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                switch (forMarkerOriginFlag) {
                    case 0:
                        //SET NEW ORIGIN BASED ON PIN LOCATION
                        origin = latLng;
                        searchOrigin.setText("Pin Location");

                        if (markerOrigin != null) {
                            markerOrigin.remove();
                            markerOrigin = null;
                        }

                        if (markerOrigin == null) {
                            markerOrigin = MapsActivity.mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title("ORIGIN")
                                    .snippet("Pin location")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_origin)));
                            Toast.makeText(getApplicationContext(), "Origin set at pin location. Coordinates: " + latLng, Toast.LENGTH_LONG).show();
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 13f));
                        }
                        forMarkerOriginFlag = 1;
                        break;
                    case 1:
                        //SET NEW DESTINATION BASED ON PIN LOCATION
                        destination = latLng;
                        searchDestination.setText("Pin Location");

                        if (markerDestination != null) {
                            markerDestination.remove();
                            markerDestination = null;
                        }

                        if (markerDestination == null) {
                            markerDestination = MapsActivity.mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title("DESTINATION")
                                    .snippet("Pin location")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_destination)));
                            Toast.makeText(getApplicationContext(), "Destination set at pin location. Coordinates: " + latLng, Toast.LENGTH_LONG).show();
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destination, 13f));
                        }
                        forMarkerOriginFlag = 0;
                        break;

                }
            }
        });
    }

    //SET ORIGIN INSTANCE FOR BUTTON
    private void openOrigin() {
        Intent intent = new Intent(this, Origin.class);
        startActivity(intent);
    }

    //SET DESTINATION INSTANCE FOR BUTTON
    public void openDestination() {
        Intent intent = new Intent(this, Destination.class);
        startActivity(intent);

    }

    //DEFAULT MAP VIEW
    private void ShowDefault() {
        LatLng davaoDefault = new LatLng(7.0707, 125.6087);
        if (isFirstTime == true) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(davaoDefault, 12f));
        }
        isFirstTime = false;
    }

    //OPEN SETTINGS in case the user DENIES
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("Need Permissions");
        builder.setMessage("TaraJ needs location and Storage permissions to function. You can grant them in the application's settings. (TaraJ > Permissions)");
        builder.setPositiveButton("OPEN SETTINGS", (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    // DIRECTIONS METHODS
    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        Log.d("mylog", "URL " + url);
        return url;

    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
    }

    // DRAWER
    public void drawerLayout() {
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.white)
                .build();

        Drawer result = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(headerResult)
                .withHasStableIds(true)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Home").withIdentifier(1).withSelectable(false),
                        new PrimaryDrawerItem().withName("Routes").withIdentifier(2).withSelectable(false),
                        new PrimaryDrawerItem().withName("Fare").withIdentifier(3).withSelectable(false),
                        new PrimaryDrawerItem().withName("About").withIdentifier(4).withSelectable(false)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            Intent intent = null;
                            if (drawerItem.getIdentifier() == 1) {
                                intent = new Intent(MapsActivity.this, MapsActivity.class);
                            } else if (drawerItem.getIdentifier() == 2) {
                                intent = new Intent(MapsActivity.this, RoutesActivity.class);
                            } else if (drawerItem.getIdentifier() == 3) {
                                System.out.println("hello fares");
                            } else if (drawerItem.getIdentifier() == 4) {
                                System.out.println("hello about");
                            }
                            if (intent != null) {
                                MapsActivity.this.startActivity(intent);
                            }
                        }
                        return false;
                    }
                })
                .build();

        getButtonDrawer.setOnClickListener(v -> {
            result.openDrawer();
        });

    }
}
