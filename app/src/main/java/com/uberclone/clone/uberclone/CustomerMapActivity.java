package com.uberclone.clone.uberclone;

import android.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private static final String TAG = "CustomerMapActivity";

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button mLoggingBtn;
    private Button mRequestBtn;
    private Button mSettingBtn;
    private Button mHistoryBtn;
    private LatLng pickUpLocation;
    private Boolean requestBol = false;
    private Boolean requestServiceBol = false;
    private Marker pickUpMarker;
    SupportMapFragment mapFragment;
    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;
    private GeoQuery geoQuery;

    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    private String destination;
    private String requestService;
    private int LOCATION_REQUEST_CODE = 1;
    private LatLng destinationLatlng;

    private LinearLayout mDrvierInfo;
    private ImageView mDrvierProfileImage;
    private TextView mDrvierCar;
    private TextView mDriverName;
    private TextView mDriverPhone;

    private RatingBar mRatingBar;

    private RadioGroup mRadioGroup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
        }else{
            mapFragment.getMapAsync(this);
        }

        destinationLatlng = new LatLng(0.0,0.0);

        mLoggingBtn = (Button) findViewById(R.id.logout);
        mRequestBtn = (Button) findViewById(R.id.request);
        mHistoryBtn = (Button) findViewById(R.id.history);
        mSettingBtn = (Button) findViewById(R.id.setting);

        mDrvierInfo = (LinearLayout) findViewById(R.id.drvierInfo);
        mDrvierProfileImage = (ImageView) findViewById(R.id.driverPorfileImage);
        mDriverName= (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDrvierCar = (TextView) findViewById(R.id.driverCar);

        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);
        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.uberX);


        mRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                     if (requestBol) {
                         endRide();
                     } else {
                         int selectedID = mRadioGroup.getCheckedRadioButtonId();

                         final RadioButton radioButton = (RadioButton) findViewById(selectedID);

                         if(radioButton.getText() == null){
                             return;
                         }

                         requestService = radioButton.getText().toString();

                         requestBol = true;

                         String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                         DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
                         GeoFire mGeoFire = new     GeoFire(ref);
                         mGeoFire.setLocation(userID, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                         pickUpLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                         pickUpMarker = mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Pick up Point").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));

                         mRequestBtn.setText("Getting your Driver");
                         getClosedDriver();
                     }  }
        });


                mLoggingBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(CustomerMapActivity.this, MainActivity.class));
                        finish();
                        return;
                    }
                });

                mSettingBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(CustomerMapActivity.this,CustomerSettingActivity.class);
                        startActivity(intent);
                        return;
                    }
                });


                mHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomerMapActivity.this,HistoryActivity.class);
                intent.putExtra("customerOrDriver","Customres");
                startActivity(intent);
                return;
            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName());

                destination = place.getName().toString();
                destinationLatlng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

    List<Marker> markerList = new ArrayList<Marker>();

    private boolean getDriverArroundStarted = false;
    private void getDriverArround(){
        getDriverArroundStarted = true;
        DatabaseReference driversLocations = FirebaseDatabase.getInstance().getReference().child("DriverAvailable");
        if(driversLocations != null) {
            GeoFire geoFire = new GeoFire(driversLocations);
            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()), 10000);
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    for(Marker marker : markerList){
                        if(marker.getTag().equals(key)){
                            return;
                        }
                    }

                    LatLng driverLocation = new LatLng(location.latitude,location.longitude);

                    Marker mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation).title(key));
                    mDriverMarker.setTag(key);
                    markerList.add(mDriverMarker);
                }

                @Override
                public void onKeyExited(String key) {
                    for(Marker markerIt : markerList) {
                        if(markerIt.getTag().equals(key)){
                            markerIt.remove();
                            markerList.remove(markerIt);
                            return;
                        }
                    }
                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                    for(Marker markerIt : markerList) {
                        if(markerIt.getTag().equals(key)){
                            markerIt.setPosition(new LatLng(location.latitude,location.longitude));
                        }
                    }
                }

                @Override
                public void onGeoQueryReady() {

                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                }
            });
        }
    }

    private void getDriverInfo() {
        mDrvierInfo.setVisibility(View.VISIBLE);

//        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mDriverDatabase = FirebaseDatabase.getInstance().getReference("Users").child("Drivers").child(driverFoundID);
        mDriverDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()   && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!= null){
                        mDriverName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!= null){
                        mDriverPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("car")!= null){
                        mDrvierCar.setText(map.get("car").toString());
                    }
                    if(map.get("profileImageUrl")!= null){
                        final String mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplicationContext()).load(mProfileImageUrl).into(mDrvierProfileImage);
                    }

                    float ratingSum = 0;
                    float ratingTotal = 0;
                    float avgRating = 0;
                    for (DataSnapshot child : dataSnapshot.child("rating").getChildren()){
                        ratingSum = ratingSum + Float.valueOf(child.getValue().toString());
                        ratingTotal++;
                    }
                    if(ratingTotal != 0){
                        avgRating = ratingSum/ratingTotal;
                        mRatingBar.setRating(avgRating);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private  DatabaseReference driveHasEndedRef;
    private ValueEventListener driverHasEndedRefListner;
    private void getHasRideEnded() {
//        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference("Users").child("Drivers").child(driverFoundID).child("CustomerRequest").child("CustomerRideID");
        driverHasEndedRefListner = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                }
                else{
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private  void endRide(){
        requestBol = false;
        requestServiceBol = false;

        geoQuery.removeAllListeners();

        if(driverLocationRef != null){
        driverLocationRef.removeEventListener(driverLocationRefListener);
        driveHasEndedRef.removeEventListener(driverHasEndedRefListner);

        }
        if(driverFoundID != null){
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("CustomerRequest");
            driverRef.removeValue();
            driverFoundID = null;
        }
        driverFound = false;
        radius = 1;

        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
        GeoFire mGeoFire = new GeoFire(ref);
        mGeoFire.removeLocation(userID);
        if(pickUpMarker != null){
            pickUpMarker.remove();
        }
        if(mDriverMarker!= null){
            mDriverMarker.remove();
        }
        mRequestBtn.setText("Call Uber ");
        mDrvierInfo.setVisibility(View.GONE);
        mDrvierProfileImage.setImageResource(R.drawable.ic_launcher_background);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDrvierCar.setText("");

    }

    public void getClosedDriver() {
        final boolean[] driverAvailabe = {true};
        DatabaseReference driverLcation = FirebaseDatabase.getInstance().getReference().child("DriverAvailable");
        driverLcation.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    Toast.makeText(CustomerMapActivity.this, "No Driver Available", Toast.LENGTH_SHORT).show();
                    requestBol = false;
                    driverAvailabe[0] = false;
                    mRequestBtn.setText("Call Uber");
                    if(pickUpMarker != null){
                        pickUpMarker.remove();
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
                        GeoFire mGeoFire = new GeoFire(ref);
                        mGeoFire.removeLocation(FirebaseAuth.getInstance().getUid());
                    }
                    return;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        GeoFire geoFire = new GeoFire(driverLcation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUpLocation.latitude, pickUpLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestBol && !requestServiceBol) {

                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                                Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                                if(driverFound){
                                    return;
                                }

                                if(driverMap.get("service").equals(requestService)){
                                    driverFound = true;
                                    requestServiceBol = true;

                                    driverFoundID = dataSnapshot.getKey();

                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("CustomerRequest");
                                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();

                                    map.put("CustomerRideID", customerID);
                                    map.put("destination", destination);
                                    map.put("destinationLat", destinationLatlng.latitude);
                                    map.put("destinationLng", destinationLatlng.longitude);

                                    driverRef.updateChildren(map);

                                    getDriverLocations();
                                    getDriverInfo();
                                    getHasRideEnded();

                                    mRequestBtn.setText("Looking For Driver Location");
                                }
                                else{
                                    requestServiceBol = false;
                                    requestBol = false;

                                    Toast.makeText(CustomerMapActivity.this, "No driver Found for this Service "+requestService, Toast.LENGTH_SHORT).show();
                                    mRequestBtn.setText("Call Uber");
                                    if(pickUpMarker != null){
                                        pickUpMarker.remove();
                                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
                                        GeoFire mGeoFire = new GeoFire(ref);
                                        mGeoFire.removeLocation(FirebaseAuth.getInstance().getUid());
                                    }
                                    return;
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound && requestBol && !requestServiceBol) {
                    radius++;
                    if(driverAvailabe[0]){
                        getClosedDriver();
                    }
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        //        LatLng sydney = new LatLng(-34, 151);
        //        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);        }
        buildGooglApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGooglApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));

        if(!getDriverArroundStarted)
            getDriverArround();
//        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");
//
//        GeoFire mGeoFire = new GeoFire(ref);
//        mGeoFire.setLocation(userID, new GeoLocation(location.getLatitude(),location.getLongitude()));
    }


    @Override
    public void onConnected(@Nullable Bundle bundle ) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);

        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();
//        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");
//
//        GeoFire mGeoFire = new GeoFire(ref);
//        mGeoFire.removeLocation(userID);
    }


    public void getDriverLocations() {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("DriverWorking").child(driverFoundID).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }

                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatlng = new LatLng(locationLat, locationLng);
                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickUpLocation.latitude);
                    loc1.setLongitude(pickUpLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatlng.latitude);
                    loc2.setLongitude(driverLatlng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance < 100) {
                        mRequestBtn.setText("Captain Arrived");

                    } else {
                        mRequestBtn.setText("Driver Found : " + distance);
                    }

                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatlng).title("Your driver here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}

