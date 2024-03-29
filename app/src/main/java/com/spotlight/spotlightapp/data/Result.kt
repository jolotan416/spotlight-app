package com.spotlight.spotlightapp.data

import androidx.annotation.StringRes
import com.spotlight.spotlightapp.R

sealed class Result<R> {
    data class Success<T>(val content: T) : Result<T>()
    data class Error<T>(val errorEntity: ErrorEntity) : Result<T>()
}

class ErrorEntity(@StringRes val errorMessageRes: Int) {
    companion object {
        val GENERAL_ERROR_ENTITY = ErrorEntity(R.string.general_error_message)
    }

    var errorMessageArguments: Array<Any?> = arrayOf()
        private set

    fun setErrorMessageArguments(vararg errorMessageArguments: Any?) {
        this.errorMessageArguments = errorMessageArguments.asList().toTypedArray()
    }
}