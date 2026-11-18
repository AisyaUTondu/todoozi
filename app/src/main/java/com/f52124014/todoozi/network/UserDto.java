package com.f52124014.todoozi.network;

import com.google.gson.annotations.SerializedName;

public class UserDto {
    private int id;
    private String username;
    @SerializedName("avatar_url")
    private String avatarUrl;

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getAvatarUrl() { return avatarUrl; }
}
