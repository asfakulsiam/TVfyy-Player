package com.example.domain.model

data class EventServer(
    val name: String,
    val url: String
)

data class TopEvent(
    val id: String,
    val title: String,
    val description: String,
    val image: String,
    val servers: List<EventServer>
)
