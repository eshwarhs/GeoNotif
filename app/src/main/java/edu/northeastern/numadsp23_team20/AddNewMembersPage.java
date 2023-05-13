package edu.northeastern.numadsp23_team20;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

public class AddNewMembersPage extends AppCompatActivity {
    private ImageButton settings;
    private Button addNewMembers;
    private String groupID;
    private String groupName;
    private Integer groupParticipantsNo;
    private ArrayList<String> groupParticipants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_members);
        Intent intent = getIntent();
        this.groupID = intent.getStringExtra("groupUUID");
        this.groupName = intent.getStringExtra("groupName");
        this.groupParticipants = intent.getStringArrayListExtra("groupParticipants");
        this.groupParticipantsNo = intent.getIntExtra("groupParticipantsNo", 1);
        // Set the group name as the text of the TextView
        TextView groupNameTextView = findViewById(R.id.groupName);
        groupNameTextView.setText(this.groupName);
        this.addNewMembers = findViewById(R.id.addmember_bttn);
        this.addNewMembers.setOnClickListener(v -> {
            Intent intent1 = new Intent(getApplicationContext(), AddMembersList.class);
            intent1.putExtra("groupUUID", groupID);
            intent1.putExtra("groupName", groupName);
            intent1.putExtra("groupParticipantsNo", groupParticipantsNo);
            intent1.putExtra("groupParticipants", groupParticipants);
            startActivity(intent1);
        });

        settings = findViewById(R.id.settings_button);
        settings.setOnClickListener(v -> {
            // create a new intent to open the new activity
            Intent intent12 = new Intent(getApplicationContext(), GroupSettingsView.class);
            intent12.putExtra("groupUUID", groupID);
            intent12.putExtra("groupName", groupName);
            intent12.putExtra("groupParticipantsNo", groupParticipantsNo);
            intent12.putExtra("groupParticipants", groupParticipants);
            startActivity(intent12);
        });
    }
}