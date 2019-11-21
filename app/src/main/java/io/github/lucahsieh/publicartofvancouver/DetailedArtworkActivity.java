package io.github.lucahsieh.publicartofvancouver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.text.style.TtsSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

public class DetailedArtworkActivity extends AppCompatActivity {

    String recordid;
    String apiURL;
    JSONObject apiResponse;
    JSONArray artRecords;

    String currentUsername;
    User currentUser;

    TextView tv_artworkID;
    TextView tv_type;
    TextView tv_status;
    TextView tv_yearOfInstallation;

    ImageView iv_detailedPic;

    TextView tv_numLikes;
    Button btn_like;

    TextView tv_URL;
    TextView tv_site;
    TextView tv_neighbourhood;
    TextView tv_address;
    TextView tv_description;
    TextView tv_artistprojectstatement;


    DatabaseReference likesDB;
    DatabaseReference userDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_artwork);

        Intent i = getIntent();
        recordid = i.getStringExtra("recordid");
        System.out.println("Displaying details for "+recordid);
        currentUsername=i.getStringExtra("currentUsername");
        System.out.println("User: "+currentUsername);

        findViewsByID();
        tv_artworkID.setText("loading...");


        getAPIResponse();

        likesDB = FirebaseDatabase.getInstance().getReference("numLikes");
        userDB = FirebaseDatabase.getInstance().getReference("users");

    }

    @Override
    protected void onStart() {
        super.onStart();
        likesDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int numlikes=-1;
                try {
                    NumberOfLikes nl = dataSnapshot.child(recordid).getValue(NumberOfLikes.class);
                    numlikes = nl.getNumLikes();
                } catch (Exception e) {
                    System.out.println("big error !!!!!!!!!!!!!!!");
                    e.printStackTrace();
                }

                tv_numLikes.setText(""+numlikes);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        userDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                System.out.println("on data change "+currentUsername);
                currentUser = dataSnapshot.child(currentUsername).getValue(User.class);
                if (currentUser.alreadyLikes(recordid)) {
                    btn_like.setText("UNLIKE");
                    btn_like.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_favorite_black_24dp, 0, 0, 0);

                } else {
                    btn_like.setText("LIKE");
                    btn_like.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_favorite_border_black_24dp, 0, 0, 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }


    private void findViewsByID() {
        tv_artworkID = findViewById(R.id.tv_artworkID);
        tv_type = findViewById(R.id.tv_type);
        tv_status = findViewById(R.id.tv_status);
        tv_yearOfInstallation = findViewById(R.id.tv_yearOfInstallation);
        // image
        btn_like = findViewById(R.id.btn_like);
        tv_numLikes = findViewById(R.id.tv_numLikes);
        tv_URL = findViewById(R.id.tv_URL);
        tv_URL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tv_URL.getText().toString()));
