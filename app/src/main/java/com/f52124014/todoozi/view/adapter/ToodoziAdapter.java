package com.f52124014.todoozi.view.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.f52124014.todoozi.R;
import com.f52124014.todoozi.databinding.ItemRowToodoziBinding;
import com.f52124014.todoozi.model.NoteTaskModel;
import com.f52124014.todoozi.util.DateUtil;

/**
 * Adapter RecyclerView untuk catatan & tugas (Modul 2).
 * Memakai ListAdapter + DiffUtil sehingga perubahan data tampil dengan animasi.
 */
public class ToodoziAdapter extends ListAdapter<NoteTaskModel, ToodoziAdapter.ToodoziViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(NoteTaskModel item);
    }

    public interface OnTaskCheckedChangeListener {
        void onCheckedChanged(NoteTaskModel item, boolean isChecked);
    }

    private static final DiffUtil.ItemCallback<NoteTaskModel> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<NoteTaskModel>() {
                @Override
                public boolean areItemsTheSame(@NonNull NoteTaskModel oldItem, @NonNull NoteTaskModel newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull NoteTaskModel oldItem, @NonNull NoteTaskModel newItem) {
                    return oldItem.isSameContent(newItem);
                }
            };

    private OnItemClickListener itemClickListener;
    private OnTaskCheckedChangeListener checkedChangeListener;

    public ToodoziAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnTaskCheckedChangeListener(OnTaskCheckedChangeListener listener) {
        this.checkedChangeListener = listener;
    }

    public NoteTaskModel getItemAt(int position) {
        if (position < 0 || position >= getItemCount()) return null;
        return getItem(position);
    }

    @NonNull
    @Override
    public ToodoziViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRowToodoziBinding binding =
                ItemRowToodoziBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ToodoziViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ToodoziViewHolder holder, int position) {
        final NoteTaskModel item = getItem(position);
        final ItemRowToodoziBinding b = holder.binding;
        final Context context = holder.itemView.getContext();
        final boolean isNote = item.isNote();

        b.tvTitle.setText(item.getTitle());

        String content = item.getContent();
        if (content == null || content.trim().isEmpty()) {
            b.tvContent.setVisibility(View.GONE);
        } else {
            b.tvContent.setVisibility(View.VISIBLE);
            b.tvContent.setText(content);
        }

        // Tag kategori
        b.tvCategoryTag.setText(isNote ? R.string.tag_note : R.string.tag_task);
        b.tvCategoryTag.setBackgroundResource(isNote ? R.drawable.bg_tag_note : R.drawable.bg_tag_task);
        b.tvCategoryTag.setTextColor(ContextCompat.getColor(context,
                isNote ? R.color.tag_note_text : R.color.tag_task_text));

        // Hindari listener lama terpanggil saat status checkbox diatur ulang
        b.cbTaskDone.setOnCheckedChangeListener(null);

        if (isNote) {
            b.cbTaskDone.setVisibility(View.GONE);
            b.tvDueDate.setVisibility(View.GONE);
            b.tvTitle.setPaintFlags(b.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            b.getRoot().setAlpha(1f);
        } else {
            b.cbTaskDone.setVisibility(View.VISIBLE);
            b.cbTaskDone.setChecked(item.isCompleted());
            bindDueDate(b, item, context);

            if (item.isCompleted()) {
                b.tvTitle.setPaintFlags(b.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                b.getRoot().setAlpha(0.6f);
            } else {
                b.tvTitle.setPaintFlags(b.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                b.getRoot().setAlpha(1f);
            }

            b.cbTaskDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (checkedChangeListener != null) checkedChangeListener.onCheckedChanged(item, isChecked);
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) itemClickListener.onItemClick(item);
        });
    }

    private void bindDueDate(ItemRowToodoziBinding b, NoteTaskModel item, Context context) {
        b.tvDueDate.setVisibility(View.VISIBLE);
        String due = item.getDueDate();
        boolean overdue = !item.isCompleted() && DateUtil.isOverdue(due);

        String text;
        if (DateUtil.isEmpty(due)) {
            text = context.getString(R.string.due_none);
        } else if (overdue) {
            text = context.getString(R.string.due_overdue, DateUtil.formatDisplay(due));
        } else {
            text = context.getString(R.string.due_format, DateUtil.formatDisplay(due));
        }
        b.tvDueDate.setText(text);

        int color = ContextCompat.getColor(context, overdue ? R.color.due_overdue : R.color.due_normal);
        b.tvDueDate.setTextColor(color);
        Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_event);
        if (icon != null) {
            int size = (int) (14 * context.getResources().getDisplayMetrics().density);
            icon = icon.mutate();
            icon.setBounds(0, 0, size, size);
            icon.setTint(color);
            b.tvDueDate.setCompoundDrawablesRelative(icon, null, null, null);
        }
    }

    static class ToodoziViewHolder extends RecyclerView.ViewHolder {
        final ItemRowToodoziBinding binding;

        ToodoziViewHolder(ItemRowToodoziBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
