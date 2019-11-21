package io.github.lucahsieh.publicartofvancouver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
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

public class LikedListActivity extends AppCompatActivity {

    /*
    idea:

    onDataChange() triggers firebase response
    firebase-> set current user
    get the record IDs of the liked art from the user

        getAPIResponse() to populate artList
            use list of liked art to populate displayList

            link listview with displayList using adapter
     */

    private String currentUsername;
    private User currentUser;
    private Map<String, ArtListItem> artMap;
    private List<ArtListItem> displayList;

    private TextView tv_likedListHeading;
    private ListView lv_likedList;

    private String apiURL;
    private JSONObject apiResponse;
    private JSONArray artRecords;

    // number of records we know about
    // set as constant because our likes database can't handle an unexpected number
    public static final int N_HITS=636;

    private DatabaseReference userDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liked_list);

        Intent i = getIntent();
        currentUsername = i.getStringExtra("currentUsername");

        artMap = new HashMap<String,ArtListItem>();
        displayList = new ArrayList<ArtListItem>();

        lv_likedList = findViewById(R.id.lv_likedList);
        tv_likedListHeading = findViewById(R.id.tv_likedListHeading);

        userDB = FirebaseDatabase.getInstance().getReference("users");

        // long click brings you to page
        lv_likedList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArtListItem item = (ArtListItem) parent.getItemAtPosition(position);

                Intent i = new Intent(LikedListActivity.this, DetailedArtworkActivity.class);
                i.putExtra("currentUsername",currentUsername);
                i.putExtra("recordid", item.getRecordID());
                startActivity(i);

//                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        userDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                currentUser = dataSnapshot.child(currentUsername).getValue(User.class);

                getAPIResponse();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
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
            nhits = Integer.parseInt(apiResponse.get("nhits").toString());
        } catch (Exception e) {
            Log.e("conversion failed: ", e.toString());
        }



        int index=0;
        artMap.clear();
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
                name1 = fields1.get("sitename").toString();
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

            ArtListItem newALI = new ArtListItem(index+1, recordid, id1, name1, desc1, new LatLng(lat1, long1),
                    0,imageURL); // dont care about how many likes
            artMap.put(newALI.getRecordID(),newALI);
            index++;
        }

        System.out.println("art list populate success with "+artMap.size()+" items");

        displayList.clear();

        List<String> likedArtIds = currentUser.getLikedArtIds();
        for (String artId:likedArtIds) {
            if (artMap.containsKey(artId)) {
                displayList.add(artMap.get(artId));
            }
        }

        System.out.println("display list has this many elements: "+displayList.size());
        renderView();
    }

    private void renderView() {

        ArtListItemAdapter adapter = new ArtListItemAdapter(LikedListActivity.this, displayList);
        lv_likedList.setAdapter(adapter);
        tv_likedListHeading.setText(currentUser.getUsername()+"'s");
    }
    private String convertPhotoURL(String format) {
        // example format: aspx?AreaId=1&ImageId=1
        String[] formatArray = format.split("&");
        String first = formatArray[0].substring(5);
        String second="&Area=Artwork&";
        String third = formatArray[1];
        return first+second+third;
    }
}
