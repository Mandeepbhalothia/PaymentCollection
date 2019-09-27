package com.sonant.paymentcollection.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.sonant.paymentcollection.R;
import com.sonant.paymentcollection.pojo.VoiceData;

import java.util.List;

public class DisplayAdapter extends RecyclerView.Adapter<DisplayAdapter.MyViewHolder> {
    private List<VoiceData> messageList;
    private Context mContext;
    private static ClickListener clickListener;



    public DisplayAdapter(Context mContext, List<VoiceData> messageList) {
        this.messageList = messageList;
        this.mContext = mContext;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.voice_msg, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, final int position) {
        final VoiceData voicedata = messageList.get(position);
        if (voicedata.getType().equals("left")) {
//            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.voice_left.getLayoutParams();
            holder.text_right.setVisibility(View.GONE);
            holder.voice_left.setVisibility(View.VISIBLE);
            {
                holder.voice_left.setText(voicedata.getMSG());
            }
        }
        if (voicedata.getType().equals("right")) {
            holder.voice_left.setVisibility(View.GONE);
            holder.text_right.setVisibility(View.VISIBLE);
            holder.text_right.setText(spanCursor(voicedata.getMSG()));
            holder.text_right.setMaxWidth(300);
            holder.text_right.setHorizontallyScrolling(false);
            holder.text_right.setMaxLines(20);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private TextView voice_left, text_right;

        private MyViewHolder(View view) {
            super(view);

            voice_left = view.findViewById(R.id.vmessage_body);


            text_right = view.findViewById(R.id.message_body);


        }

        @Override
        public void onClick(View v) {

        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        DisplayAdapter.clickListener = clickListener;
    }

    public interface ClickListener {
        void onItemClick(int position, View v);

        void onItemLongClick(int position, View v);
    }

    private SpannableString spanCursor(String msg){
        SpannableString  spannedString = new SpannableString(msg);
        if (msg.endsWith(" |")){
            spannedString.setSpan(new ForegroundColorSpan(Color.BLUE),spannedString.length()-2,
                    spannedString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannedString;
    }
}
