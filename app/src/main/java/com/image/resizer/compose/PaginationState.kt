package com.image.resizer.compose

data class PaginationState(
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isLastPage: Boolean = false
)

const val IMAGES_PER_PAGE = 10