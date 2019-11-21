package io.github.lucahsieh.publicartofvancouver;

import com.google.android.gms.maps.model.LatLng;

public class ArtListItem implements Comparable<ArtListItem> {



    private int listIndex;
    private String recordID;
    private String registryID;
    private String name;
    private String description;
    private LatLng latLng;
    private int numLikes;

    private String imageURL;

    public ArtListItem(int listIndex, String recordID, String registryID, String name, String description, LatLng latLng, int numLikes, String imageURL) {
        this.listIndex = listIndex;
        this.recordID = recordID;
        this.registryID = registryID;
        this.name = name;
        this.description = description;
        this.latLng = latLng;
        this.numLikes = numLikes;
        this.imageURL=imageURL;
    }

    public String getRegistryID() {
        return registryID;
    }

    public void setRegistryID(String registryID) {
        this.registryID = registryID;
    }

    public String getRecordID() {
        return recordID;
    }

    public void setRecordID(String recordID) {
        this.recordID = recordID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public int getNumLikes() {
        return numLikes;
    }

    public void setNumLikes(int numLikes) {
        this.numLikes = numLikes;
    }

    public int getListIndex() {
        return listIndex;
    }

    public void setListIndex(int listIndex) {
        this.listIndex = listIndex;
    }
    public String getImageURL() {
        return imageURL;
    }
    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    @Override
    public int compareTo(ArtListItem o) {
        // those with more likes should appear first
        return (o.getNumLikes() - this.getNumLikes());
    }
}
