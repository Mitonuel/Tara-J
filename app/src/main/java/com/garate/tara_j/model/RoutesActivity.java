package com.garate.tara_j.model;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.garate.tara_j.MapsActivity;
import com.garate.tara_j.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Map;

public class RoutesActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    public EditText searchBar;
    public Button searchButton;
    public ArrayList<Integer> id;
    public ArrayList<Double> lat;
    public ArrayList<Double> lng;
    public Polyline route = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        searchBar = findViewById(R.id.searchBar);
        searchButton = findViewById(R.id.findButton);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseAccess databaseAccess = DatabaseAccess.getInstance(getApplicationContext());
                databaseAccess.open();

                String route = searchBar.getText().toString();
                id = databaseAccess.getDataID(route);
                lat = databaseAccess.getDataLat(route);
                lng = databaseAccess.getDataLng(route);

                for (int z = 0; z < id.size(); z++) {
                    System.out.println(id.get(z) + " " + lat.get(z) + " " + lng.get(z));
                }
                drawRoute(id, lat, lng);

                databaseAccess.close();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // default when nothing is showing up
        mMap = googleMap;
        LatLng davaoDefault = new LatLng(7.0707, 125.6087);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(davaoDefault, 12f));
    }

    public void drawRoute(ArrayList<Integer> id, ArrayList<Double> lat, ArrayList<Double> lng) {
        if (route != null) {
            mMap.clear();
        }
        PolylineOptions options = new PolylineOptions().width(12).color(Color.BLUE).geodesic(true).clickable(true);
        for (int z = 0; z < id.size(); z++) {
            LatLng point = new LatLng(lat.get(z), lng.get(z));
            options.add(point);
        }
        // will try to add caps and pattern
        route = mMap.addPolyline(options);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat.get(0), lng.get(0)), 12.5f));
        // mMap.getMinZoomLevel();
    }



}


