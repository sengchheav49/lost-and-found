package com.example.lostandfound.adapters;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.R;
import com.example.lostandfound.activities.History_Details;
import com.example.lostandfound.models.Item;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ItemHistoryAdapter extends RecyclerView.Adapter<ItemHistoryAdapter.ItemViewHolder> {
    private static final String DATE_FORMAT = "MMM dd, yyyy";
    private Context context;
    private List<Item> itemList;

    public ItemHistoryAdapter(Context context, List<Item> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_row, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateData(List<Item> newItems) {
        this.itemList = newItems;
        notifyDataSetChanged();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView ivItemImage;
        private TextView tvItemTitle;
        private TextView tvItemCategory;
        private TextView tvItemLocation;
        private TextView tvItemDate;
        private TextView tvItemType;
        private static final String DATE_FORMAT = "MMM dd, yyyy";

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.iv_item_image);
            tvItemTitle = itemView.findViewById(R.id.tv_item_title);
            tvItemCategory = itemView.findViewById(R.id.tv_item_category);
            tvItemLocation = itemView.findViewById(R.id.tv_item_location);
            tvItemDate = itemView.findViewById(R.id.tv_item_date);
            tvItemType = itemView.findViewById(R.id.tv_item_type);

            itemView.setOnClickListener(this);
        }

        public void bind(Item item) {
            tvItemTitle.setText(item.getTitle());
            tvItemCategory.setText(item.getCategory());
            tvItemLocation.setText(item.getLocation());

            // Format date
            if (item.getDate() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
                tvItemDate.setText(dateFormat.format(item.getDate()));
            } else {
                tvItemDate.setText("Unknown Date");
                Log.w(TAG, "Date is null for item: " + item.getTitle());
            }

            // Set item type (LOST or FOUND)
            tvItemType.setText(item.getItemType().toUpperCase());

            // Set type background color
            int backgroundColorRes;
            if (item.getItemType().equalsIgnoreCase("Not Returned")) {
                backgroundColorRes = R.color.error; // Red color for Not Returned
            } else if (item.getItemType().equalsIgnoreCase("Returned")) {
                backgroundColorRes = R.color.success; // Green color for Returned
            } else {
                backgroundColorRes = R.color.error; // Default color if item type is not recognized
            }
            tvItemType.setBackgroundResource(backgroundColorRes);

            // Load image with Picasso and log the URL
            // Clear any existing image
            ivItemImage.setImageDrawable(null);

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Log.d("Loading image", "Loading image: " + item.getImageUrl());

                Glide.with(context)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.circle_background)
                        .error(R.drawable.circle_background)
                        .centerCrop()
                        .into(ivItemImage);
                
                Log.d("TAG", "Started loading image: " + item.getImageUrl());
            } else {
                Log.w("TAG", "Image URL is null or empty for item: " + item.getTitle());
                ivItemImage.setImageResource(R.drawable.circle_background);
            }

            // Add relative time indication
            if (item.getTimestamp() > 0) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        item.getTimestamp(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS);
                Log.d(TAG, "Relative time for " + item.getTitle() + ": " + relativeTime);
            }
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                Item item = itemList.get(position);
                Intent intent = new Intent(context, History_Details.class);

                intent.putExtra("itemId", item.getItemId());
                intent.putExtra("title", item.getTitle());
                intent.putExtra("category", item.getCategory());
                intent.putExtra("location", item.getLocation());
                intent.putExtra("date", item.getDate());
                if (item.getReturnDate() != null) {
                    intent.putExtra("returnDate", item.getReturnDate().getTime());
                } else {
                    intent.putExtra("returnDate", -1L);  // Use -1L to specify a long value
                }


                intent.putExtra("itemType", item.getItemType());
                intent.putExtra("imageUrl", item.getImageUrl());
                intent.putExtra("description", item.getDescription());
                intent.putExtra("contactInfo", item.getUserPhoneNumber());
                intent.putExtra("contactInfo", item.getUserPhoneNumber());


                context.startActivity(intent);
            }
        }

    }
}