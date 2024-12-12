package com.example.cheapchomp.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cheapchomp.viewmodel.KrogerProductViewModel
import com.example.cheapchomp.repository.KrogerRepository
import com.example.cheapchomp.repository.DatabaseRepository
import com.example.cheapchomp.repository.OfflineDatabase

class KrogerProductViewModelFactory(private val room_db: OfflineDatabase) : ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KrogerProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KrogerProductViewModel(
                krogerRepository = KrogerRepository(),
                databaseRepository = DatabaseRepository(),
                room_db = room_db
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}