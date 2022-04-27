package com.spotlight.spotlightapp.task

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.spotlight.spotlightapp.data.CharacterCountData
import com.spotlight.spotlightapp.data.task.Task

class TaskFormViewModel : ViewModel() {
    companion object {
        private const val MAX_TASK_TITLE_CHARACTERS = 140
    }

    private val mutableInitialTask: MutableLiveData<Task> by lazy {
        MutableLiveData<Task>()
    }

    private val mutableTitleCharacterCountData: MutableLiveData<CharacterCountData> by lazy {
        MutableLiveData<CharacterCountData>(
            CharacterCountData(
                MAX_TASK_TITLE_CHARACTERS,
                MAX_TASK_TITLE_CHARACTERS, false))
    }

    private val mutableHasDescription: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    private var title: String = ""
    private var description: String = ""

    val initialTask: LiveData<Task> = mutableInitialTask
    val titleCharacterCountData: LiveData<CharacterCountData> = mutableTitleCharacterCountData
    val isFormValid: MediatorLiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        val onChangedListener = {
            value = title.isNotBlank() && description.isNotBlank()
        }
        addSource(mutableTitleCharacterCountData) { onChangedListener() }
        addSource(mutableHasDescription) { onChangedListener() }
    }

    fun setTask(task: Task?) {
        task?.let {
            mutableInitialTask.value = it
            updateTitle(it.title)
            updateDescription(it.description)
        }
    }

    fun updateTitle(title: String?) {
        title.let {
            this.title = it ?: ""
            val charactersRemaining = MAX_TASK_TITLE_CHARACTERS - (this.title.length ?: 0)
            mutableTitleCharacterCountData.value = titleCharacterCountData.value!!.apply {
                this.charactersRemaining = charactersRemaining
            }
            updateTitleCharacterCountVisibility(false)
        }
    }

    fun updateTitleCharacterCountVisibility(hasFocus: Boolean) {
        mutableTitleCharacterCountData.value = titleCharacterCountData.value!!.apply {
            willShowCharactersRemaining = hasFocus || (charactersRemaining != MAX_TASK_TITLE_CHARACTERS)
        }
    }

    fun updateDescription(description: String?) {
        description.let {
            this.description = it ?: ""
            mutableHasDescription.value = this.description.isNotBlank()
        }
    }

    fun saveTask() {
        // TODO: Handle saving of task in repository
    }
}