//                startActivity(browserIntent);

                String url = tv_URL.getText().toString().substring(5);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        iv_detailedPic = findViewById(R.id.iv_detailedPic);
        tv_site = findViewById(R.id.tv_site);
        tv_neighbourhood = findViewById(R.id.tv_neighbourhood);
        tv_address = findViewById(R.id.tv_address);
        tv_description = findViewById(R.id.tv_description);
        tv_artistprojectstatement = findViewById(R.id.tv_artistprojectstatement);
    }

    private void getAPIResponse() {
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
                    populateDetails();
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
        System.out.println("API response success");
    }

    private void populateDetails() {
        try {
            artRecords = apiResponse.getJSONArray("records");
            //System.out.println(artList);
        } catch (Exception e) {
            Log.e("conversion failed: ", e.toString());
            return;
        }
        int index = findArtRecord();

        String artworkid="";
        String type="";
        String status="";
        String yearofinstallation="";
        String photoURL="";
        String url="";
        String site="";
        String neighbourhood="";
        String address="";
        String description="none found";
        String artistproject="none found";

        JSONObject thisArt=null;
        JSONObject fields=null;
        try {
            thisArt = artRecords.getJSONObject(index);
            fields = thisArt.getJSONObject("fields");
        } catch (Exception e) {}

        try {
            artworkid = fields.get("registryid").toString();
            type = fields.get("type").toString();
            status = fields.get("status").toString();
            yearofinstallation = fields.get("yearofinstallation").toString();

            // url = fields.get("url").toString();



        } catch (Exception e ) {}

        try {
            site = fields.get("sitename").toString() +", "
                    + fields.get("locationonsite").toString();
        } catch(Exception e) {}

        try{
            neighbourhood = fields.get("neighbourhood").toString();
        } catch (Exception e) {}

        try{
            address = fields.get("siteaddress").toString();
        } catch (Exception e) {}

        try {
            url=fields.get("url").toString();
        } catch (Exception e) {}

        try {
            String suburl= fields.getJSONObject("photourl").get("format").toString();
            String suburl2=fields.getJSONObject("photourl").get("filename").toString();
            photoURL = "https://covapp.vancouver.ca/PublicArtRegistry/ImageDisplay.aspx?"
                    +convertPhotoURL(suburl)
                    +"&ImageType=Large"
//                    +"/"+suburl2
            ;
        } catch (Exception e) { photoURL = "no photo url";}

        try {
            description = fields.get("descriptionofwork").toString();
        } catch (Exception e) {}

        try {
            artistproject = fields.get("artistprojectstatement").toString();
        } catch (Exception e) {}

        setTexts(artworkid, type, status,yearofinstallation,photoURL,site,neighbourhood,address,description,artistproject,url) ;
    }

    private String convertPhotoURL(String format) {
        // example format: aspx?AreaId=1&ImageId=1
        String[] formatArray = format.split("&");
        String first = formatArray[0].substring(5);
        String second="&Area=Artwork&";
        String third = formatArray[1];
        return first+second+third;
    }

    private int findArtRecord() {
        boolean found = false;
        int index=0;
        while (!found) {
            String candidateRecord="";
            try {
                candidateRecord = artRecords.getJSONObject(index).get("recordid").toString();
            } catch(Exception e) { }
            if (candidateRecord.equals(recordid)) {break;}
            index++;
        }
        return index;
    }

    private void setTexts(String artworkid, String type, String status, String yearofinstallation, String photoURL, String site, String neighbourhood, String address, String description, String artistproject, String link) {
        tv_artworkID.setText("Artwork ID #"+artworkid);
        tv_type.setText(type.toUpperCase());
        tv_status.setText(status.toUpperCase());
        tv_yearOfInstallation.setText(yearofinstallation.toUpperCase());
        tv_URL.setText("URL: "+link);

        Picasso.with(this).load(photoURL).placeholder(R.drawable.placeholder).into(iv_detailedPic);

        tv_site.setText(site);
        tv_neighbourhood.setText(neighbourhood);
        tv_address.setText(address);
        tv_description.setText(description);
        tv_artistprojectstatement.setText(artistproject);
    }

    public void likeThisArt(View view) {

        DatabaseReference dbRef = likesDB.child(recordid);

        NumberOfLikes nl;
        final String likeResponse;
        if (currentUser.alreadyLikes(recordid)) {
            nl = new NumberOfLikes(recordid,-1+Integer.parseInt(tv_numLikes.getText().toString()));
            likeResponse = "UNLIKE!";
            btn_like.setText("LIKE");
            btn_like.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_favorite_border_black_24dp, 0, 0, 0);
            currentUser.removeLikeFromStr(recordid);
        } else {
            nl = new NumberOfLikes(recordid,1+Integer.parseInt(tv_numLikes.getText().toString()));
            likeResponse = "LIKED!";
            btn_like.setText("UNLIKE");
            btn_like.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_favorite_black_24dp, 0, 0, 0);
            currentUser.addLike(recordid);
        }


        Task setValueTask = dbRef.setValue(nl);

        setValueTask.addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {

                Toast.makeText(DetailedArtworkActivity.this,
                        likeResponse,Toast.LENGTH_SHORT).show();

                // if like successful
                updateUserLike();
            }
        });

        setValueTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(DetailedArtworkActivity.this,
                        "Something went wrong.\n" + e.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    private void updateUserLike() {
        DatabaseReference dbref = userDB.child(currentUsername);

        Task setValueTask = dbref.setValue(currentUser);
    }
}
