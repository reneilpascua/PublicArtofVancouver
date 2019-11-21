package io.github.lucahsieh.publicartofvancouver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    EditText et_username;
    EditText et_password;
    Button btn_login;
    Button btn_signup;

    DatabaseReference userDB;

    List<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_username = findViewById(R.id.et_username);
        et_password = findViewById(R.id.et_password);

        btn_login = findViewById(R.id.btn_login);
        btn_login.setText(R.string.loading);
        btn_signup = findViewById(R.id.btn_addUser);
        btn_signup.setText(R.string.loading);

        userDB = FirebaseDatabase.getInstance().getReference("users");

        users = new ArrayList<User>();


    }

    @Override
    protected void onStart() {
        super.onStart();
        userDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                users.clear();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    users.add(user);
                }

                btn_login.setText(R.string.login);
                btn_signup.setText(R.string.addUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }


    public void onClickLogin(View view) {

        String username = et_username.getText().toString();
        String password = et_password.getText().toString();

        if (verify(username, password)) {
            Intent i = new Intent(MainActivity.this, MapListActivity.class);
            i.putExtra("currentUsername",username);
            startActivity(i);
        } else {
            Toast.makeText(MainActivity.this, "invalid username and password combination", Toast.LENGTH_LONG).show();
        }
    }

    public boolean verify(String username, String password) {
        boolean verified = false;
        for (User user:users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return true;
            }
        }
        return verified;
    }


    public void onClickSignup(View view) {

        showAddDialog();

    }

    private void addUser(String username, String password, List<String> likedArtIds) {
        User user = new User(username,password,likedArtIds);

        Task setValueTask = userDB.child(username).setValue(user);

        setValueTask.addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                Toast.makeText(MainActivity.this,
                        "Successfully signed up",Toast.LENGTH_LONG).show();
            }
        });

        setValueTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this,
                        "Something went wrong.\n" + e.toString(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.add_user_dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText et_newusername = dialogView.findViewById(R.id.et_newusername);
        final EditText et_newpassword = dialogView.findViewById(R.id.et_newpassword);
        final Button btn_addUserToDB = dialogView.findViewById(R.id.btn_addUserToDB);

        dialogBuilder.setTitle("Add New User");

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawableResource(R.color.secondaryColor);


        btn_addUserToDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newusername = et_newusername.getText().toString().trim();
                String newpassword = et_newpassword.getText().toString().trim();
                List<String> newLikedIds = new ArrayList<String>();
                newLikedIds.add("record1");
                newLikedIds.add("record2");

                // check to see if that user exists already
                for (User existingUser:users) {
                    if (newusername.equals(existingUser.getUsername())) {
                        et_newusername.setError("That username already exists. Please choose another.");
                        return;
                    }
                }

                // handle empty fields
                if (TextUtils.isEmpty(newusername)) {
                    et_newusername.setError("Please enter a username to use");
                    return;
                } else if (TextUtils.isEmpty(newpassword)) {
                    et_newpassword.setError("Please enter a password to use");
                    return;
                }

                addUser(newusername,newpassword,newLikedIds);
                Toast.makeText(MainActivity.this,"User account created!",Toast.LENGTH_LONG).show();

                alertDialog.dismiss();
            }
        });
    }




}
