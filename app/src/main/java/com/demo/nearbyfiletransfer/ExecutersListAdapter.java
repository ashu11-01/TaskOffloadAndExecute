package com.demo.nearbyfiletransfer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.nearbyfiletransfer.Utility.Constants;

import java.util.List;

public class ExecutersListAdapter extends RecyclerView.Adapter<ExecutersListAdapter.ViewHolder> {

    List<ExecuterModel> executerList;
    Context context;
    ItemClicked activity;
    public interface ItemClicked{
        void onItemClicked(int position);
    }
    public ExecutersListAdapter(List<ExecuterModel> executerList, Context context) {
        this.executerList = executerList;
        this.context = context;
        activity = (ItemClicked)context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.discovered_executers_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.setTag(executerList.get(position));

        holder.tvIndex.setText(position+1+"");
        ExecuterModel executer = executerList.get(position);
        holder.tvName.setText(executer.getCodename());
        holder.tvRating.setText(executer.getRating());
        holder.tvBattery.setText(executer.getBattery()+" %");
        holder.tvRam.setText(executer.getRAM()+" %");
        holder.tvCpu.setText(executer.getCpu()+" MHz");
        holder.tvStorage.setText(executer.getStorage()+" KB");
        holder.tvService.setText(executer.getServiceType());
        switch (executerList.get(position).getStatus()){
            case Constants.ConnectionStatus.NEUTRAL:
                holder.ivConnStatus.setImageDrawable(context.getDrawable(R.drawable.neutral_conn_status));
                holder.tvConnStatus.setText("Neutral");
                break;

            case Constants.ConnectionStatus.CONNECTED:
                holder.ivConnStatus.setImageDrawable(context.getDrawable(R.drawable.connected_conn_status));
                holder.tvConnStatus.setText("CONNECTED");
                break;

            case Constants.ConnectionStatus.REQUESTED:
                holder.ivConnStatus.setImageDrawable(context.getDrawable(R.drawable.requested_conn_status));
                holder.tvConnStatus.setText("REQUESTED");
                break;

            case Constants.ConnectionStatus.REJECTED:
                holder.ivConnStatus.setImageDrawable(context.getDrawable(R.drawable.rejected_conn_status));
                holder.tvConnStatus.setText("REJECTED");
                break;
        }
    }

    @Override
    public int getItemCount() {
        return executerList==null? 0 : executerList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView tvIndex, tvName, tvRating,tvConnStatus,tvBattery,tvRam,tvCpu,tvStorage,tvService;
        ImageView ivConnStatus;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tv_discovered_item_index);
            tvName=itemView.findViewById(R.id.tv_discovered_item_codename);
            tvRating = itemView.findViewById(R.id.tv_discovered_item_rating1);
            ivConnStatus = itemView.findViewById(R.id.iv_off_conn_status);
            tvConnStatus = itemView.findViewById(R.id.tv_conn_status);
            tvBattery = itemView.findViewById(R.id.tv_battery);
            tvRam = itemView.findViewById(R.id.tv_ram);
            tvCpu = itemView.findViewById(R.id.tv_cpu);
            tvStorage = itemView.findViewById(R.id.tv_storage);
            tvService = itemView.findViewById(R.id.tv_service_avl);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.onItemClicked(executerList.indexOf((ExecuterModel)view.getTag()));
                }
            });
        }
    }
}
