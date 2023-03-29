package com.learnoset.chatapplication;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.learnoset.chatapplication.messages.MessagesAdapter;
import com.learnoset.chatapplication.messages.MessagesList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    // creating database reference
    private DatabaseReference databaseReference;

    // array list to store user's
    private final List<MessagesList> userMessagesList = new ArrayList<>();

    // user messages adapter
    private MessagesAdapter messagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView messagesRecyclerView = findViewById(R.id.messagesRecyclerView);

        // get user's mobile number from memory
        final String mobileNumber = MemoryData.getMobile(this);

        // getting database reference
        databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl(getString(R.string.database_url));

        // configure recyclerview
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // set adapter to recyclerview
        messagesAdapter = new MessagesAdapter(userMessagesList, MainActivity.this);
        messagesRecyclerView.setAdapter(messagesAdapter);

        // show progress bar while chat is being fetched from the database
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        // getting user's chat details from firebase realtime database
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // clear old messages details from the list
                userMessagesList.clear();

                // getting all users available in the firebase realtime database
                for (DataSnapshot dataSnapshot : snapshot.child("users").getChildren()) {

                    // getting user's mobile number
                    final String getMobile = dataSnapshot.getKey();

                    // checking whether mobile number is exists in the database.
                    if (getMobile != null) {

                        // don't fetch logged in user details as we don't want to logged in user to show in the user's list
                        if (!getMobile.equals(mobileNumber)) {

                            // getting user's full name
                            final String getUserFullName = dataSnapshot.child("name").getValue(String.class);

                            // getting last message from the chat
                            final String[] lastMessage = {""};
                            final int[] unseenMessagesCount = {0};
                            final String[] chatKey = {""}; // chat key is a unique key for 2 persons who had chat before. Ex. User1 and User2 might have 123 as chat key or User3 and User4 might have 456 as chat key

                            // checking if chat exists for logged in user and this user in the database
                            if (snapshot.child("chat").hasChild(mobileNumber + getMobile)) {
                                chatKey[0] = mobileNumber + getMobile;
                            } else if (snapshot.child("chat").hasChild(getMobile + mobileNumber)) {
                                chatKey[0] = getMobile + mobileNumber;
                            }

                            // chat key empty means users do not have chat yet
                            if (!chatKey[0].isEmpty()) {

                                // getting last seen message timestamps from memory
                                final long userLastSeenMessage = Long.parseLong(MemoryData.getLastMsgTS(MainActivity.this, chatKey[0]));

                                // getting chat messages from chat key
                                snapshot.child("chat").child(chatKey[0]).getRef().addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        // getting users chat / messages
                                        for (DataSnapshot messages : snapshot.getChildren()) {

                                            // getting message details
                                            final String getMessageTimeStamps = messages.getKey();

                                            if (messages.hasChild("mobile") && messages.hasChild("msg")) {
                                                final String getUserMobile = messages.child("mobile").getValue(String.class);
                                                final String getMsg = messages.child("msg").getValue(String.class);

                                                // getting unseen messages from the chat
                                                if (Long.parseLong(Objects.requireNonNull(getMessageTimeStamps)) > userLastSeenMessage) {
                                                    if (!Objects.requireNonNull(getUserMobile).equals(mobileNumber)) {
                                                        unseenMessagesCount[0]++;
                                                    }
                                                }

                                                // storing message
                                                lastMessage[0] = getMsg;
                                            }
                                        }

                                        // load chat/messages in the list
                                        loadData(chatKey[0], getUserFullName, getMobile, lastMessage[0], unseenMessagesCount[0]);

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                        // load chat/messages in the list
                                        loadData(chatKey[0], getUserFullName, getMobile, lastMessage[0], unseenMessagesCount[0]);
                                    }
                                });
                            } else {

                                // load chat/messages in the list
                                loadData(chatKey[0], getUserFullName, getMobile, lastMessage[0], unseenMessagesCount[0]);
                            }
                        }
                    }
                }

                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        });

    }

    private void loadData(String chatKey, String fullName, String mobile, String lastMessage, int unseenMessagesCount) {

        // check if message already exists in the list. This is to prevent duplicate messages / chats/ users
        if (!mobileAlreadyExists(mobile)) {
            final MessagesList messagesList = new MessagesList(chatKey, fullName, mobile, lastMessage, unseenMessagesCount);
            userMessagesList.add(messagesList);
            messagesAdapter.updateMessages(userMessagesList);
        }
    }

    private boolean mobileAlreadyExists(String mobile) {
        for (int i = 0; i < userMessagesList.size(); i++) {
            if (userMessagesList.get(i).getMobile().equals(mobile)) {
                return true;
            }
        }
        return false;
    }
}