package com.f52124014.todoozi.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.f52124014.todoozi.R;
import com.f52124014.todoozi.databinding.ActivityMainBinding;
import com.f52124014.todoozi.model.NoteTaskModel;
import com.f52124014.todoozi.util.SessionManager;
import com.f52124014.todoozi.view.adapter.SectionsPagerAdapter;
import com.f52124014.todoozi.viewmodel.AuthViewModel;
import com.f52124014.todoozi.viewmodel.ToodoziViewModel;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Halaman utama: Toolbar + TabLayout + ViewPager2 dengan 3 fragment (Catatan, Tugas, Profil).
 */
public class MainActivity extends BaseActivity {

    public static final String EXTRA_USERNAME = "extra_username";
    private static final String KEY_CURRENT_TAB = "current_tab";   // Modul 1: onSaveInstanceState
    private static final int TAB_PROFILE = 2;

    private ActivityMainBinding binding;
    private ToodoziViewModel viewModel;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            openLogin();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets();

        viewModel = new ViewModelProvider(this).get(ToodoziViewModel.class);

        // Toolbar sebagai ActionBar agar menu (cari, list/grid) tampil  -> Modul 2
        setSupportActionBar(binding.toolbar);
        String username = getIntent().getStringExtra(EXTRA_USERNAME);
        if (username == null || username.isEmpty()) username = session.getUsername();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setSubtitle(getString(R.string.greeting, username));
        }

        // ViewPager2 + TabLayout  -> Modul 3
        binding.viewPager.setAdapter(new SectionsPagerAdapter(this));
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.tab_notes);
                    tab.setIcon(R.drawable.ic_note);
                    break;
                case 1:
                    tab.setText(R.string.tab_tasks);
                    tab.setIcon(R.drawable.ic_task);
                    break;
                default:
                    tab.setText(R.string.tab_profile);
                    tab.setIcon(R.drawable.ic_person);
                    break;
            }
        }).attach();

        if (savedInstanceState != null) {
            binding.viewPager.setCurrentItem(savedInstanceState.getInt(KEY_CURRENT_TAB, 0), false);
        }

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == TAB_PROFILE) {
                    binding.fabAdd.hide();
                } else {
                    binding.fabAdd.show();
                }
                invalidateOptionsMenu();
            }
        });

        binding.fabAdd.setOnClickListener(v -> {
            String category = binding.viewPager.getCurrentItem() == 1
                    ? NoteTaskModel.CATEGORY_TASK : NoteTaskModel.CATEGORY_NOTE;
            TodoEditorSheet.show(this, category, null, new TodoEditorSheet.Listener() {
                @Override
                public void onSave(NoteTaskModel item) {
                    viewModel.addToodozi(item);
                }

                @Override
                public void onDelete(NoteTaskModel item) { /* tidak dipakai saat menambah */ }
            });
        });

        // Pesan dari ViewModel (mis. server tidak terjangkau)
        viewModel.getMessage().observe(this, event -> {
            String text = event.getContentIfNotHandled();
            if (text != null) showToast(text);
        });
        viewModel.getSessionExpired().observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                showToast(R.string.msg_session_expired);
                new ViewModelProvider(this).get(AuthViewModel.class).logout();
                openLogin();
            }
        });
        viewModel.getGridMode().observe(this, grid -> invalidateOptionsMenu());

        if (savedInstanceState == null) viewModel.refresh();
    }

    private void applyInsets() {
        final int fabMargin = (int) (20 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            binding.appBar.setPadding(bars.left, bars.top, bars.right, 0);
            binding.viewPager.setPadding(bars.left, 0, bars.right, bars.bottom);
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) binding.fabAdd.getLayoutParams();
            lp.bottomMargin = fabMargin + bars.bottom;
            lp.setMarginEnd(fabMargin + bars.right);
            binding.fabAdd.setLayoutParams(lp);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            outState.putInt(KEY_CURRENT_TAB, binding.viewPager.getCurrentItem());
        }
    }

    // ---------------- Menu ----------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (viewModel == null) return false;
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setMaxWidth(Integer.MAX_VALUE);

            // pulihkan pencarian aktif setelah layar diputar
            String active = viewModel.getQueryValue();
            if (!active.isEmpty()) {
                searchItem.expandActionView();
                searchView.setQuery(active, false);
            }

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    viewModel.setQuery(newText);
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean onListPage = binding != null && binding.viewPager.getCurrentItem() != TAB_PROFILE;
        boolean grid = viewModel != null && Boolean.TRUE.equals(viewModel.getGridMode().getValue());

        menu.findItem(R.id.action_search).setVisible(onListPage);
        // hanya tampilkan tombol untuk mode yang BELUM aktif
        menu.findItem(R.id.action_list).setVisible(onListPage && grid);
        menu.findItem(R.id.action_grid).setVisible(onListPage && !grid);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_list) {
            viewModel.setGridMode(false);
            return true;
        } else if (id == R.id.action_grid) {
            viewModel.setGridMode(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
