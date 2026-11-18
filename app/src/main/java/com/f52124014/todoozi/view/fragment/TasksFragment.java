package com.f52124014.todoozi.view.fragment;

import androidx.lifecycle.LiveData;

import com.f52124014.todoozi.R;
import com.f52124014.todoozi.model.NoteTaskModel;

import java.util.List;

/** Halaman Tugas Harian. */
public class TasksFragment extends ToodoziListFragment {

    @Override
    protected String getCategory() {
        return NoteTaskModel.CATEGORY_TASK;
    }

    @Override
    protected LiveData<List<NoteTaskModel>> getData() {
        return viewModel.getTasks();
    }

    @Override
    protected String buildSubtitle(List<NoteTaskModel> list) {
        int done = 0;
        for (NoteTaskModel item : list) {
            if (item.isCompleted()) done++;
        }
        return getString(R.string.subtitle_tasks, list.size(), done);
    }

    @Override
    protected int getHeaderTitleRes() { return R.string.header_tasks; }

    @Override
    protected int getEmptyTitleRes() { return R.string.empty_tasks_title; }

    @Override
    protected int getEmptyHintRes() { return R.string.empty_tasks_hint; }

    @Override
    protected int getEmptyIconRes() { return R.drawable.ic_task; }
}
