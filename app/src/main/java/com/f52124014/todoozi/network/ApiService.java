package com.f52124014.todoozi.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/** Daftar endpoint REST API Todoozi (Modul 5 - Retrofit). */
public interface ApiService {

    @POST("auth/register")
    Call<AuthResponse> register(@Body AuthRequest body);

    @POST("auth/login")
    Call<AuthResponse> login(@Body AuthRequest body);

    @GET("todoozi")
    Call<TodoListResponse> getTodoozi();

    @POST("todoozi")
    Call<TodoResponse> createTodoozi(@Body TodoDto body);

    @PUT("todoozi/{id}")
    Call<TodoResponse> updateTodoozi(@Path("id") int id, @Body TodoDto body);

    @DELETE("todoozi/{id}")
    Call<MessageResponse> deleteTodoozi(@Path("id") int id);
}
