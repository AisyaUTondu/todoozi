package com.f52124014.todoozi.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.f52124014.todoozi.R;
import com.f52124014.todoozi.databinding.SheetTodooziEditorBinding;
import com.f52124014.todoozi.model.NoteTaskModel;
import com.f52124014.todoozi.util.DateUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.MaterialDatePicker;

/** Bottom sheet untuk menambah atau mengedit catatan/tugas (dipakai bersama oleh semua halaman). */
public final class TodoEditorSheet {

    public interface Listener {
        void onSave(NoteTaskModel item);

        void onDelete(NoteTaskModel item);
    }

    private TodoEditorSheet() {}

    public static void show(@NonNull FragmentActivity activity,
                            @NonNull String category,
                            @Nullable NoteTaskModel existing,
                            @NonNull Listener listener) {

        final boolean isNote = NoteTaskModel.CATEGORY_NOTE.equals(category);
        final boolean isEdit = existing != null;

        final SheetTodooziEditorBinding b = SheetTodooziEditorBinding.inflate(LayoutInflater.from(activity));
        final BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(b.getRoot());
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);

        Window window = dialog.getWindow();
        if (window != null) {
            int state = isEdit ? WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                    : WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | state);
        }

        // Judul sheet
        if (isNote) {
            b.tvSheetTitle.setText(isEdit ? R.string.sheet_edit_note : R.string.sheet_add_note);
        } else {
            b.tvSheetTitle.setText(isEdit ? R.string.sheet_edit_task : R.string.sheet_add_task);
        }

        // Tenggat hanya untuk tugas
        b.tilDueDate.setVisibility(isNote ? View.GONE : View.VISIBLE);
        final String[] dueIso = {isEdit ? existing.getDueDate() : ""};

        if (isEdit) {
            b.etTitle.setText(existing.getTitle());
            b.etContent.setText(existing.getContent());
            b.etDueDate.setText(DateUtil.formatDisplay(dueIso[0]));
        }
        b.tilDueDate.setEndIconVisible(!dueIso[0].isEmpty());

        b.etDueDate.setOnClickListener(v -> showDatePicker(activity, dueIso, b));
        b.tilDueDate.setEndIconOnClickListener(v -> {
            dueIso[0] = "";
            b.etDueDate.setText("");
            b.tilDueDate.setEndIconVisible(false);
        });

        b.btnDelete.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        b.btnCancel.setOnClickListener(v -> dialog.dismiss());

        b.btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            listener.onDelete(existing);
        });

        b.btnSave.setOnClickListener(v -> {
            String title = String.valueOf(b.etTitle.getText()).trim();
            String content = String.valueOf(b.etContent.getText()).trim();
            b.tilTitle.setError(null);

            if (title.isEmpty()) {
                b.tilTitle.setError(activity.getString(R.string.err_title_required));
                return;
            }

            String due = isNote ? "" : dueIso[0];
            NoteTaskModel item;
            if (isEdit) {
                item = existing.copy();   // salinan, agar objek di daftar tidak berubah sebelum disimpan
                item.setTitle(title);
                item.setContent(content);
                item.setDueDate(due);
            } else {
                item = new NoteTaskModel(0, title, content, category, false, due);
            }
            dialog.dismiss();
            listener.onSave(item);
        });

        dialog.show();
        if (!isEdit) b.etTitle.requestFocus();
    }

    private static void showDatePicker(FragmentActivity activity, String[] dueIso,
                                       SheetTodooziEditorBinding b) {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.pick_due_date);
        if (!dueIso[0].isEmpty()) {
            builder.setSelection(DateUtil.toUtcMillis(dueIso[0]));
        }
        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            dueIso[0] = DateUtil.fromUtcMillis(selection);
            b.etDueDate.setText(DateUtil.formatDisplay(dueIso[0]));
            b.tilDueDate.setEndIconVisible(true);
        });
        picker.show(activity.getSupportFragmentManager(), "due_date_picker");
    }
}
