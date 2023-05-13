package edu.northeastern.numadsp23_team20;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.storage.StorageReference;

public class FriendsData implements Parcelable {

    //private String imageUrl;

    private String emailID;
    private String fullName;
    private String userID;
    private String userName;
    private String buttonDetails;

    private StorageReference pathReference;

    //intialize the variables of our link object.
    public FriendsData(String emailID, String fullName, String userID, String userName, StorageReference pathReference) {
        //this.imageUrl = imageURL;
        this.emailID = emailID;
        this.fullName = fullName;
        this.userID = userID;
        this.userName = userName;
        this.buttonDetails = "Add Friend";
        this.pathReference = pathReference;
    }

    protected FriendsData(Parcel in) {
        emailID = in.readString();
        fullName = in.readString();
        userID = in.readString();
        userName = in.readString();
        buttonDetails = in.readString();
        //pathReference = in.readString();
    }

    public static final Creator<FriendsData> CREATOR = new Creator<FriendsData>() {
        @Override
        public FriendsData createFromParcel(Parcel in) {
            return new FriendsData(in);
        }

        @Override
        public FriendsData[] newArray(int size) {
            return new FriendsData[size];
        }
    };

    //getters
    public String getUserName() {
        return userName;
    }

    public String getFullname() {
        return fullName;
    }

    public String getEmailID() {
        return emailID;
    }

    public String getUserID() {
        return userID;
    }

    public String getButtonDetails() {
        return buttonDetails;
    }

    //setters
    public void setButtonDetails(String buttonDetails) {
        this.buttonDetails = buttonDetails;
    }

    public StorageReference getImageUrl() {
        return pathReference;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(emailID);
        parcel.writeString(fullName);
        parcel.writeString(userID);
        parcel.writeString(userName);
        parcel.writeString(buttonDetails);
    }
}
