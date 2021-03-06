package com.example.koreantime;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.koreantime.DTO.DTO_schecule;
import com.example.koreantime.DTO.DTO_user;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import net.daum.android.map.MapViewEventListener;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Meetingpage extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    DTO_user user_info;
    String[] members;
    ArrayList<String> late_member = new ArrayList<String>();
    RelativeLayout kakaoMap;
    MapView mapView;
    Button arrive;
    Button punish;
    Geocoder geocoder;
    double initLat = 36.6259;
    double initLon = 127.4526;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};
    Messaging temp1 = new Messaging();
    Messaging temp2 = new Messaging();
    Messaging temp3 = new Messaging();
    Messaging temp4 = new Messaging();
    TextView nowAddress;
    MapPOIItem initMarker;
    boolean arriveFlag = false;
    boolean punishFlag = false;
    DTO_schecule meetingclass;
    String tttkk;
    String[] member_id;

    ArrayList<MapPOIItem> mapPOIItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meetingpage);
        TextView date = findViewById(R.id.month);
        TextView time = findViewById(R.id.time);

        nowAddress = findViewById(R.id.nowAddress);
        geocoder = new Geocoder(this);
        kakaoMap = findViewById(R.id.kakaoMap);
        mapView = new MapView(Meetingpage.this);
        kakaoMap.addView(mapView);

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        } else {
            checkRunTimePermission();
        }

        Intent Intent = getIntent();
        String m_id = Intent.getStringExtra("id");
        String g_id = Intent.getStringExtra("gid");
        String email = Intent.getStringExtra("email");
        String[] members_token = Intent.getStringArrayExtra("tokens");
        Log.d("plz", email);
        member_id = Intent.getStringArrayExtra("member_id");
        ArrayList<String> other_id=new ArrayList<String>();
        for ( int i=0; i < member_id.length; i++ ) {
            if(member_id[i]!=email){
                other_id.add(member_id[i]);
            }
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("group").document(g_id).collection("schedule").document(m_id)
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        meetingclass = document.toObject(DTO_schecule.class);
                        date.setText(SplitDate(meetingclass.getDate()));
                        time.setText(meetingclass.getTime());
                        GeoCoding(meetingclass.getLocation());
                        GPSToAddress(initLat, initLon);
                    } else {
                        Log.d("inter meeing", "No such document");
                    }
                } else {
                    Log.d("inter meeing", "get failed with ", task.getException());
                }
            }
        });

        Task<String> token = FirebaseMessaging.getInstance().getToken();

        token.addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if(task.isSuccessful()){
                    Log.d("FCM Token", task.getResult().toString());
                    tttkk=task.getResult().toString();
                }
            }
        });

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        MapPOIItem marker = new MapPOIItem();
        final LocationListener gpsLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                for(String id:other_id){
                    RemoveAllMarkers();
                    db.collection("user").document(id)
                            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Log.d("ssivl", document.getData().get("latitude").toString());

                                    Log.d("ssivl", document.getData().get("longitude").toString());

                                    MakeMarker(Double.parseDouble(document.getData().get("longitude").toString()) , Double.parseDouble(document.getData().get("latitude").toString()) );
                                } else {
                                    Log.d("inter meeing", "No such document");
                                }
                            } else {
                                Log.d("inter meeing", "get failed with ", task.getException());
                            }
                        }
                    });

                }
                mapView.removePOIItem(marker);
                MapPoint MARKER_POINT = MapPoint.mapPointWithGeoCoord(latitude, longitude);
                marker.setItemName("It's Me!!");
                marker.setTag(0);
                marker.setMapPoint(MARKER_POINT);
                marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // ???????????? ???????????? BluePin ?????? ??????.
                marker.setCustomImageResourceId(R.drawable.redpin);
                mapView.addPOIItem(marker);

                if(!arriveFlag){
                    Check10M(latitude, longitude);
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                5000,
                5,
                gpsLocationListener);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                5000,
                5,
                gpsLocationListener);

        arrive = findViewById(R.id.arrive);
        punish = findViewById(R.id.punish);

        arrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (arriveFlag) {
                    punish.setVisibility(View.VISIBLE);
                }else{
                    punish.setVisibility(View.INVISIBLE);
                }
            }
        });

        punish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                while (true) {
//                    punish.setBackgroundColor(Color.parseColor("#FF2C2C"));
//                    try {
//                        Thread.sleep(16000);
//                        break;
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                punish.setBackgroundColor(Color.parseColor("#FFF"));
                try {
                    if (temp1.getStatus() == AsyncTask.Status.RUNNING) {
                        temp1.cancel(true);
                    }
                    if (temp2.getStatus() == AsyncTask.Status.RUNNING) {
                        temp1.cancel(true);
                    }
                    if (temp3.getStatus() == AsyncTask.Status.RUNNING) {
                        temp1.cancel(true);
                    }
                    if (temp4.getStatus() == AsyncTask.Status.RUNNING) {
                        temp1.cancel(true);
                    }
                } catch (Exception e) {
                }
                if(members_token.length>0){
                    if(tttkk!=members_token[0]){
                        temp1.setToken(members_token[0]);
                        temp1.execute();
                    }
                }
                if(members_token.length>1){
                    if(tttkk!=members_token[1]) {
                        temp2.setToken(members_token[1]);
                        temp2.execute();
                    }
                }
                if(members_token.length>2){
                    if(tttkk!=members_token[2]) {
                        temp3.setToken(members_token[2]);
                        temp3.execute();
                    }
                }
                if(members_token.length>3){
                    if(tttkk!=members_token[3]) {
                        temp4.setToken(members_token[3]);
                        temp4.execute();
                    }
                }
            }
        });
    }

    private void MakeMarker(double latitude, double longitude) {
        Log.d("MakeMarker", latitude+"< "+longitude);
        MapPoint MARKER_POINT = MapPoint.mapPointWithGeoCoord(latitude, longitude);
        MapPOIItem marker = new MapPOIItem();
        marker.setItemName("Default Marker");
        marker.setTag(0);
        marker.setMapPoint(MARKER_POINT);
        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // ???????????? ???????????? BluePin ?????? ??????.
        marker.setCustomImageResourceId(R.drawable.bluepin);
        mapView.addPOIItem(marker);
        mapPOIItems.add(marker);
    }

    private void RemoveAllMarkers() {
        for(int i=0;i<mapPOIItems.size();i++){
            mapView.removePOIItem(mapPOIItems.get(i));
        }
    }

    private String SplitDate(String date) {
        String tmpDate = date.substring(5);
        String[] newDate = tmpDate.split("-");
        return GetMonth(newDate[0])+"."+newDate[1];
    }

    public String GetMonth(String num) {
        switch (num) {
            case "01":
                return "JAN";
            case "02":
                return "FEB";
            case "03":
                return "MAR";
            case "04":
                return "APR";
            case "05":
                return "MAY";
            case "06":
                return "JUN";
            case "07":
                return "JUL";
            case "08":
                return "AUG";
            case "09":
                return "SEP";
            case "10":
                return "OCT";
            case "11":
                return "NOV";
            case "12":
                return "DEC";
        }
        return "";
    }

    private void GeoCoding(String location) {
        List<Address> list = null;
        try {
            list = geocoder.getFromLocationName(location, 10);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (list != null) {
            if (list.size() == 0) {
                initLat = 36.6287;
                initLon = 127.4605;
            } else {
                initLat = list.get(0).getLatitude();
                initLon = list.get(0).getLongitude();
            }
        }
        initMarker = new MapPOIItem();
        MapPoint MARKER_POINT = MapPoint.mapPointWithGeoCoord(initLat, initLon);
        initMarker.setItemName("????????? ???");
        initMarker.setTag(0);
        initMarker.setMapPoint(MARKER_POINT);
        initMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // ???????????? ???????????? BluePin ?????? ??????.
        initMarker.setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage); // ????????? ???????????????, ???????????? ???????????? RedPin ?????? ??????.
        initMarker.setCustomImageResourceId(R.drawable.mymarker);// ???????????? ???????????? BluePin ?????? ??????.
        mapView.addPOIItem(initMarker);
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(initLat, initLon), true);
        mapView.setZoomLevel(5, true);
    }

    private String SplitAddress(String addressLine) {
        return addressLine.substring(5);
    }

    private void GPSToAddress(double latitude, double longitude) {
        List<Address> list = null;
        try {
            list = geocoder.getFromLocation(
                    latitude, // ??????
                    longitude, // ??????
                    10); // ????????? ?????? ??????
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("test", "????????? ??????");
        }
        if (list != null) {
            if (list.size() == 0) {
//                marker.setItemName(" ");
            } else {
                nowAddress.setText(SplitAddress(list.get(0).getAddressLine(0)));
            }
        }
    }

    void send_penalty(String token, String vibrate, String alarm) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        kakaoMap.removeAllViews();
    }

    private void Check10M(double latitude, double longitude) {
        double theta = initLon - longitude;
        double dist = Math.sin(deg2rad(latitude)) * Math.sin(deg2rad(initLat)) + Math.cos(deg2rad(latitude)) * Math.cos(deg2rad(initLat)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1609.344;
        if (dist <= 30) {
            arrive.setBackgroundResource(R.drawable.get_img_btn1);
            punish.setVisibility(View.INVISIBLE);
            arriveFlag = true;
        } else {
            Log.d("distance", "?????? ??????");
            arriveFlag = false;
            arrive.setBackgroundResource(R.drawable.get_img_btn);
        }
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }


    private void onFinishReverseGeoCoding(String result) {
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults);
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // ?????? ????????? PERMISSIONS_REQUEST_CODE ??????, ????????? ????????? ???????????? ??????????????????
            boolean check_result = true;

            // ?????? ???????????? ??????????????? ???????????????.
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if (check_result) {
                Log.d("@@@", "start");
                //?????? ?????? ????????? ??? ??????

            } else {
                // ????????? ???????????? ????????? ?????? ????????? ??? ?????? ????????? ??????????????? ?????? ???????????????.2 ?????? ????????? ??????
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
                    Toast.makeText(Meetingpage.this, "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ??????????????????.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(Meetingpage.this, "???????????? ?????????????????????. ??????(??? ??????)?????? ???????????? ???????????? ?????????. ", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    void checkRunTimePermission() {

        //????????? ????????? ??????
        // 1. ?????? ???????????? ????????? ????????? ???????????????.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(Meetingpage.this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 2. ?????? ???????????? ????????? ?????????
            // ( ??????????????? 6.0 ?????? ????????? ????????? ???????????? ???????????? ????????? ?????? ????????? ?????? ???????????????.)
            // 3.  ?????? ?????? ????????? ??? ??????

        } else {  //2. ????????? ????????? ????????? ?????? ????????? ????????? ????????? ???????????????. 2?????? ??????(3-1, 4-1)??? ????????????.
            // 3-1. ???????????? ????????? ????????? ??? ?????? ?????? ????????????
            if (ActivityCompat.shouldShowRequestPermissionRationale(Meetingpage.this, REQUIRED_PERMISSIONS[0])) {
                // 3-2. ????????? ???????????? ?????? ?????????????????? ???????????? ????????? ????????? ???????????? ????????? ????????????.
                Toast.makeText(Meetingpage.this, "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.", Toast.LENGTH_LONG).show();
                // 3-3. ??????????????? ????????? ????????? ?????????. ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(Meetingpage.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            } else {
                // 4-1. ???????????? ????????? ????????? ??? ?????? ?????? ???????????? ????????? ????????? ?????? ?????????.
                // ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(Meetingpage.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    //??????????????? GPS ???????????? ?????? ????????????
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(Meetingpage.this);
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n"
                + "?????? ????????? ?????????????????????????");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GPS_ENABLE_REQUEST_CODE) {//???????????? GPS ?????? ???????????? ??????
            if (checkLocationServicesStatus()) {
                if (checkLocationServicesStatus()) {
                    Log.d("@@@", "onActivityResult : GPS ????????? ?????????");
                    checkRunTimePermission();
                }
            }
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


}