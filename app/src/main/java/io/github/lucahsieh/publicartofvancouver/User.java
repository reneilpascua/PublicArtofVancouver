package io.github.lucahsieh.publicartofvancouver;

import java.util.ArrayList;
import java.util.List;

public class User {


    private String username;
    private String password;
    private List<String> likedArtIds;

//    // in the format of "record1,record4,record69," for storing in firebase
//    private String likedArtIdsStr;

    public User() {
    }

    public User(String username, String password, List<String> likedArtIds) {

        this.username = username;
        this.password = password;
//        this.likedArtIdsStr = likedArtIdsStr;
//        likedArtIds = new ArrayList<String>();
        this.likedArtIds=likedArtIds;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public List<String> getLikedArtIds() {
        return likedArtIds;
    }

    public void setLikedArtIds(List<String> likedArtIds) {

        this.likedArtIds = likedArtIds;
    }

//    public String getLikedArtIdsStr() {
//        return likedArtIdsStr;
//    }
//
//    public void setLikedArtIdsStr(String likedArtIdsStr) {
//        this.likedArtIdsStr = likedArtIdsStr;
//    }




//    public void convert_likedArtIdsStr_toList() {
//        String[] likedArtIDs_array = likedArtIdsStr.split(",");
//
//        likedArtIds.clear();
//        for (String artid:likedArtIDs_array) {
//            likedArtIds.add(artid);
//        }
//    }

//    public String convert_list_toStr() {
//        String likedArtIdsStr="";
//        for (String entry:likedArtIds) {
//            likedArtIdsStr+=entry+",";
//        }
//        return likedArtIdsStr;
//    }

    public void addLike(String newrecord) {
        if (!alreadyLikes(newrecord)) {
            // add to both the string and set
            likedArtIds.add(newrecord);
//            likedArtIdsStr+=newrecord+",";
        }
    }

    public void removeLikeFromStr(String unlikedRecord) {
        // remove like from list
        likedArtIds.remove(unlikedRecord);

        // update string
//        likedArtIdsStr = convert_list_toStr();
    }

    public boolean alreadyLikes(String record) {
        return (likedArtIds.contains(record));
    }

}
