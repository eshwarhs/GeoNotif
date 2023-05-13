package edu.northeastern.numadsp23_team20;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;

import java.util.List;
import java.util.UUID;

public class FriendsRecyclerView extends RecyclerView.Adapter<FriendsRecyclerView.ViewHolder> {

    private List<FriendsData> mData; // replace String with your data type

    private OnButtonClickListener mListener;


    // Constructor
    public FriendsRecyclerView(List<FriendsData> data, OnButtonClickListener listener) {
        mData = data;
        mListener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public FriendsRecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout_friends, parent, false);
        ViewHolder viewHolder = new ViewHolder(view, mListener);
        return viewHolder;
    }


    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // get element from your dataset at this position
        // replace String with your data type
        FriendsData data = mData.get(position);
        // set the data to the view holder's views
        holder.userName.setText(data.getFullname());
        holder.button.setText(data.getButtonDetails());


        data.getImageUrl().getDownloadUrl().addOnSuccessListener(downloadUrl -> {
            if (holder.itemView.getContext() == null) {
            } else {
                Glide.with(holder.itemView.getContext())
                        .load(downloadUrl)
                        .circleCrop()
                        .signature(new ObjectKey(data.getUserID()))
                        .into(holder.photoImageView);

            }
        });


        //Picasso.get().load(data.getImageUrl()).into(holder.photoImageView);


    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mData.size();
    }


    public interface OnButtonClickListener {
        void onButtonClickChange(int position);
    }

    // Provide a reference to the views for each data item
    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView photoImageView;
        public TextView userName;

        public Button button;

        public ViewHolder(View view, OnButtonClickListener listener) {
            super(view);

            photoImageView = itemView.findViewById(R.id.photo_of_user);
            userName = view.findViewById(R.id.name_of_person);
            button = view.findViewById(R.id.follow_button);

            button.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onButtonClickChange(position);
                    notifyItemChanged(position); // Notify the adapter that the data has changed
                }
            });
        }


    }

}