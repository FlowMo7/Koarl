package dev.moetz.koarl.backend.dashboard.page

import kotlin.math.floor

class PaginationManager {

    data class PaginationResult(
        val activePage: Int,
        val allPagesStart: Int,
        val allPagesEnd: Int,
        val rangeStart: Int,
        val rangeEnd: Int
    )

    fun getPaginatedPagesToDisplay(
        numberOfPages: Int,
        activePage: Int,
        maxDisplayedPages: Int
    ): PaginationResult {
        return if (numberOfPages <= maxDisplayedPages) {
            PaginationResult(
                activePage = activePage,
                allPagesStart = 1,
                allPagesEnd = numberOfPages,
                rangeStart = 1,
                rangeEnd = numberOfPages
            )
        } else {
            val offset = (activePage - (maxDisplayedPages / 2))
            when {
                activePage < floor(maxDisplayedPages / 2f) -> {
                    PaginationResult(
                        activePage = activePage,
                        allPagesStart = 1,
                        allPagesEnd = numberOfPages,
                        rangeStart = 1,
                        rangeEnd = maxDisplayedPages - 1
                    )
                }
                activePage < floor(numberOfPages - (maxDisplayedPages / 2f)) + 1 -> {
                    PaginationResult(
                        activePage = activePage,
                        allPagesStart = 1,
                        allPagesEnd = numberOfPages,
                        rangeStart = 1 + offset,
                        rangeEnd = maxDisplayedPages - 1 + offset
                    )
                }
                else -> {
                    PaginationResult(
                        activePage = activePage,
                        allPagesStart = 1,
                        allPagesEnd = numberOfPages,
                        rangeStart = numberOfPages - maxDisplayedPages + 2,
                        rangeEnd = numberOfPages
                    )
                }
            }
        }
    }


}