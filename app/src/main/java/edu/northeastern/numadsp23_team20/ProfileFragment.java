package edu.northeastern.numadsp23_team20;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ProfileFragment extends Fragment {

    FirebaseUser firebaseUser;
    FirebaseAuth mAuth;

    View view;
    Button logoutButton;
    private EditText fullNameEditText, emailEditText;
    private SwitchMaterial notificationSwitch;
    private TextView gamificationTextView;

    ImageView profileImage;
    FloatingActionButton fab;
    FirebaseStorage storage;
    StorageReference storageRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);
        fab = view.findViewById(R.id.fab_add_photo);
        fab.setOnClickListener(v -> ImagePicker.with(this)
                .galleryOnly()
                .cropSquare()
                .start());
        logoutButton = view.findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            getContext().stopService(new Intent(this.getContext(), LocationService.class));
            Intent intent = new Intent(getActivity(), MainActivity.class);
            getActivity().finish();
            getActivity().stopService(new Intent(this.getContext(), LocationService.class));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            this.startActivity(intent);
        });
        this.notificationSwitch = view.findViewById(R.id.switch_notifications);
        SharedPreferences sharedPreferences = this.getActivity().getSharedPreferences(GeoNotif.PREFERENCES, MODE_PRIVATE);
        String notifSetting = sharedPreferences.getString(GeoNotif.NOTIF_SETTING, GeoNotif.ENABLE_NOTIF_SETTING);
        if (notifSetting.equalsIgnoreCase(GeoNotif.ENABLE_NOTIF_SETTING)) {
            this.notificationSwitch.setChecked(true);
        } else {
            this.notificationSwitch.setChecked(false);
        }
        this.notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = this.getActivity().getSharedPreferences(GeoNotif.PREFERENCES, MODE_PRIVATE).edit();
            if (isChecked) {
                getContext().startService(new Intent(this.getContext(), LocationService.class));
                editor.putString(GeoNotif.NOTIF_SETTING, GeoNotif.ENABLE_NOTIF_SETTING);
            } else {
                getContext().stopService(new Intent(this.getContext(), LocationService.class));
                editor.putString(GeoNotif.NOTIF_SETTING, GeoNotif.DISABLE_NOTIF_SETTING);
            }
            editor.apply();
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        StorageReference pathReference = storageRef.child("profileImages/" + firebaseUser.getUid() + "/profile.jpg");
        profileImage = view.findViewById(R.id.imgProfile);
        fullNameEditText = view.findViewById(R.id.fullnameTextBox);
        emailEditText = view.findViewById(R.id.emailTextBox);
        gamificationTextView = view.findViewById(R.id.GamificationTextView);
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + firebaseUser.getUid());
        mDatabase.get().addOnSuccessListener(dataSnapshot -> {
            User user = dataSnapshot.getValue(User.class);
            emailEditText.setText(firebaseUser.getEmail());
            fullNameEditText.setText(user.getFullname());
            gamificationTextView.setText(gamificationTextView.getText().toString() + user.getAssignableTasks());
        });

        pathReference.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
            if (ProfileFragment.super.getContext() == null) {
            } else {
                Glide.with(ProfileFragment.super.getContext())
                        .load(downloadUrl)
                        .circleCrop()
                        .into(profileImage);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = data.getData();
        profileImage.setImageURI(uri);

        StorageReference profileImages = storageRef.child("profileImages/" + firebaseUser.getUid() + "/profile.jpg");

        profileImage.setDrawingCacheEnabled(true);
        profileImage.buildDrawingCache();
        Bitmap bitmap1 = ((BitmapDrawable) profileImage.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] datax = baos.toByteArray();

        UploadTask uploadTask = profileImages.putBytes(datax);
        uploadTask.addOnFailureListener(exception -> {
            System.out.println("Upload failed");
        }).addOnSuccessListener(taskSnapshot -> System.out.println("Upload success"));
    }
}