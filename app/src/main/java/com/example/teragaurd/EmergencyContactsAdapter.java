package com.example.teragaurd;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ViewHolder> {

    private List<EmergencyContact> contacts;
    private OnContactActionListener listener;

    public interface OnContactActionListener {
        void onCallClick(EmergencyContact contact);
        void onDeleteClick(EmergencyContact contact, int position);
    }

    public EmergencyContactsAdapter(List<EmergencyContact> contacts, OnContactActionListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);
        holder.txtContactName.setText(contact.getName());
        holder.txtContactNumber.setText(contact.getPhoneNumber());

        // Show delete button only for user-added contacts
        holder.btnDelete.setVisibility(contact.isEditable() ? View.VISIBLE : View.GONE);

        holder.btnCall.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCallClick(contact);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(contact, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts != null ? contacts.size() : 0;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < contacts.size()) {
            contacts.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void addItem(EmergencyContact contact) {
        contacts.add(contact);
        notifyItemInserted(contacts.size() - 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtContactName, txtContactNumber;
        ImageButton btnCall, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtContactName = itemView.findViewById(R.id.txtContactName);
            txtContactNumber = itemView.findViewById(R.id.txtContactNumber);
            btnCall = itemView.findViewById(R.id.btnCall);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
