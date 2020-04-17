package com.demo.nearbyfiletransfer;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ExecuterModel implements Parcelable {

    private String codename;
    private String rating;
    private String endpointId;
    private int status;

    //required empty constructor
    public ExecuterModel() {}
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public ExecuterModel(String codename, String rating, String endpointId,int status) {
        this.codename = codename;
        this.rating = rating;
        this.endpointId = endpointId;
        this.status = status;
    }

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(codename);
        parcel.writeString(endpointId);
        parcel.writeString(rating);
        parcel.writeInt(status);
    }

    public ExecuterModel(Parcel in){
        codename = in.readString();
        endpointId = in.readString();
        rating = in.readString();
        status = in.readInt();
    }

    public static final Parcelable.Creator<ExecuterModel> CREATOR = new Creator<ExecuterModel>() {
        @Override
        public ExecuterModel createFromParcel(Parcel parcel) {
            return new ExecuterModel(parcel);
        }

        @Override
        public ExecuterModel[] newArray(int i) {
            return new ExecuterModel[i];
        }
    };

    @NonNull
    @Override
    public String toString() {
         super.toString();
         return this.getCodename();
    }
}
