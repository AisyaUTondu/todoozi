package com.f52124014.todoozi.network;

import java.util.List;

/** { "data": [ ...item... ] } */
public class TodoListResponse {
    private List<TodoDto> data;

    public List<TodoDto> getData() { return data; }
}
