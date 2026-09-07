package com.vodileats.core.common.result

/**
 * Tarmoq so'rovlari va boshqa asinxron operatsiyalar uchun umumiy Result wrapper.
 * MVI arxitekturasida UI holatini boshqarish uchun ishlatiladi.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val code: Int? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error
    val isLoading get() = this is Loading

    fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(message, code)
        is Loading -> Loading
    }

    fun onSuccess(action: (T) -> Unit): Resource<T> {
        if (this is Success) action(data)
        return this
    }

    fun onError(action: (String, Int?) -> Unit): Resource<T> {
        if (this is Error) action(message, code)
        return this
    }
}
