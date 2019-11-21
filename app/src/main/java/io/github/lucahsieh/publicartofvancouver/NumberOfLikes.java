package io.github.lucahsieh.publicartofvancouver;

public class NumberOfLikes {


    private String recordID;
    private int numLikes;

    public NumberOfLikes(){}

    public NumberOfLikes(String recordID, int numLikes) {
        this.recordID=recordID;
        this.numLikes=numLikes;
    }



    public int getNumLikes() {
        return numLikes;
    }
    public void setNumLikes(int numLikes) {
        this.numLikes = numLikes;
    }


    public String getRecordID() {
        return recordID;
    }

    public void setRecordID(String recordID) {
        this.recordID = recordID;
    }

}
