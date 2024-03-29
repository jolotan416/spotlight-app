package com.spotlight.spotlightapp.task.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.spotlight.spotlightapp.data.task.Task
import com.spotlight.spotlightapp.task.repo.TasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class DailyIntentListViewModel @Inject constructor(private val tasksRepository: TasksRepository)
    : ViewModel() {
    val dailyIntentList: LiveData<List<Task>>
        get() = tasksRepository.dailyIntentList.asLiveData()

    val willShowEmptyState: LiveData<Boolean>
        get() = tasksRepository.dailyIntentList
            .map { it.isEmpty() }
            .asLiveData()
}