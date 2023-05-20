package com.geonotif.geonotif;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class GroupSettingsView extends AppCompatActivity {
    private Button edit;
    private Button leaveBttn;
    private GroupService groupService;
    private Group group;
    private String groupID;
    private String groupName;
    private Integer groupParticipantsNo;
    private ArrayList<String> groupParticipants;
    private ArrayList<String> recyclerViewParticipantList;
    private ParticipantListAdapter participantListAdapter;
    private ActivityResultLauncher<Intent> addMembersActivityLauncher;

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("LeaveGroup", false);
        returnIntent.putExtra("GroupName", groupName);
        returnIntent.putExtra("GroupParticipants", groupParticipants);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_group_settings_old);
        setContentView(R.layout.activity_group_settings_view);
        Intent intent = getIntent();
        this.groupID = intent.getStringExtra("groupUUID");
        this.groupName = intent.getStringExtra("groupName");
        this.groupParticipants = intent.getStringArrayListExtra("groupParticipants");
        this.groupParticipantsNo = intent.getIntExtra("groupParticipantsNo", 1);
        // Set the group name as the text of the TextView
        TextView groupNameTextView = findViewById(R.id.GroupSettingsGroupName);
        groupNameTextView.setText(groupName);
        this.groupService = new GroupService();

        RecyclerView groupParticipantsRecyclerView = findViewById(R.id.GroupSettingsParticipantsRecyclerViewContainer);
        this.recyclerViewParticipantList = new ArrayList<>();
        this.participantListAdapter = new ParticipantListAdapter(this.recyclerViewParticipantList);
        groupParticipantsRecyclerView.setAdapter(participantListAdapter);
        groupParticipantsRecyclerView.setHasFixedSize(true);
        groupParticipantsRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        GroupService groupService = new GroupService();
        GroupService.GroupServiceReadParticipantsListener groupServiceReadParticipantsListener = new GroupService.GroupServiceReadParticipantsListener() {
            @Override
            public void onParticipantRead(String participant) {
                recyclerViewParticipantList.add(participant);
                participantListAdapter.notifyDataSetChanged();
            }
        };
        groupService.setGroupServiceReadParticipantsListener(groupServiceReadParticipantsListener);
        groupService.readParticipantsForGroup(this.groupID);

        // Find the group with the matching name in groupsList
        this.group = new Group(groupName, groupParticipants);
        this.group.setUuid(groupID);
        this.edit = findViewById(R.id.GroupSettingsEditName);
        this.edit.setOnClickListener(v -> editGroupAlertDialog(group));
        //Button addMember = findViewById(R.id.addmember_bttn);
        Button addMember = findViewById(R.id.GroupSettingsAddParticipants);
        this.addMembersActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            recyclerViewParticipantList.clear();
                            groupService.readParticipantsForGroup(groupID);
                        }
                    });
        addMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AddMembersList.class);
                intent.putExtra("groupUUID", groupID);
                intent.putExtra("groupName", groupName);
                intent.putExtra("groupParticipantsNo", groupParticipantsNo);
                intent.putExtra("groupParticipants", groupParticipants);
                startActivity(intent);
            }
        });
        // this.leaveBttn = findViewById(R.id.leave_bttn);
        this.leaveBttn = findViewById(R.id.GroupSettingsLeaveButton);
        this.leaveBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leaveGroupAlertDialog();
            }
        });
    }

    public void editGroupAlertDialog(Group group) {
        View view = getLayoutInflater().inflate(R.layout.editgroupname_dialog, null);
        EditText group_name = view.findViewById(R.id.editgroupname);
        group_name.setText(this.groupName);
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(GroupSettingsView.this);
        alertDialog.setView(view);

        alertDialog.setPositiveButton("DONE", (dialogInterface, i) -> {

        });
        alertDialog.setNegativeButton("CANCEL", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        AlertDialog alert = alertDialog.create();
        alert.show();

        alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view1 -> {
            String newGroupName = group_name.getText().toString();
            groupName = newGroupName;
            TextView groupNameTextView = findViewById(R.id.GroupSettingsGroupName);
            groupNameTextView.setText(newGroupName);

            Group updatedGroup = group;
            updatedGroup.setGroupName(newGroupName);
            this.groupService.editGroupName(updatedGroup.getUuid(), updatedGroup.getGroupName());
            alert.dismiss();
        });
    }

    public void leaveGroupAlertDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("Are you sure you want to leave this group?")
                .setIcon(R.drawable.warning)
                .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        groupService.leaveGroup(groupID);
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("LeaveGroup", true);
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();

    }


}