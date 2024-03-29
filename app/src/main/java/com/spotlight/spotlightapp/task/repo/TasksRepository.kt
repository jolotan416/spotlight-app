package com.spotlight.spotlightapp.task.repo

import com.spotlight.spotlightapp.R
import com.spotlight.spotlightapp.data.ErrorEntity
import com.spotlight.spotlightapp.data.Result
import com.spotlight.spotlightapp.data.task.Task
import com.spotlight.spotlightapp.data.task.TaskTimerData
import com.spotlight.spotlightapp.utilities.RepositoryErrorHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksRepository @Inject constructor(
    private val localDataSource: TaskLocalDataSource,
    private val repositoryErrorHandler: RepositoryErrorHandler) {
    companion object {
        private const val MAX_PRIORITY = 5
    }

    val pendingTasks: Flow<List<Task>> = localDataSource.observeTasks()
        .map { tasks -> filterTasksByCompletion(tasks, false) }

    val completedTasks: Flow<List<Task>> = localDataSource.observeTasks()
        .map { tasks -> filterTasksByCompletion(tasks, true) }

    val dailyIntentList: Flow<List<Task>> = localDataSource.observeTasks()
        .map { tasks ->
            tasks.filter { it.priority != 0 }
                .sortedBy { it.priority }
        }

    fun observeTask(taskId: Int): Flow<Task?> = localDataSource.observeTask(taskId)

    suspend fun insertTask(task: Task): Result<Any?> {
        return repositoryErrorHandler.handleGeneralRepositoryOperation {
            localDataSource.insertTask(task)

            Result.Success(null)
        }
    }

    suspend fun updateTask(task: Task, willTogglePriority: Boolean = false): Result<Task> {
        return repositoryErrorHandler.handleGeneralRepositoryOperation {
            if (willTogglePriority) {
                toggleTaskPriority(task)
            } else {
                localDataSource.updateTask(task)

                Result.Success(task)
            }
        }
    }

    suspend fun toggleTaskTimer(task: Task, isTimerRunning: Boolean): Result<Task> {
        return repositoryErrorHandler.handleGeneralRepositoryOperation {
            val newTaskTimerData: TaskTimerData? = if (isTimerRunning) {
                val newCurrentTimerStartDate = Calendar.getInstance()
                val newCurrentTimerEndDate = Calendar.getInstance().apply {
                    time = newCurrentTimerStartDate.time
                    add(task.alertInterval.unit.calendarUnit, task.alertInterval.amount)
                }

                TaskTimerData(newCurrentTimerStartDate.time, newCurrentTimerEndDate.time)
            } else null
            val updatedTask = task.copy(taskTimerData = newTaskTimerData)
            updateTask(updatedTask)
        }
    }

    suspend fun toggleTaskFinished(task: Task): Result<Task> {
        return repositoryErrorHandler.handleGeneralRepositoryOperation {
            val isTaskFinished = task.isFinished
            val updatedTask = task.copy(isFinished = !isTaskFinished)
            updateTask(updatedTask, updatedTask.isFinished && updatedTask.priority != 0)
        }
    }

    suspend fun deleteTask(task: Task): Result<Any?> {
        return repositoryErrorHandler.handleGeneralRepositoryOperation {
            if (task.priority != 0) {
                toggleTaskPriority(task)
            }
            localDataSource.deleteTask(task)

            Result.Success(null)
        }
    }

    suspend fun resetDailyIntentListPriority(): Result<Any?> {
        return repositoryErrorHandler.handleGeneralRepositoryOperation {
            dailyIntentList.firstOrNull()?.forEach { updateTaskPriority(it, 0) }

            Result.Success(null)
        }
    }

    private suspend fun toggleTaskPriority(task: Task): Result<Task> {
        val tasks = localDataSource.getTasks()
        val maxPendingTaskListPriority = tasks.maxOfOrNull { it.priority } ?: 0

        return when {
            task.priority != 0 -> {
                tasks.filter { it.priority > task.priority }
                    .forEach { succeedingTask ->
                        updateTaskPriority(succeedingTask, succeedingTask.priority - 1)
                    }

                updateTaskPriority(task, 0)

                Result.Success(task)
            }
            maxPendingTaskListPriority >= MAX_PRIORITY -> {
                Result.Error(ErrorEntity(R.string.maximum_daily_intent_task_error_message).apply {
                    setErrorMessageArguments(MAX_PRIORITY)
                })
            }
            else -> {
                updateTaskPriority(task, maxPendingTaskListPriority + 1)

                Result.Success(task)
            }
        }
    }

    private suspend fun updateTaskPriority(task: Task, priority: Int) {
        localDataSource.updateTask(task.copy(priority = priority))
    }

    private fun filterTasksByCompletion(tasks: List<Task>, isFinished: Boolean) =
        sortTasks(tasks.filter { it.isFinished == isFinished })

    private fun sortTasks(tasks: List<Task>) =
        tasks.filter { it.priority != 0 }.sortedBy { it.priority } +
                tasks.filter { it.priority == 0 }
}