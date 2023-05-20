package com.geonotif.geonotif;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AddMemberAdapter extends RecyclerView.Adapter<AddMemberAdapter.ViewHolder> {
    private ArrayList<User> memberList;
    private OnButtonClickListener listener;

    public AddMemberAdapter(ArrayList<User> memberList, OnButtonClickListener listener) {
        this.memberList = memberList;
        this.listener = listener;
    }

    public void setList(ArrayList<User> memberList) {
        this.memberList = memberList;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnButtonClickListener listener) {
        this.listener = listener;
    }

    public void setFilteredList(ArrayList<User> filteredList){
        this.memberList = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddMemberAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_listview_item, parent,
                false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = memberList.get(position);
        holder.username.setText(user.getFullname());
        holder.addButton.setText(user.getButtonDetails());
    }

    public interface OnButtonClickListener {
        void onButtonClickChange(int position);
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView username;
        public Button addButton;
        public ViewHolder(@NonNull View itemView, OnButtonClickListener listener) {
            super(itemView);

            username = itemView.findViewById(R.id.name_of_person);
            addButton = itemView.findViewById(R.id.add_button);

            addButton.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onButtonClickChange(position);
                        notifyItemChanged(position);
                    }
                }
            });

        }
    }
}

