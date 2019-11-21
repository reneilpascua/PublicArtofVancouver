package io.github.lucahsieh.publicartofvancouver;

import androidx.annotation.NonNull;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MapListActivity extends FragmentActivity implements GoogleMap.OnInfoWindowClickListener, OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean cameraInitialSet = false;
    private LatLng currentLatLng;

    private Marker currentLocMarker;

    private String apiURL;
    private JSONObject apiResponse;
    private JSONArray artRecords;
    private List<ArtListItem> artList;
    private List<ArtListItem> displayList;

    private ListView lv_ArtList_Fragment;
    private int searchMode;
    private int numResults;

    private boolean refreshCurrentLoc=false;

    public static final float DEFAULT_ZOOM=12.3f;
    public static final float SPECIFIC_ZOOM=17.0f;
    public static final double VAN_LAT = 49.2827;
    public static final double VAN_LONG = -123.1207;

    // number of records we know about
    // set as constant because our likes database can't handle an unexpected number
    public static final int N_HITS=636;

    private DatabaseReference likesDB;
    private Map<String, Integer> likesMap;
    private boolean firebaseAlreadyGET = false;

    private String currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_list);

        Intent i = getIntent();
        currentLatLng = new LatLng(i.getDoubleExtra("currentlat",VAN_LAT),
                i.getDoubleExtra("currentlong",VAN_LONG));
        searchMode = i.getIntExtra("searchMode",0);
        numResults = i.getIntExtra("numResults",10);
        currentUser = i.getStringExtra("currentUsername");

        // Obtain SupportMapFragment and get notified when map is ready
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_mapListActivity);
        mapFragment.getMapAsync(this);


        lv_ArtList_Fragment = findViewById(R.id.lv_ArtList_Fragment);
        artList = new ArrayList<ArtListItem>();
        displayList = new ArrayList<ArtListItem>();
        likesMap = new HashMap<>();

        likesDB = FirebaseDatabase.getInstance().getReference("numLikes");

        //getAPIResponse();


        // clicking focuses the map
        lv_ArtList_Fragment.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ArtListItem item = (ArtListItem) parent.getItemAtPosition(position);

                double lat = item.getLatLng().latitude;

                if (lat!=0) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        item.getLatLng(),SPECIFIC_ZOOM));
                } else {
                    Toast.makeText(MapListActivity.this,
                            "No location info found.",
                            Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        // long click brings you to page
        lv_ArtList_Fragment.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArtListItem item = (ArtListItem) parent.getItemAtPosition(position);

                Intent i = new Intent(MapListActivity.this, DetailedArtworkActivity.class);
                i.putExtra("currentUsername",currentUser);
                i.putExtra("recordid", item.getRecordID());
                startActivity(i);

//                return false;
            }
        });



    }

    @Override
    protected void onStart() {
        super.onStart();
        likesDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (firebaseAlreadyGET) return;

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    NumberOfLikes nl = ds.getValue(NumberOfLikes.class);
                    likesMap.put(nl.getRecordID(),nl.getNumLikes());
                }
                firebaseAlreadyGET=true;

                // gets api response, populates list with likes
                getAPIResponse();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        Spinner spinner = findViewById(R.id.spinner_numResults);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE); /* if you want your item to be white */
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCurrentLoc = true;
        firebaseAlreadyGET = false;
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(this);

        try {
            currentLocMarker= mMap.addMarker(new MarkerOptions().position(currentLatLng)
                    .title("Current Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        } catch (Exception e) {
            currentLocMarker= mMap.addMarker(new MarkerOptions().position(new LatLng(VAN_LAT,VAN_LONG))
                    .title("Default Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
//        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,DEFAULT_ZOOM));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(VAN_LAT,VAN_LONG),DEFAULT_ZOOM));


        Intent intent = getIntent();
        if (intent.getIntExtra("Place Number",0) == 0 ) {
            // Zoom into users location
            locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    currentLatLng = new LatLng(location.getLatitude(),location.getLongitude());
                    if (!cameraInitialSet) {
//                        currentLocMarker.setPosition(new LatLng(location.getLatitude(),location.getLongitude()));

//                        LatLng vancouver = new LatLng(VAN_LAT, VAN_LONG);
                        // regardless of current location, center camera to vancouver.
//                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(vancouver,DEFAULT_ZOOM));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,DEFAULT_ZOOM));
                        currentLocMarker.setTitle("Current Location");

                        cameraInitialSet=true;
                    } else if (refreshCurrentLoc) {


                        // create new?
                        currentLocMarker.setPosition(currentLatLng);
                        refreshCurrentLoc=false;
                    }
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {}

                @Override
                public void onProviderEnabled(String s) {}

                @Override
                public void onProviderDisabled(String s) {}
            };

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                currentLocMarker.setPosition(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
                currentLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            } else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                currentLocMarker.setPosition(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
                currentLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            }
        }
    }

    private void addArtMarker(ArtListItem ali, double latitude, double longitude) {
        LatLng latlng = new LatLng(latitude, longitude);

        Marker mark= mMap.addMarker(new MarkerOptions().position(latlng)
                .title(ali.getListIndex()+".) Artwork ID "+ali.getRegistryID()+" at "+ali.getName())
                .snippet("Click for more details."));
        mark.setTag(ali.getRecordID());
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        if (marker.equals(currentLocMarker)) return;

        String recordid = marker.getTag().toString();

        Intent i = new Intent(MapListActivity.this, DetailedArtworkActivity.class);
        i.putExtra("recordid",recordid);
        i.putExtra("currentUsername",currentUser);
        startActivity(i);
    }

    public void goCurrent(View v) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                currentLocMarker.getPosition(),DEFAULT_ZOOM));
    }




    public void getAPIResponse() {
        System.out.println("trying to get API response");
        apiURL = "https://opendata.vancouver.ca/api/records/1.0/search/?dataset=public-art&rows=636&facet=type&facet=status&facet=sitename&facet=siteaddress&facet=primarymaterial&facet=ownership&facet=neighbourhood&facet=artists&facet=photocredits";

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = null;

        try {
            jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiURL, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    System.out.println("the response is: " + response.toString());
                    apiResponse = response;
                    System.out.println("API response success");

                    populateArtList();

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.println("something went wrong with volley: " + error.toString());
                }
            });
        } catch (Exception e) {
            System.out.println("something went wrong with jsonArrayRequest: " + e.toString());
        }

        requestQueue.add(jsonObjectRequest);

    }

    public void populateArtList() {
        mMap.clear();

        System.out.println("trying to populate page");
        JSONObject art1;
        JSONObject fields1;
        String recordid;
        String id1;
        String name1;
        String desc1;
        double lat1;
        double long1;
        String imageURL;


        int nhits = 0;
        try {
            artRecords = apiResponse.getJSONArray("records");
            //System.out.println(artList);
            nhits = Integer.parseInt(apiResponse.get("nhits").toString());
        } catch (Exception e) {
            Log.e("conversion failed: ", e.toString());
        }



        int index=0;
        artList.clear();
        while (
                index<N_HITS
        ) {
            art1=null;
            fields1=null;
            recordid="no record ID found";

            id1 = "no registry ID found";
            name1 = "no site name found";
            desc1 = "no description found";
            lat1 = 0;
            long1 = 0;

            // try to get image

            // try to get description
            try {
                art1 = artRecords.getJSONObject(index);
                recordid = art1.get("recordid").toString();
                fields1 = art1.getJSONObject("fields");
                id1 = fields1.get("registryid").toString();
            } catch (Exception e) {
                Log.e("conversion failed for index "+index+":", e.toString());
            }

            try {
                name1 = fields1.get("sitename").toString();
            } catch (Exception e) {
                Log.e("conversion failed for index "+index+":", e.toString());
            }

            try {
                desc1 = fields1.get("descriptionofwork").toString();
            } catch (Exception e) {
                Log.e("conversion failed for index "+index+":", e.toString());
            }


            // try to get image url
            try {
                String suburl= fields1.getJSONObject("photourl").get("format").toString();
                String suburl2=fields1.getJSONObject("photourl").get("filename").toString();
                imageURL = "https://covapp.vancouver.ca/PublicArtRegistry/ImageDisplay.aspx?"
                        +convertPhotoURL(suburl)
                        +"&ImageType=Thumb"
//                    +"/"+suburl2
                ;
            } catch (Exception e) { imageURL = "no photo url";}


            // try to get location
            try {
                long1 = Double.parseDouble(fields1.getJSONObject("geom").getJSONArray("coordinates").get(0).toString());
                lat1 = Double.parseDouble(fields1.getJSONObject("geom").getJSONArray("coordinates").get(1).toString());
            } catch (Exception e) {
                Log.e("conversion failed for index "+index+":", e.toString());
            }

            int nl = likesMap.get(recordid);

            ArtListItem newALI = new ArtListItem(index+1, recordid, id1, name1, desc1, new LatLng(lat1, long1), nl, imageURL);
            //System.out.println("Desription:"+newALI.getDescription());
//            if (lat1!=0 && long1!=0) {
//                addArtMarker(newALI,lat1,long1);
//            }
            artList.add(newALI);
            index++;
        }

        System.out.println("art list populate success with "+artList.size()+" items");

        displayList.clear();


        if (searchMode==0) { // results by distance
            TreeMap<Double,Integer> distancerank = rankArrayListByDistance(artList);

            int numAdded = 0;
            for (Map.Entry<Double,Integer> entry : distancerank.entrySet()) {
                artList.get(entry.getValue()).setListIndex(numAdded+1);
                displayList.add(artList.get(entry.getValue()));
                numAdded++;
                if (numAdded==numResults) break;
            }
        } else { // results by likes
            Collections.sort(artList);
            int added=0;
            while (added<numResults) {
                artList.get(added).setListIndex(added+1);
                displayList.add(artList.get(added));
                added++;
            }
        }

        System.out.println("display list has this many elements: "+displayList.size());



        currentLocMarker = mMap.addMarker(new MarkerOptions().position(currentLatLng)
                .title("Current Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));


        renderView();


    }


    private void renderView() {

        for (ArtListItem ali : displayList) {
            double lat1 = ali.getLatLng().latitude;
            double long1 = ali.getLatLng().longitude;
            if (lat1!=0 && long1!=0) {
                addArtMarker(ali,lat1,long1);
            }
        }


        ArtListItemAdapter adapter = new ArtListItemAdapter(MapListActivity.this, displayList);
        lv_ArtList_Fragment.setAdapter(adapter);
        ((TextView) findViewById(R.id.searchresultstext)).setText("");
    }
    private String convertPhotoURL(String format) {
        // example format: aspx?AreaId=1&ImageId=1
        String[] formatArray = format.split("&");
        String first = formatArray[0].substring(5);
        String second="&Area=Artwork&";
        String third = formatArray[1];
        return first+second+third;
    }


    public void updateSearch_Distance(View view) {
        Intent i = getIntent();
        i.putExtra("currentlat",currentLatLng.latitude);
        i.putExtra("currentlong",currentLatLng.latitude);
        i.putExtra("searchMode",0);

        Spinner spinner = findViewById(R.id.spinner_numResults);
        String numResults_str = spinner.getSelectedItem().toString();
        int numResults;
        if (numResults_str.equals("All")) {
            numResults = N_HITS;
        } else {
            numResults = Integer.parseInt(numResults_str);
        }
        i.putExtra("numResults",numResults);

        finish();
        startActivity(i);
    }

    public void updateSearch_Likes(View view) {
        Intent i = getIntent();
        i.putExtra("currentlat",currentLatLng.latitude);
        i.putExtra("currentlong",currentLatLng.latitude);
        i.putExtra("searchMode",1);

        Spinner spinner = findViewById(R.id.spinner_numResults);
        String numResults_str = spinner.getSelectedItem().toString();
        int numResults;
        if (numResults_str.equals("All")) {
            numResults = N_HITS;
        } else {
            numResults = Integer.parseInt(numResults_str);
        }
        i.putExtra("numResults",numResults);

        finish();
        startActivity(i);
    }


    // returns map<distance, listindex>, sorted by key (distance)
    private TreeMap<Double, Integer> rankArrayListByDistance(List<ArtListItem> list) {
        TreeMap<Double, Integer> distancerank = new TreeMap<Double,Integer>();

        int index=0;
        for (ArtListItem item:list) {
            distancerank.put(getDistance(item.getLatLng()),index);
            index++;
        }

        return distancerank;
    }

    private double getDistance(LatLng ll) {
        return (
                Math.pow((ll.latitude - currentLatLng.latitude),2)
                + Math.pow((ll.longitude - currentLatLng.longitude),2)
                );
    }

    public void goLiked(View view) {
        Intent i = new Intent(MapListActivity.this, LikedListActivity.class);
        i.putExtra("currentUsername",currentUser);
        startActivity(i);
    }


/**
 * only to be used once in a while, to add records in numLikes database per artwork
 */
//    public void populateDatabase() {
//        for (ArtListItem ali : artList) {
//            String id = ali.getRecordID();
//            NumberOfLikes numLikes = new NumberOfLikes(id,0);
//            Task setValueTask = likesDB.child(id).setValue(numLikes);
//        }
//    }
}
