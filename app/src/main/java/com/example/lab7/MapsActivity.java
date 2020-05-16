package com.example.lab7;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener{

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;
    List<Marker> markerList;

    private final String MAPS_JSON_FILE = "maps.json";

    private boolean accelerate = false;
    private FloatingActionButton fabo = null;
    private FloatingActionButton fabx = null;
    private TextView acceleration = null;
    private SensorManager sensorManager = null;
    private Button clearMemory = null;
    List<LatLng> parametersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        markerList = new ArrayList<>();
        parametersList = new ArrayList<>();

        fabo = (FloatingActionButton) findViewById(R.id.fabo);
        fabx = (FloatingActionButton) findViewById(R.id.fabx);
        acceleration = (TextView) findViewById(R.id.acceleration_view);
        fabo.setVisibility(View.INVISIBLE);
        fabx.setVisibility(View.INVISIBLE);
        acceleration.setVisibility(View.INVISIBLE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        clearMemory = (Button) findViewById(R.id.clear_memory_button);

        fabo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!accelerate)
                    accelerate = true;
                else accelerate = false;
            }
        });

        fabx.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                hide(fabo);
                hide(fabx);
                hide(acceleration);
            }
        });

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(acceleration != null && accelerate){
                    acceleration.setText(String.format("Acceleration:\n x: %.5f, y: %.5f", event.values[0], event.values[1]));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);

        clearMemory.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                for(Marker i: markerList){
                    i.remove();
                }
                markerList.clear();
                parametersList.clear();
                mMap.moveCamera(CameraUpdateFactory.zoomTo(0f));

                if(fabo != null && fabx != null && acceleration != null){
                    hide(fabo);
                    hide(fabx);
                    hide(acceleration);
                }
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        restoreFromJson();
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastLocation = fusedLocationClient.getLastLocation();

        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null && mMap != null){
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title(getString(R.string.last_known_loc_msg)));
                }
            }
        });

        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        LatLng posXY = new LatLng(latLng.latitude, latLng.longitude);

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(posXY)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .alpha(0.8f)
                .title(String.format("Position: (%.2f, %.2f)", latLng.latitude, latLng.longitude)));
        markerList.add(marker);
        parametersList.add(posXY);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(fabo != null && fabx !=null && acceleration != null){
            show(fabo);
            show(fabx);
            show(acceleration);
        }
        return false;
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPersmission")
    private void startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void createLocationCallback(){
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                if(locationResult != null){
                    if(gpsMarker != null)
                        gpsMarker.remove();
                    Location location = locationResult.getLastLocation();
                    gpsMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .alpha(0.8f)
                    .title("Current location"));
                }
            }
        };
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates(){
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void zoomInClick(View v){
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v){
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    private void saveMapsToJson(List<LatLng> markers){
        Gson gson = new Gson();
        String listJson = gson.toJson(markers);
        FileOutputStream outputStream;
        try{
            outputStream = openFileOutput(MAPS_JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
            Log.v("saving", "File saved");
        } catch (FileNotFoundException e){
            e.printStackTrace();
            Log.v("saving", "File not saved - not found file");
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void restoreFromJson(){
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try{
            inputStream = openFileInput(MAPS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while((n = reader.read(buf)) >= 0){
                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<LatLng>>(){}.getType();
            List<LatLng> o = gson.fromJson(readJson, collectionType);
            if(o != null){
                markerList.clear();
                parametersList.clear();

                for(LatLng i: o){
                    LatLng latLng = new LatLng(i.latitude, i.longitude);
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            .alpha(0.8f)
                            .title("Restored marker");
                    try{
                        markerList.add(mMap.addMarker(markerOptions));
                        parametersList.add(latLng);
                    } catch (Exception e){
                        Log.e("loading", e.getMessage());
                    }
                }
            } else{
                Log.v("loading", "Empty file");
            }
        } catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy(){
        saveMapsToJson(parametersList);
        super.onDestroy();
    }

    public void show(final View view){
        view.animate().translationY(0).alpha(1f).setDuration(250).setListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    public void hide(final View view){
        view.animate().translationY(0).alpha(0f).setDuration(250).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }
}