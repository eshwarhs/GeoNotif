package edu.northeastern.numadsp23_team20;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TaskTypeListAdapter extends RecyclerView.Adapter<TaskTypeListAdapter.ViewHolder> {

    List<Group> groupList;
    List<User> friendList;
    // OnTaskTypeAssigneeItemClickListener onTaskTypeAssigneeItemClickListener;
    OnTaskTypeGroupItemClickListener onTaskTypeGroupItemClickListener;
    OnTaskTypeFriendItemClickListener onTaskTypeFriendItemClickListener;
    int selectedPosition = -1;
    TaskType taskType;

    public TaskTypeListAdapter(TaskType taskType,
                               List<Group> groupList, OnTaskTypeGroupItemClickListener onTaskTypeGroupItemClickListener,
                               List<User> friendList, OnTaskTypeFriendItemClickListener onTaskTypeFriendItemClickListener)
    {
        this.taskType = taskType;
        if (taskType == TaskType.GROUP) {
            this.groupList = groupList;
            this.friendList = new ArrayList<>();
        } else if (taskType == TaskType.FRIEND) {
            this.groupList = new ArrayList<>();
            this.friendList = friendList;
        }
        this.onTaskTypeGroupItemClickListener = onTaskTypeGroupItemClickListener;
        this.onTaskTypeFriendItemClickListener = onTaskTypeFriendItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder
    onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tasktype_recyclerview_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (this.taskType == TaskType.GROUP) {
            holder.assigneeRadioButton.setText(this.groupList.get(position).getGroupName());
            holder.assigneeRadioButton.setChecked(position == selectedPosition);
            holder.assigneeRadioButton.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean b)
                        {
                            if (b) {
                                selectedPosition = holder.getAdapterPosition();
                                onTaskTypeGroupItemClickListener.onClick(groupList.get(position));
                            }
                        }
                    });
        } else if (this.taskType == TaskType.FRIEND) {
            holder.assigneeRadioButton.setText(this.friendList.get(position).getFullname());
            holder.assigneeRadioButton.setChecked(position == selectedPosition);
            holder.assigneeRadioButton.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean b)
                        {
                            if (b) {
                                selectedPosition = holder.getAdapterPosition();
                                onTaskTypeFriendItemClickListener.onClick(friendList.get(position));
                            }
                        }
                    });
        }
    }

    @Override public long getItemId(int position) {
        return position;
    }

    @Override public int getItemViewType(int position) {
        return position;
    }

    @Override public int getItemCount() {
        if (this.taskType == TaskType.GROUP) {
            return groupList.size();
        } else {
            return friendList.size();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RadioButton assigneeRadioButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.assigneeRadioButton = itemView.findViewById(R.id.AssigneeRadioButton);
        }
    }
}
