package edu.northeastern.numadsp23_team20;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class CreateaGroupDialog extends AppCompatDialogFragment {

    AlertDialog.Builder builder;
    private EditText groupNameEditText;
    private Button saveButton, cancelButton;

    public interface CreateaGroupDialogListener {
        void onGroupAdded(String groupName);
    }

    public CreateaGroupDialog() {
        // Required empty public constructor
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState){
        this.builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.activity_createa_group_dialog, null);
        groupNameEditText = view.findViewById(R.id.editgroupname);
        saveButton = view.findViewById(R.id.donebttn);
        cancelButton = view.findViewById(R.id.cancelbttn);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupName = groupNameEditText.getText().toString();
                CreateaGroupDialogListener listener = (CreateaGroupDialogListener) getActivity();
                listener.onGroupAdded(groupName);
                dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        this.builder.setView(view);
        return builder.create();
    }
}