package com.learnoset.chatapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {

    // creating database reference
    private DatabaseReference databaseReference;

    // progress bar
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText mobileET = findViewById(R.id.l_mobile);
        final EditText passwordET = findViewById(R.id.l_password);
        final AppCompatButton loginNowBtn = findViewById(R.id.l_LoginBtn);
        final TextView registerNowTV = findViewById(R.id.l_registerNowBtn);

        // getting database reference
        databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl(getString(R.string.database_url));

        // check if user already logged in. If getMobile() return empty then user is not logged in
        // If already logged in then open MainActivity else user needs to register first
        if (!MemoryData.getMobile(this).isEmpty()) {

            // open MainActivity
            startActivity(new Intent(Login.this, MainActivity.class));

            // finish this(Login) activity
            finish();
        }

        // Create a progress bar
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading...");

        loginNowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // getting user's entered data from EditText
                final String mobileTxt = mobileET.getText().toString();
                final String passwordTxt = passwordET.getText().toString();

                // checking if user's entered mobile number and password is whether empty or not
                if (mobileTxt.isEmpty() || passwordTxt.isEmpty()) {
                    Toast.makeText(Login.this, "Please enter mobile or password", Toast.LENGTH_SHORT).show();
                } else {

                    // login user
                    loginUser(mobileTxt, passwordTxt);
                }
            }
        });

        registerNowTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this, Register.class)); // open Register activity
                finish(); // finish this(Login) activity
            }
        });
    }

    private void loginUser(String mobileNumber, String userEnteredPassword) {

        // sending data to firebase
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // hide progress bar
                progressDialog.dismiss();

                // checking if user's mobile number exists in the database or not
                if (!snapshot.child("users").hasChild(mobileNumber)) {
                    Toast.makeText(Login.this, "Mobile number is not exists in the database", Toast.LENGTH_SHORT).show();
                } else {

                    // getting user's saved password from database
                    final String password = snapshot.child("users").child(mobileNumber).child("password").getValue(String.class);

                    if (password != null) {

                        // checking if user's entered password matches with the password stored in the database
                        if (password.equals(userEnteredPassword)) {

                            Toast.makeText(Login.this, "Login successfully", Toast.LENGTH_SHORT).show();

                            // saving user's mobile number to memory so when the next time user will open the application then he will not have to login again
                            MemoryData.saveMobile(mobileNumber, Login.this);

                            // open MainActivity
                            startActivity(new Intent(Login.this, MainActivity.class));

                            // finish this(Login) activity
                            finish();
                        }
                    } else {
                        Toast.makeText(Login.this, "Unable to get user's password from the database", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(Login.this, "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}