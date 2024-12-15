package com.example.cheapchomp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.cheapchomp.repository.DatabaseRepository
import com.example.cheapchomp.repository.OfflineDatabase


class StatisticsViewModel(
    private val databaseRepository: DatabaseRepository,
    private val room_db: OfflineDatabase
) : ViewModel() {
    // Implement your ViewModel logic here



}
