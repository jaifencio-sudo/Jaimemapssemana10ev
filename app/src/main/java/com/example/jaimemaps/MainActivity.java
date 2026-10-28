package com.example.jaimemaps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_REQUEST_CODE = 101;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ImageView ivPhoto;

    private int markerCount = 0;
    private final List<Marker> markers = new ArrayList<>();

    // Captura una imagen con la cámara y la muestra en pantalla
    private final ActivityResultLauncher<Void> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    ivPhoto.setImageBitmap(bitmap);
                    ivPhoto.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivPhoto = findViewById(R.id.ivPhoto);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Cargar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Botones
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> checkLocationPermission());
        findViewById(R.id.btnClearMarkers).setOnClickListener(v -> clearMarkers());
        findViewById(R.id.btnTakePhoto).setOnClickListener(v -> launchCamera());
        ivPhoto.setOnClickListener(v -> ivPhoto.setVisibility(View.GONE)); // tocar la foto la oculta
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Tocar el mapa = añadir un marcador
        mMap.setOnMapClickListener(this::addMarker);

        checkLocationPermission();
    }

    // ---------- Permisos y ubicación ----------

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        mMap.setMyLocationEnabled(true); // punto azul del GPS
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15));
                mMap.addMarker(new MarkerOptions()
                        .position(current)
                        .title("Ubicación actual")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            } else {
                Toast.makeText(this, "No hay ubicación disponible. Activa el GPS.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------- Marcadores ----------

    private void addMarker(LatLng latLng) {
        markerCount++;
        String coords = String.format(Locale.US, "Lat: %.5f, Lng: %.5f",
                latLng.latitude, latLng.longitude);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Marcador " + markerCount)
                .snippet(coords));
        if (marker != null) {
            markers.add(marker);
            marker.showInfoWindow();
        }
    }

    private void clearMarkers() {
        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();
        markerCount = 0;
        Toast.makeText(this, "Marcadores eliminados", Toast.LENGTH_SHORT).show();
    }

    // ---------- Cámara ----------

    private void launchCamera() {
        try {
            takePicture.launch(null);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show();
        }
    }
}
