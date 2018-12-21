package com.cqkct.FunKidII.Ui.Adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.db.Entity.ContactEntity;
import com.cqkct.FunKidII.db.Entity.SosEntity;

import java.util.List;

public class SosContactRecyclerAdapter extends RecyclerView.Adapter{

    @NonNull
    final private List<SosEntity> sosContactList;
    @NonNull
    final private List<ContactEntity> contactEntities;

    private boolean hasEditPermission;

    public SosContactRecyclerAdapter(@NonNull List<SosEntity> contactList,
                                     boolean hasEditPermission, @NonNull List<ContactEntity> contactEntityList) {
        this.sosContactList = contactList;
        this.hasEditPermission = hasEditPermission;
        this.contactEntities = contactEntityList;
    }


    public List<SosEntity> getDataList() {
        return sosContactList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_sos_contact, parent, false);
        return new SosViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        SosViewHolder holder = (SosViewHolder) viewHolder;
        SosEntity data = sosContactList.get(position);

        String sosName = data.getName();
        String sosNumber = data.getNumber();
        holder.name.setText(sosName);
        holder.contact.setText(sosNumber);
        holder.progressBar.setVisibility(data.getSynced() ? View.GONE : View.VISIBLE);
//        holder.divider.setVisibility(position >= getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);

//        holder.editIcon.setVisibility(hasEditPermission ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return sosContactList.size();
    }


    private class SosViewHolder extends RecyclerView.ViewHolder {
        View itemView;

        TextView name;
        TextView contact;
        ProgressBar progressBar;
        View divider;

        SosViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;

            name = itemView.findViewById(R.id.name);
            contact = itemView.findViewById(R.id.number);
            progressBar = itemView.findViewById(R.id.sos_pb_syncing);
            divider = itemView.findViewById(R.id.divider);
        }
    }

}
