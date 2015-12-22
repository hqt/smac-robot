package com.smac.order.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.smac.order.config.Pair;
import com.smac.order.R;

import java.util.List;

/**
 * Created by Huynh Quang Thao on 12/18/15.
 */
public class ItemAdapter extends ArrayAdapter<Pair<Integer, String>> {

    List<Pair<Integer,String>> products;

    public ItemAdapter(Context context, List<Pair<Integer,String>> products) {
        super(context, 0, products);
        this.products = products;
    }

    @Override
    public View getView(int position, View convertView,
                        ViewGroup parent) {
        Pair<Integer, String> p = products.get(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.list_item, parent, false);
        }

        TextView contentTextView = (TextView) convertView.findViewById(R.id.content_textview);
        contentTextView.setText(p.second + ":" + p.first);




        return convertView;
    }



}
