package com.f52124014.todoozi.view.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.f52124014.todoozi.R;
import com.f52124014.todoozi.databinding.FragmentProfileBinding;
import com.f52124014.todoozi.model.NoteTaskModel;
import com.f52124014.todoozi.util.AppConfig;
import com.f52124014.todoozi.util.SessionManager;
import com.f52124014.todoozi.view.LoginActivity;
import com.f52124014.todoozi.viewmodel.AuthViewModel;
import com.f52124014.todoozi.viewmodel.ToodoziViewModel;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


/** Halaman Profil: info akun, statistik, dan tombol keluar. */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SessionManager session = new SessionManager(requireContext());
        ToodoziViewModel viewModel = new ViewModelProvider(requireActivity()).get(ToodoziViewModel.class);
        AuthViewModel authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        binding.tvUsername.setText(session.getUsername());
        binding.tvStatus.setText(AppConfig.USE_API
                ? R.string.profile_mode_server : R.string.profile_mode_local);
        bindAvatar(session.getAvatarUrl());

        viewModel.getAllNotes().observe(getViewLifecycleOwner(), list ->
                binding.tvStatNotes.setText(String.valueOf(list == null ? 0 : list.size())));

        viewModel.getAllTasks().observe(getViewLifecycleOwner(), list -> {
            int total = list == null ? 0 : list.size();
            int done = 0;
            if (list != null) {
                for (NoteTaskModel item : list) {
                    if (item.isCompleted()) done++;
                }
            }
            binding.tvStatTasks.setText(String.valueOf(total));
            binding.tvStatDone.setText(String.valueOf(done));
        });

        binding.btnLogout.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.logout_title)
                        .setMessage(R.string.logout_message)
                        .setPositiveButton(R.string.btn_logout, (dialog, which) -> {
                            authViewModel.logout();
                            Intent intent = new Intent(requireContext(), LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            requireActivity().finish();
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show());
    }

    /** Foto profil dimuat dari URL dengan Glide (Modul 5); jika tidak ada, tampilkan ikon orang. */
    private void bindAvatar(String url) {
        int padding = (int) (22 * getResources().getDisplayMetrics().density);
        if (url == null || url.isEmpty()) {
            binding.imgAvatar.setPadding(padding, padding, padding, padding);
            binding.imgAvatar.setImageResource(R.drawable.ic_person);
            int primary = MaterialColors.getColor(binding.imgAvatar, com.google.android.material.R.attr.colorPrimary);
            binding.imgAvatar.setImageTintList(ColorStateList.valueOf(primary));
        } else {
            binding.imgAvatar.setPadding(0, 0, 0, 0);
            binding.imgAvatar.setImageTintList(null);
            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.imgAvatar);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
