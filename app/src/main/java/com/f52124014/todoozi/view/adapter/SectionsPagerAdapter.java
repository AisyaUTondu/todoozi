package com.f52124014.todoozi.view.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.f52124014.todoozi.view.fragment.NotesFragment;
import com.f52124014.todoozi.view.fragment.ProfileFragment;
import com.f52124014.todoozi.view.fragment.TasksFragment;

/** Adapter ViewPager2 untuk 3 halaman: Catatan, Tugas, Profil (Modul 3). */
public class SectionsPagerAdapter extends FragmentStateAdapter {

    public SectionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new NotesFragment();
            case 1:
                return new TasksFragment();
            default:
                return new ProfileFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
