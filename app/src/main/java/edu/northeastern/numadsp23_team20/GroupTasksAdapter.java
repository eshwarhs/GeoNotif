package edu.northeastern.numadsp23_team20;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GroupTasksAdapter extends RecyclerView.Adapter<GroupTasksAdapter.TaskListViewHolder>
        implements OnTaskItemClickListener {

    private List<Task> grouptaskList;
    private OnTaskItemClickListener onTaskItemClickListener;
    private TaskService taskService;

    private static final int LAYOUT_ONE = 0;
    private static final int LAYOUT_TWO = 1;

    @Override
    public int getItemViewType(int position) {
        Task task = this.grouptaskList.get(position);
        if (task.getIsComplete())
            return LAYOUT_ONE;
        else
            return LAYOUT_TWO;
    }

    public GroupTasksAdapter(List<Task> grouptaskList, OnTaskItemClickListener onTaskItemClickListener) {
        this.grouptaskList = grouptaskList;
        this.onTaskItemClickListener = onTaskItemClickListener;
        this.taskService = new TaskService();

        TaskService.TaskServiceCreateListener taskServiceCreateListener = taskUUID -> {
        };
        this.taskService.setTaskServiceCreateListener(taskServiceCreateListener);
    }

    @NonNull
    @Override
    public TaskListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view;

        if (viewType == LAYOUT_ONE) {
            view = inflater.inflate(R.layout.task_recyclerview_completeditem, parent, false);
        } else {
            view = inflater.inflate(R.layout.task_recyclerview_item, parent, false);
        }
        return new TaskListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskListViewHolder holder, int position) {
        Task task = this.grouptaskList.get(position);
        holder.RVTaskTitle.setText(task.getTaskName());
        holder.RVTaskTypeString.setText(task.getTaskTypeString());
        holder.RVTaskLocation.setText("\uD83D\uDCCD " + task.getLocation().getKey());
        holder.RVCheckBox.setChecked(task.getIsComplete());

        holder.RVCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            this.grouptaskList.get(position).setIsComplete(isChecked);
            notifyItemChanged(position);
            //this.taskService.createTask(task);
        });
    }

    @Override
    public int getItemCount() {
        return this.grouptaskList.size();
    }

    @Override
    public void onTaskItemClick(int position) {
    }

    public class TaskListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView RVTaskTitle;
        private TextView RVTaskLocation;
        private TextView RVTaskTypeString;
        public CheckBox RVCheckBox;

        public TaskListViewHolder(@NonNull View itemView) {
            super(itemView);

            this.RVTaskTitle = itemView.findViewById(R.id.RVTaskTitle);
            this.RVTaskTypeString = itemView.findViewById(R.id.RVTaskType);
            this.RVTaskLocation = itemView.findViewById(R.id.RVTaskLocation);
            this.RVCheckBox = itemView.findViewById(R.id.checkBox);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (onTaskItemClickListener != null) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onTaskItemClickListener.onTaskItemClick(position);
                }
            }
        }
    }
}