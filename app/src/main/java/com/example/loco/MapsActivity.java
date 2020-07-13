package com.example.loco;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


import java.security.acl.NotOwnerException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private PendingIntent pendingIntent;
    private final int REQUEST_LOCATION_PERMISSION= 1;
    private boolean runningQorLater =  Build.VERSION.SDK_INT >=   Build.VERSION_CODES.Q;
    private boolean activityTrackingEnabled = true;
    private TransitionReceiver receiver;
    private List<ActivityTransition> transitionList = new ArrayList<>();
    private LocationManager nManager;
    private final String TRANSITIONS_RECEIVER_ACTION= BuildConfig.APPLICATION_ID+"TRANSITIONS_RECEIVER_ACTION";
    ActivityTransitionRequest activityTransitionRequest;
    //private LocationManager nManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        GoogleApiClient mGApiClient = new GoogleApiClient.Builder(MapsActivity.this)
                .addApi(ActivityRecognition.API)
                .build();
        mGApiClient.connect();
        checkAppPermission();
        mapFragment.getMapAsync(this);
        loadActivities();
        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        pendingIntent = PendingIntent.getBroadcast(MapsActivity.this,0,intent,0);
        receiver = new TransitionReceiver();
        registerReceiver(receiver,new IntentFilter(TRANSITIONS_RECEIVER_ACTION));

        //loadActivities();


    }

//    @Override
//    protected void onResume() {
//         loadActivities();
//        super.onResume();
//
//
//    }

    private void checkAppPermission(){
        if(runningQorLater){
            if( PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,Manifest.permission.ACTIVITY_RECOGNITION))
          {
           finish();
            }
        }

    }
    private void loadActivities(){

        transitionList.add(new ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build());
        transitionList.add(new ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build());

        transitionList.add(new ActivityTransition.Builder().setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build());
        transitionList.add(new ActivityTransition.Builder().setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build());

        transitionList.add(new ActivityTransition.Builder().setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build());
        transitionList.add(new ActivityTransition.Builder().setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build());

        transitionList.add(new ActivityTransition.Builder().setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build());
        transitionList.add(new ActivityTransition.Builder().setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build());

        transitionList.add(new ActivityTransition.Builder().setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build());
        transitionList.add(new ActivityTransition.Builder().setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build());

        activityTransitionRequest = new ActivityTransitionRequest(transitionList);

        Task<Void> task = ActivityRecognition.getClient(this).requestActivityTransitionUpdates(activityTransitionRequest,pendingIntent);
        task.addOnSuccessListener(
                new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o) {
                        activityTrackingEnabled = true;
                        Log.d(TAG, "onSuccess: "+"SUCCESSFULL");
                    }
                }
        );
                task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: "+"FAILED");
                    }
                }
        );

    }

    private void onRemoveActivity(){
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(pendingIntent).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                activityTrackingEnabled = false;
                Log.d(TAG, "onSuccess: "+"Transition Successfully Unregistered");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: "+"The Transition could not be UNregistered");
                Log.e(TAG, "onFailure: The Transition could not be UNregistered",e );
            }
        });
    }

    @Override
    protected void onStop() {

        onRemoveActivity();
        super.onStop();
        //super.onStop();
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
        enableMyLocation();
        LatLng burgerPark = new LatLng(49.889847, 8.666955);
        mMap.addMarker(new MarkerOptions().position(burgerPark).title("Marker in BurgerPark"));

        LatLng college = new LatLng( 49.873952, 8.658833);
        mMap.addMarker(new MarkerOptions().position(college).title("Marker in College"));
        LatLng zoo = new LatLng( 49.866050, 8.684217);
        mMap.addMarker(new MarkerOptions().position(zoo).title("Marker in Zoo"));

        //googleMap.addMarker(new MarkerOptions().position(latLng).title(" ").snippet(snippet).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));)

        setMapLongClick(googleMap);
        setPoiClick(googleMap);

        //callNotification();
    }
    private void createNotificationChannel(String channelName, String channelDescription) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //CharSequence name = "NEAR COLLEGE";
            //String description = "STUDENT NEAR COLLEGE";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(TRANSITIONS_RECEIVER_ACTION, channelName, importance);
            channel.setDescription(channelDescription);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void callNotification(String title, String context,int notificationId){
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent1 = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,TRANSITIONS_RECEIVER_ACTION) .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(context)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("")).setContentIntent(pendingIntent1).setNumber(notificationId)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

// notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, builder.build());


    }
    private void setMapLongClick(final GoogleMap map){
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(LatLng latLng) {
                String snippet = String.format(Locale.getDefault(),
                        "Lat: %1$.5f, Long: %2$.5f",
                        latLng.latitude,
                        latLng.longitude);
                map.addMarker(new MarkerOptions().position(latLng).title(" ").snippet(snippet).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            }
        });
    }
    private void setPoiClick(final GoogleMap map){
        map.setOnPoiClickListener(new GoogleMap.OnPoiClickListener() {
            @Override
            public void onPoiClick(PointOfInterest pointOfInterest) {
                Marker poiMarker = map.addMarker(new MarkerOptions().position(pointOfInterest.latLng).title(pointOfInterest.name));
                poiMarker.showInfoWindow();
            }
        });
    }
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(
                MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            Location locationGPS = nManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (locationGPS != null) {
                double lat = locationGPS.getLatitude();
                double longi = locationGPS.getLongitude();

                Log.d(TAG, "getLocation: "+"Your Location: " + "\n" + "Latitude: " + lat + "\n" + "Longitude: " + longi);
//                if(lat >= 49.86 && lat<=49.876&& longi>= 8.653 && longi<=8.658){
//                    createNotificationChannel("NEAR COLLEGE","STUDENT NEAR COLLEGE");
//                    callNotification("Location Near College","Please turn your phone to silent mode",1);
//                }
//                if(lat >= 49.86 && lat<=49.869&& longi>= 8.68&& longi<=8.689){
//                    createNotificationChannel("NEAR ZOO","STUDENT NEAR ZOO");
//                    callNotification("Location Near ZOO","Turn phone to airplane and watch animals and click pictures",1);
//                }
//               if((lat>= 49.885 && lat<=49.895) &&(longi>= 8.66 && longi<=8.668)){
//                   createNotificationChannel("NEAR BURGERPARK","STUDENT NEAR BURGERPARK");
//                   callNotification("Location Near BURGERPARK","Put music on .You are in park GO for a walk",1);
//               }
                if(lat >= 37 && longi<= -122){
                    createNotificationChannel("NEAR COLLEGE","STUDENT NEAR COLLEGE");
                    callNotification("Location Near College","Please turn your phone to silent mode",1);
                }

            } else {
                Toast.makeText(this, "Unable to find location.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void enableMyLocation(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){

            mMap.setMyLocationEnabled(true);
            nManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            getLocation();

            Log.d(TAG, "enableMyLocation: "+"currentLocation");
        }
        else{

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_LOCATION_PERMISSION:
                if(grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    enableMyLocation();
                    break;
                }
        }
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    public class TransitionReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: "+"INTENT ACTION"+intent.getAction());
            if(!TextUtils.equals(intent.getAction(),TRANSITIONS_RECEIVER_ACTION)){
                Log.d(TAG, "onReceive: "+"INTENT ACTION"+intent.getAction());
                return;
            }

            if(ActivityTransitionResult.hasResult(intent)){
                ActivityTransitionResult activityResult = ActivityTransitionResult.extractResult(intent);
                for(ActivityTransitionEvent event:activityResult.getTransitionEvents()){
                    Log.d(TAG, "onReceive: "+"INSIDE");
                    String transition = "Transition: "+ event.getActivityType()+ " transition Type:"+ event.getTransitionType()+
                            new SimpleDateFormat("HH:mm:ss",Locale.US).format(new Date());
                    Log.d(TAG, "loadActivities: "+transition);
                }
            }
        }
    }
}
