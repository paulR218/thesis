package com.example.thesis;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.thesis.databinding.ActivityDriverMapBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private ActivityDriverMapBinding binding;

    private Button mLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDriverMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //logout button function which logs out the user
        mLogout = (Button) findViewById(R.id.logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                //when the user clicks logout, the program switches from DriverMapActivity page to
                //MainActivity page
                Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);

        /*// Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        mLastLocation = location;
        //gets the latitude and longitude of the phone
        LatLng latlng = new LatLng(location.getLatitude(),location.getLongitude());
        //moves the camera of the phone to center the location of the driver
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        //get the uid of the user currently logged in the application
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //gets the data of the available driver/s
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");

        //sets the reference on where you want the GeoFire to be storing to
        GeoFire geoFire = new GeoFire(ref);
        //stores the latitude and longitude into the userId which is stored in the DriverAvailable database
        geoFire.setLocation(userId,new GeoLocation(location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LocationListener.super.onStatusChanged(provider, status, extras);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }
    //This tells that the user is currently using the application
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //when connected to google maps this requests the location of the phone
        mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdateDelayMillis(1000)
                .build();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this::onLocationChanged);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    //this will be called when the user minimized or closed the application
    protected void onStop(){
        super.onStop();

        //get the uid of the user currently logged in the application
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //gets the data of the available driver/s
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");

        //sets the reference on where you want the GeoFire to be storing to
        GeoFire geoFire = new GeoFire(ref);
        //tells the database that the user is no longer available so the location of the phone will
        //be removed in the database
            geoFire.removeLocation(userId);
    }
}