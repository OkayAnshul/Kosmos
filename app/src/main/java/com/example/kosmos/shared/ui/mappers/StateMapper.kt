package com.example.kosmos.shared.ui.mappers

import com.example.kosmos.shared.ui.layouts.ListState

/**
 * Mapper functions to convert between different state types
 */
object StateMapper {

    /**
     * Convert a loading boolean and data to ListState
     */
    fun <T> toListState(
        isLoading: Boolean,
        data: List<T>?,
        error: String?
    ): ListState<T> {
        return when {
            isLoading -> ListState.Loading
            error != null -> ListState.Error(error)
            data != null -> ListState.Success(data)
            else -> ListState.Loading
        }
    }

    /**
     * Map ListState with a transformation function
     */
    fun <T, R> mapListState(
        state: ListState<T>,
        transform: (T) -> R
    ): ListState<R> {
        return when (state) {
            is ListState.Loading -> ListState.Loading
            is ListState.Error -> ListState.Error(state.message)
            is ListState.Success -> ListState.Success(state.data.map(transform))
        }
    }

    /**
     * Convert nullable data to ListState
     */
    fun <T> fromNullable(data: T?, errorMessage: String = "Data not available"): ListState<T> {
        return if (data != null) {
            ListState.Success(listOf(data))
        } else {
            ListState.Error(errorMessage)
        }
    }

    /**
     * Combine multiple ListStates
     */
    fun <T> combine(states: List<ListState<T>>): ListState<List<T>> {
        // If any state is loading, return loading
        if (states.any { it is ListState.Loading }) {
            return ListState.Loading
        }

        // If any state has error, return first error
        val firstError = states.firstOrNull { it is ListState.Error } as? ListState.Error
        if (firstError != null) {
            return ListState.Error(firstError.message)
        }

        // All states are success, combine data
        val allData = states.mapNotNull { (it as? ListState.Success)?.data }
        return ListState.Success(allData)
    }

    /**
     * Convert Result to ListState (for repository responses)
     */
    fun <T> fromResult(result: Result<List<T>>): ListState<T> {
        return result.fold(
            onSuccess = { ListState.Success(it) },
            onFailure = { ListState.Error(it.message ?: "Unknown error") }
        )
    }
}
