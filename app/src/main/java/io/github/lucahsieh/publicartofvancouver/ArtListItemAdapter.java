package io.github.lucahsieh.publicartofvancouver;

import android.app.Activity;
import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;

import com.squareup.picasso.Picasso;

import java.util.List;

public class ArtListItemAdapter extends ArrayAdapter<ArtListItem> {
    private Activity context;
    private List<ArtListItem> artList;

    public ArtListItemAdapter(Activity context, List<ArtListItem> artList) {
        super(context, R.layout.simple_artlist_item, artList);
        this.context = context;
        this.artList = artList;
    }

    public ArtListItemAdapter(Context context, int resource, List<ArtListItem> objects, Activity context1, List<ArtListItem> artList) {
        super(context, resource, objects);
        this.context = context1;
        this.artList = artList;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();

        View listViewItem = inflater.inflate(R.layout.simple_artlist_item, null, true);

        TextView tv_ListItemName = listViewItem.findViewById(R.id.tv_ListItemName);
        TextView tv_ListItemDesc = listViewItem.findViewById(R.id.tv_ListItemDesc);
        TextView tv_ListItemNumLikes = listViewItem.findViewById(R.id.tv_ListItemNumLikes);
        ImageView iv_ListItemImage = listViewItem.findViewById(R.id.iv_ListItemImage);



        ArtListItem artListItem = artList.get(position);
        tv_ListItemName.setText("Artwork ID "+artListItem.getRegistryID());
        tv_ListItemDesc.setText(artListItem.getDescription());
        if (artListItem.getNumLikes()!=0) {
            tv_ListItemNumLikes.setText(""+artListItem.getNumLikes());
        }
        Picasso.with(context).load(artListItem.getImageURL()).placeholder(R.drawable.placeholder).into(iv_ListItemImage);

        return listViewItem;
    }

    public ArtListItem getItem(int position) {
        return artList.get(position);
    }

}
