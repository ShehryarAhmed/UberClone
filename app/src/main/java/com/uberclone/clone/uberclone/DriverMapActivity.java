package com.uberclone.clone.uberclone;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.LocationCallback;
//import com.firebase.geofire.LocationCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
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

public class DriverMapActivity extends FragmentActivity
        implements OnMapReadyCallback, RoutingListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button mLogoutBtn;
    private Button mSettingsBtn;
    private Button mHistoryBtn;
    private Button mRideStatusBtn;
    private float rideDistance;
    private String customerID = "", destination;

    private int status =  0;
    private LatLng destinationLatlng,pickupLatLng ;

    private FusedLocationProviderClient mFusedLocationClient;

    private boolean  isLoggingOut = false;
    private SupportMapFragment mapFragment;
    private LinearLayout mCustomerInfo;
    private ImageView mCustomerProfileImage;
    private TextView customerDestination;
    private TextView customerName;
    private TextView customerPhone;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    private Switch mWorkingSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        polylines = new ArrayList<>();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);




        mLogoutBtn = (Button) findViewById(R.id.logout);
        mSettingsBtn = (Button) findViewById(R.id.setting);
        mRideStatusBtn = (Button) findViewById(R.id.rideStatus);
        mHistoryBtn = (Button) findViewById(R.id.history);
        mCustomerInfo = (LinearLayout) findViewById(R.id.customerInfo);
        mCustomerProfileImage = (ImageView) findViewById(R.id.customerPorfileImage);
        customerDestination= (TextView) findViewById(R.id.custmerDestination);
        customerName= (TextView) findViewById(R.id.customerName);
        customerPhone = (TextView) findViewById(R.id.customerPhone);

        mWorkingSwitch = (Switch) findViewById(R.id.working_switch);

        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    connectedDriver();
                }else{
                    disconnectedDriver();
                }
            }
        });
        mRideStatusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status){
                    case 1:
                        status = 2;
                        erasePloyLines();

                        if (destinationLatlng.latitude != 0.0 && destinationLatlng.longitude != 0.0){
                            getRouteToMarker(destinationLatlng);
                        }
                        mRideStatusBtn.setText("drive Completed");
                        break;

                    case 2:
                        recordRide();
                        endRide();
                        break;
                }
            }
        });


        mLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isLoggingOut = true;

                disconnectedDriver();
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(DriverMapActivity.this, MainActivity.class));
                finish();
                return;
            }
        });

        mSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DriverMapActivity.this, DriverSettingActivity.class));
                finish();
            }
        });

        mHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DriverMapActivity.this,HistoryActivity.class);
                intent.putExtra("customerOrDriver","Drivers");
                startActivity(intent);
                return;
            }
        });


        getAssignedCustomer();
    }

    private void getAssignedCustomer() {
        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignCustomerRef = FirebaseDatabase.getInstance().getReference("Users").child("Drivers").child(driverID).child("CustomerRequest").child("CustomerRideID");
        assignCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                        status = 1;
                        customerID = dataSnapshot.getValue().toString();
                        getAssignCustomerPickupLocation();
                        getAssignCustomerInfo();
                        getAssignCustomerDestination();
                }
                else{
                     endRide();
//                    customerID = "";
//                    erasePloyLines();
//                    if(pickUpLocationMarker != null){
//                        pickUpLocationMarker.remove();
//                    }
//                    if(assignCustomerPickUpLocationRefListner != null){
//                        assignCustomerPickUpLocationRef.removeEventListener(assignCustomerPickUpLocationRefListner);
//                    }
//                    mCustomerInfo.setVisibility(View.GONE);
//                    mCustomerProfileImage.setImageResource(R.drawable.ic_launcher_background);
//                    customerName.setText("");
//                    customerPhone.setText("");
//                    customerDestination.setText("Destination: --");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    Marker pickUpLocationMarker;
    private DatabaseReference assignCustomerPickUpLocationRef;
    private ValueEventListener assignCustomerPickUpLocationRefListner;

    private void getAssignCustomerPickupLocation() {
        assignCustomerPickUpLocationRef = FirebaseDatabase.getInstance().getReference().child("CustomerRequest").child(customerID).child("l");
        assignCustomerPickUpLocationRefListner = assignCustomerPickUpLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerID.equals("")) {

                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }

                     if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    pickupLatLng  = new LatLng(locationLat, locationLng);
                    pickUpLocationMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pick up Location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));
                    getRouteToMarker(pickupLatLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getRouteToMarker(LatLng pickupLatLng) {
        if (pickupLatLng != null && mLastLocation != null){
            Routing routing = new Routing.Builder()

                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)

                    .build();
            routing.execute();
        }

    }
    private String getDirectionsUrl(LatLng origin,LatLng dest){
        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;
        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;
        // Sensor enabled
        String sensor = "sensor=false";
        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters+ "&key=AIzaSyBtZK9vReQL1gmLXPSJElx9Yabl6wjSa48" ;
        return url;
    }


    private  void endRide(){
        mRideStatusBtn.setText("Picked Customer");
        erasePloyLines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("CustomerRequest");
        driverRef.removeValue();

        DatabaseReference Ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
        GeoFire geoFire = new GeoFire(Ref);
        geoFire.removeLocation(customerID);

        customerID ="";
        rideDistance = 0;

        if(pickUpLocationMarker != null){
            pickUpLocationMarker.remove();
        }

        if(assignCustomerPickUpLocationRefListner != null){
            assignCustomerPickUpLocationRef.removeEventListener(assignCustomerPickUpLocationRefListner);
        }

        mCustomerInfo.setVisibility(View.GONE);
        mCustomerProfileImage.setImageResource(R.drawable.ic_launcher_background);
        customerName.setText("");
        customerPhone.setText("");
        customerDestination.setText("Destination: --");


        }

        private void recordRide(){
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("History");
            DatabaseReference custmerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customres").child(customerID).child("History");
            DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("History");

            String requestId = historyRef.push().getKey();
            driverRef.child(requestId).setValue(true);
            custmerRef.child(requestId).setValue(true);

            HashMap map = new HashMap();
            map.put("driver",userId);
            map.put("customer",customerID);
            map.put("rating",0);
            map.put("timeStamp",getCurrentTime());
            map.put("destination",destination);
            map.put("location/from/lat",pickupLatLng.latitude);
            map.put("location/from/lng",pickupLatLng.longitude);
            map.put("location/to/lat",destinationLatlng.latitude);
            map.put("location/to/lng",destinationLatlng.longitude);
            map.put("distance",rideDistance);
            map.put("timeStamp",getCurrentTime());


            historyRef.child(requestId).updateChildren(map);
        }

    private Long getCurrentTime() {
            return  System.currentTimeMillis()/1000;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED){

            }else{
                checkLocationPermission();
            }
        }
    }

    LocationCallback mLccationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location: locationResult.getLocations()){
                if (getApplicationContext() != null) {

                    if(!customerID.matches("")){
                        rideDistance += mLastLocation.distanceTo(location)/1000;
                    }
                    mLastLocation = location;
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriverAvailable");
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("DriverWorking");

                    GeoFire geoFireAvailable = new GeoFire(refAvailable);
                    GeoFire geoFireWorking = new GeoFire(refWorking);

                    switch (customerID){
                        case "":
                            Log.d("TAG", "onLocationChanged: "+customerID);
                            geoFireWorking.removeLocation(userID);
                            geoFireAvailable.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;
                        default:
                            Log.d("default", "onLocationChanged: "+customerID);

                            geoFireAvailable.removeLocation(userID);
                            geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;

                    }

                }
            }
        }
    };


    private void checkLocationPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("Give Permission")
                        .setMessage("Give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(DriverMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);

                            }
                        }).create().show();
            }else{
                ActivityCompat.requestPermissions(DriverMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);

            }
        }
    }

    private void connectedDriver(){
     checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLccationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if(FirebaseAuth.getInstance().getCurrentUser().getUid() != null && isLoggingOut){
//            connectedDriver();
        }
    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        if(!isLoggingOut){
//            isLoggingOut = true;
//            disconnectedDriver();
//        }
//    }

    private void disconnectedDriver(){

        if(mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLccationCallback);
        }
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");

        if(ref != null){
            GeoFire mGeoFire = new GeoFire(ref);
            mGeoFire.removeLocation(userID);
        }

    }

//    private void connectedDriver(){
//
////        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
//        }
//        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
//
//        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");
//
//        GeoFire mGeoFire = new GeoFire(ref);
////        geoFireAvailable.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
//        mGeoFire.setLocation(userID, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
//
//    }
//

    private void getAssignCustomerInfo(){
        mCustomerInfo.setVisibility(View.VISIBLE);

        DatabaseReference mcustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customres").child(customerID);

        mcustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0 ){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if(map.get("name") != null){
                        final String mUserName = map.get("name").toString();
                        customerName.setText(mUserName);
                    }
                    if(map.get("phone") != null){
                        final String mPhone = map.get("phone").toString();
                        customerPhone.setText(mPhone);

                    }
                    if(map.get("profileImageUrl") != null){
                        final String mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplicationContext()).load(mProfileImageUrl).into(mCustomerProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignCustomerDestination(){
//        mCustomerInfo.setVisibility(View.VISIBLE);

        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mcustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverID).child("CustomerRequest");

        mcustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() ) {

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if(map.get("destination")!= null){
                        destination = map.get("destination").toString();
                        customerDestination.setText("Destination: "+destination);
                    }
                    else{
                        customerDestination.setText("Destination : --");
                    }

                    Double destinationlat = 0.0;
                    Double destinationlng = 0.0;

                    if(map.get("destinationLat")!= null){
                        destinationlat = Double.valueOf(map.get("destinationLat").toString());
                    }

                    if(map.get("destinationLng")!= null){
                        destinationlng = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatlng =     new LatLng(destinationlat, destinationlng);

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:{
                if(grantResults.length > 0 && grantResults[0]==  PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLccationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else{
                    Toast.makeText(this, "Pleased Grant Permission", Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d("TAG IF", "onRoutingFailure: "+e.getMessage());
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
            Log.d("TAG ELSE", "onRoutingFailure: "+e);

        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouterIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void erasePloyLines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
}
