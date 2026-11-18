package com.f52124014.todoozi.view.fragment;

import androidx.lifecycle.LiveData;

import com.f52124014.todoozi.R;
import com.f52124014.todoozi.model.NoteTaskModel;

import java.util.List;

/** Halaman Catatan. */
public class NotesFragment extends ToodoziListFragment {

    @Override
    protected String getCategory() {
        return NoteTaskModel.CATEGORY_NOTE;
    }

    @Override
    protected LiveData<List<NoteTaskModel>> getData() {
        return viewModel.getNotes();
    }

    @Override
    protected String buildSubtitle(List<NoteTaskModel> list) {
        return getString(R.string.subtitle_notes, list.size());
    }

    @Override
    protected int getHeaderTitleRes() { return R.string.header_notes; }

    @Override
    protected int getEmptyTitleRes() { return R.string.empty_notes_title; }

    @Override
    protected int getEmptyHintRes() { return R.string.empty_notes_hint; }

    @Override
    protected int getEmptyIconRes() { return R.drawable.ic_note; }
}
