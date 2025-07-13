package com.shanodh.seeforme.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.shanodh.seeforme.R;

import java.util.List;

public class FacesAdapter extends RecyclerView.Adapter<FacesAdapter.FaceViewHolder> {

    private List<ViewFacesActivity.FaceItem> facesList;
    private OnFaceActionListener listener;

    public interface OnFaceActionListener {
        void onDeleteFace(ViewFacesActivity.FaceItem face, int position);
        void onFaceClick(ViewFacesActivity.FaceItem face);
    }

    public FacesAdapter(List<ViewFacesActivity.FaceItem> facesList, OnFaceActionListener listener) {
        this.facesList = facesList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_face, parent, false);
        return new FaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaceViewHolder holder, int position) {
        ViewFacesActivity.FaceItem face = facesList.get(position);
        
        holder.tvPersonName.setText(face.getName());
        holder.tvRelationship.setText(face.getRelationship());
        holder.tvAdded.setText("Added " + face.getFormattedDate());
        holder.ivFaceImage.setImageBitmap(face.getBitmap());

        // Set click listeners
        holder.cardContainer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFaceClick(face);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteFace(face, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return facesList.size();
    }

    static class FaceViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardContainer;
        ImageView ivFaceImage;
        TextView tvPersonName;
        TextView tvRelationship;
        TextView tvAdded;
        MaterialButton btnDelete;

        public FaceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = (MaterialCardView) itemView;
            ivFaceImage = itemView.findViewById(R.id.ivFaceImage);
            tvPersonName = itemView.findViewById(R.id.tvPersonName);
            tvRelationship = itemView.findViewById(R.id.tvRelationship);
            tvAdded = itemView.findViewById(R.id.tvAdded);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
