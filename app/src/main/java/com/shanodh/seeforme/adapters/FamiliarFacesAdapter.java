package com.shanodh.seeforme.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.shanodh.seeforme.R;
import com.shanodh.seeforme.models.FamiliarFace;
import java.util.List;

public class FamiliarFacesAdapter extends RecyclerView.Adapter<FamiliarFacesAdapter.FaceViewHolder> {
    
    private List<FamiliarFace> faces;
    private Context context;
    private OnFaceClickListener onFaceClickListener;

    public interface OnFaceClickListener {
        void onFaceClick(FamiliarFace face, int position);
        void onFaceLongClick(FamiliarFace face, int position);
    }

    public FamiliarFacesAdapter(Context context, List<FamiliarFace> faces) {
        this.context = context;
        this.faces = faces;
    }

    public void setOnFaceClickListener(OnFaceClickListener listener) {
        this.onFaceClickListener = listener;
    }

    public void updateFaces(List<FamiliarFace> newFaces) {
        this.faces = newFaces;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_familiar_face, parent, false);
        return new FaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaceViewHolder holder, int position) {
        FamiliarFace face = faces.get(position);
        holder.bind(face, position);
    }

    @Override
    public int getItemCount() {
        return faces != null ? faces.size() : 0;
    }

    class FaceViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private ImageView imageView;
        private TextView nameTextView;

        public FaceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardFace);
            imageView = itemView.findViewById(R.id.imageFace);
            nameTextView = itemView.findViewById(R.id.textFaceName);
        }

        public void bind(FamiliarFace face, int position) {
            nameTextView.setText(face.getName());
            
            // Load face image
            if (face.getImagePath() != null) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(face.getImagePath());
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageResource(R.drawable.ic_person);
                    }
                } catch (Exception e) {
                    imageView.setImageResource(R.drawable.ic_person);
                }
            } else {
                imageView.setImageResource(R.drawable.ic_person);
            }

            // Set click listeners
            cardView.setOnClickListener(v -> {
                if (onFaceClickListener != null) {
                    onFaceClickListener.onFaceClick(face, position);
                }
            });

            cardView.setOnLongClickListener(v -> {
                if (onFaceClickListener != null) {
                    onFaceClickListener.onFaceLongClick(face, position);
                }
                return true;
            });

            // Accessibility
            cardView.setContentDescription("Familiar face: " + face.getName() + ". Double tap to view details, long press for options.");
        }
    }
}
