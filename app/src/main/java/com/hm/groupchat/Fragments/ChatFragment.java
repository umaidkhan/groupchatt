package com.hm.groupchat.Fragments;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hm.groupchat.Models.Author;
import com.hm.groupchat.Models.Message;
import com.hm.groupchat.R;
import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.commons.models.MessageContentType;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.util.ArrayList;
import java.util.Date;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {

    private MessagesListAdapter<Message> adapter;

    private MessageInput messageInput;
    private MessagesList messagesList;

    private DatabaseReference messagesReference;

    private Author currentUser;

    private ImageButton attachmentButton;

    private StorageReference mStorageRef;
    private static  final int GALLARY = 100;
    private Uri uri;

    private ImageView messageImage;

    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        mStorageRef = FirebaseStorage.getInstance().getReference();
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        attachmentButton = (ImageButton)view.findViewById(R.id.attachmentButton);
        messageInput = (MessageInput) view.findViewById(R.id.input);
        messagesList = (MessagesList) view.findViewById(R.id.messagesList);

        messageImage = (ImageView)view.findViewById(R.id.imageMessage);
        onAttachmentButtonClicked();
        ImageLoader imageLoader = new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, @Nullable String url, @Nullable Object payload) {

                Picasso.get().load(url).into(imageView);
            }
        };

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUser = new Author(firebaseUser.getUid(), firebaseUser.getDisplayName());

        adapter = new MessagesListAdapter<>(currentUser.getId(), imageLoader);

        messagesList.setAdapter(adapter);

        messageInput.setInputListener(new MessageInput.InputListener() {
            @Override
            public boolean onSubmit(CharSequence input) {

                String key = messagesReference.push().getKey();

                Message message = new Message(key, input.toString(), currentUser, new Date());

                adapter.addToStart(message, true);

                sendMessage(message);

                return true;
            }
        });

        messagesReference = FirebaseDatabase.getInstance().getReference("messages");

        loadMessages();

        return view;
    }

    private void onAttachmentButtonClicked() {
        attachmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

                intent.setType("image/*");
                startActivityForResult(intent,GALLARY);
            }
        });
    }

    private void loadMessages() {

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Log.v("ChatFragment", dataSnapshot.toString());

                ArrayList<Message> messages = new ArrayList<>();

                for(DataSnapshot messageSnapshot: dataSnapshot.getChildren()) {
                    Long timeStamp = null;
                    if(messageSnapshot.child("createdAt").getValue() !=null) {
                        timeStamp = (Long) messageSnapshot.child("createdAt").getValue();
                    }
                    Message message = new Message();
                    if(messageSnapshot.child("id").getValue()!= null) {
                        message.setId(messageSnapshot.child("id").getValue().toString());
                    }

                    if(messageSnapshot.child("text").getValue() !=null) {
                        message.setText(messageSnapshot.child("text").getValue().toString());
                    }



                    message.setAuthor(new Author());


                    if(timeStamp !=null) {
                        message.setCreatedAt(new Date(timeStamp));
                    }else {
                        message.setCreatedAt(new Date());
                    }
                    if(messageSnapshot.child("image").getValue() !=null) {
                        String image = (String)messageSnapshot.child("image").getValue();
                        if(image !=null) {

                            message.setImageUrl(image);
                        }
                    }

                    messages.add(message);
                }

                adapter.addToEnd(messages, true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                Log.v("ChatFragment", databaseError.toString());
            }
        };

        messagesReference.addValueEventListener(listener);
    }

    private void sendMessage(Message message) {

        messagesReference.child(message.getId()).setValue(message.toMap());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLARY && resultCode == RESULT_OK) {
            uri = data.getData();
            if(uri !=null) {
                StorageReference filePath = mStorageRef.child(uri.getLastPathSegment());
                filePath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        final Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        Toast.makeText(getContext(),"Image uploaded",Toast.LENGTH_SHORT).show();

                        final DatabaseReference newMessage = messagesReference.push();
                        newMessage.child("image").setValue(downloadUrl.toString());
                        newMessage.child("id").setValue(newMessage.getKey());
                        newMessage.child("createdAt").setValue(new Date().getTime());
                        newMessage.child("author").setValue(new Author());



                    }
                });
            }


        }
    }
}
