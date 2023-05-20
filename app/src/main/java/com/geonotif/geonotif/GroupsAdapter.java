package com.geonotif.geonotif;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.ViewHolder> {
    private final ArrayList<Group> groupsList;
    private ItemClickListener listener;
    private Context context;
    //String currentGroup;

    //Creating the constructor
    public GroupsAdapter(ArrayList<Group> groupsList, Context context) {
        this.groupsList = groupsList;
        this.context = context;
    }

    public void setOnItemClickListener(ItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.groups_item, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group group = groupsList.get(position);
        holder.groupname.setText(group.getGroupName());
        if (group.getGroupParticipantsNo() != null) {
            if (group.getGroupParticipantsNo() == 1)
                holder.participants_no.setText(group.getGroupParticipantsNo() + " participant");
            else
                holder.participants_no.setText(group.getGroupParticipantsNo() + " participants");
        }
        holder.itemView.setOnClickListener(view -> {
//            if (group.getGroupParticipantsNo() < 2) {
//                Intent intent = new Intent(context, AddNewMembersPage.class);
//                intent.putExtra("groupName", group.getGroupName());
//                intent.putExtra("groupUUID", group.getUuid());
//                intent.putExtra("groupParticipantsNo", group.getGroupParticipantsNo());
//                intent.putExtra("groupParticipants", group.getGroupParticipants());
//                context.startActivity(intent);
//            } else {
//                Bundle bundle = new Bundle();
//                bundle.putString("groupUUID", group.getUuid());
//                bundle.putString("groupName", group.getGroupName());
//                bundle.putStringArrayList("groupParticipants", group.getGroupParticipants());
//                bundle.putInt("groupParticipantsNo", group.getGroupParticipants().size());
//                GroupTasksFragment grouptasksFragment = new GroupTasksFragment();
//                grouptasksFragment.setArguments(bundle);
//                FragmentTransaction transaction = ((FragmentActivity) context).getSupportFragmentManager().beginTransaction();
//                transaction.replace(R.id.FrameLayout, grouptasksFragment);
//                transaction.addToBackStack(null);
//                transaction.commit();
//            }

            Bundle bundle = new Bundle();
            bundle.putString("groupUUID", group.getUuid());
            bundle.putString("groupName", group.getGroupName());
            bundle.putStringArrayList("groupParticipants", group.getGroupParticipants());
            bundle.putInt("groupParticipantsNo", group.getGroupParticipants().size());
            GroupTasksFragment grouptasksFragment = new GroupTasksFragment();
            grouptasksFragment.setArguments(bundle);
            FragmentTransaction transaction = ((FragmentActivity) context).getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.FrameLayout, grouptasksFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });
    }


    @Override
    public int getItemCount() {
        return groupsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView groupname;
        public TextView participants_no;

        public ViewHolder(@NonNull View itemView, final ItemClickListener listener) {
            super(itemView);

            groupname = itemView.findViewById(R.id.groupName);
            participants_no = itemView.findViewById(R.id.no_of_participants);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getLayoutPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(v, position);
                    }
                }
            });

        }
    }
}
