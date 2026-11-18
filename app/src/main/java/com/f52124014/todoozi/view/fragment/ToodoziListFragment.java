package com.f52124014.todoozi.view.fragment;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.f52124014.todoozi.R;
import com.f52124014.todoozi.databinding.FragmentToodoziListBinding;
import com.f52124014.todoozi.model.NoteTaskModel;
import com.f52124014.todoozi.view.TodoEditorSheet;
import com.f52124014.todoozi.view.adapter.ToodoziAdapter;
import com.f52124014.todoozi.viewmodel.ToodoziViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * Fragment dasar untuk halaman daftar (Catatan & Tugas) - Modul 1 (pewarisan) & Modul 3 (Fragment).
 * Subclass cukup menentukan kategori, sumber data, dan teks header.
 */
public abstract class ToodoziListFragment extends Fragment {

    protected FragmentToodoziListBinding binding;
    protected ToodoziViewModel viewModel;
    private ToodoziAdapter adapter;

    private boolean loading = false;
    private List<NoteTaskModel> current = null;

    protected abstract String getCategory();

    protected abstract LiveData<List<NoteTaskModel>> getData();

    protected abstract String buildSubtitle(List<NoteTaskModel> list);

    @StringRes
    protected abstract int getHeaderTitleRes();

    @StringRes
    protected abstract int getEmptyTitleRes();

    @StringRes
    protected abstract int getEmptyHintRes();

    @DrawableRes
    protected abstract int getEmptyIconRes();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentToodoziListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ToodoziViewModel.class);

        binding.tvHeaderTitle.setText(getHeaderTitleRes());
        binding.ivEmpty.setImageResource(getEmptyIconRes());

        adapter = new ToodoziAdapter();
        binding.rvToodozi.setAdapter(adapter);
        binding.rvToodozi.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter.setOnItemClickListener(item ->
                TodoEditorSheet.show(requireActivity(), getCategory(), item, new TodoEditorSheet.Listener() {
                    @Override
                    public void onSave(NoteTaskModel updated) {
                        viewModel.updateToodozi(updated);
                        Snackbar.make(binding.getRoot(), R.string.msg_updated, Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDelete(NoteTaskModel toDelete) {
                        deleteWithUndo(toDelete);
                    }
                }));

        // Centang tugas sekarang benar-benar disimpan ke database
        adapter.setOnTaskCheckedChangeListener((item, isChecked) -> viewModel.setCompleted(item, isChecked));

        attachSwipeToDelete();

        binding.swipeRefresh.setColorSchemeResources(R.color.md_primary);
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        viewModel.getGridMode().observe(getViewLifecycleOwner(),
                grid -> applyLayoutManager(Boolean.TRUE.equals(grid)));

        getData().observe(getViewLifecycleOwner(), list -> {
            current = list;
            adapter.submitList(list);
            if (list != null) binding.tvHeaderSubtitle.setText(buildSubtitle(list));
            updateStates();
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            loading = Boolean.TRUE.equals(isLoading);
            if (!loading) binding.swipeRefresh.setRefreshing(false);
            updateStates();
        });
    }

    private void applyLayoutManager(boolean grid) {
        RecyclerView.LayoutManager lm = binding.rvToodozi.getLayoutManager();
        boolean isGrid = lm instanceof GridLayoutManager;
        if (lm != null && isGrid == grid) return;
        binding.rvToodozi.setLayoutManager(grid
                ? new GridLayoutManager(requireContext(), 2)
                : new LinearLayoutManager(requireContext()));
    }

    private void updateStates() {
        boolean empty = current != null && current.isEmpty();
        boolean searching = !viewModel.getQueryValue().isEmpty();

        binding.tvEmptyTitle.setText(searching ? R.string.empty_search_title : getEmptyTitleRes());
        binding.tvEmptyHint.setText(searching ? R.string.empty_search_hint : getEmptyHintRes());
        binding.layoutEmpty.setVisibility(empty && !loading ? View.VISIBLE : View.GONE);

        boolean showProgress = loading && (current == null || current.isEmpty())
                && !binding.swipeRefresh.isRefreshing();
        binding.progressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
    }

    private void deleteWithUndo(NoteTaskModel item) {
        viewModel.deleteToodozi(item);
        Snackbar.make(binding.getRoot(), R.string.msg_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> viewModel.restoreToodozi(item))
                .show();
    }

    /** Geser kiri/kanan untuk menghapus, dengan latar merah lembut dan opsi Batalkan. */
    private void attachSwipeToDelete() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(ContextCompat.getColor(requireContext(), R.color.md_error_container));
        final float radius = 18 * getResources().getDisplayMetrics().density;

        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        NoteTaskModel item = adapter.getItemAt(viewHolder.getBindingAdapterPosition());
                        if (item != null) deleteWithUndo(item);
                    }

                    @Override
                    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
                        View v = viewHolder.itemView;
                        if (dX != 0f) {
                            float left = dX > 0 ? v.getLeft() : v.getRight() + dX;
                            float right = dX > 0 ? v.getLeft() + dX : v.getRight();
                            c.drawRoundRect(left, v.getTop(), right, v.getBottom(), radius, radius, paint);
                        }
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }
                };
        new ItemTouchHelper(callback).attachToRecyclerView(binding.rvToodozi);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
