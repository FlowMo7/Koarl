package dev.moetz.koarl.backend.dashboard.page

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test


internal class PaginationManagerTest {

    private lateinit var manager: PaginationManager

    @Before
    fun setUp() {
        manager = PaginationManager()
    }


    @Test
    fun notPaginatedTest1() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 10,
            activePage = 1,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 1,
            allPagesStart = 1,
            allPagesEnd = 10,
            rangeStart = 1,
            rangeEnd = 10
        )
    }


    @Test
    fun notPaginatedTest2() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 10,
            activePage = 5,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 5,
            allPagesStart = 1,
            allPagesEnd = 10,
            rangeStart = 1,
            rangeEnd = 10
        )
    }


    @Test
    fun notPaginatedTest3() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 10,
            activePage = 9,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 9,
            allPagesStart = 1,
            allPagesEnd = 10,
            rangeStart = 1,
            rangeEnd = 10
        )
    }


    @Test
    fun notPaginatedTest4() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 10,
            activePage = 10,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 10,
            allPagesStart = 1,
            allPagesEnd = 10,
            rangeStart = 1,
            rangeEnd = 10
        )
    }


    @Test
    fun paginatedTestAtStart1() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 1,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 1,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 1,
            rangeEnd = 9
        )
    }


    @Test
    fun paginatedTestAtStart2() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 2,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 2,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 1,
            rangeEnd = 9
        )
    }


    @Test
    fun paginatedTestAtStart3() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 3,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 3,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 1,
            rangeEnd = 9
        )
    }


    @Test
    fun paginatedTestAtStart4() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 4,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 4,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 1,
            rangeEnd = 9
        )
    }


    @Test
    fun paginatedTestAtStart5() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage =5,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 5,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 1,
            rangeEnd = 9
        )
    }


    @Test
    fun paginatedTestAtStart6() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 6,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 6,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 2,
            rangeEnd = 10
        )
    }


    @Test
    fun paginatedTestAtStart7() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 7,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 7,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 3,
            rangeEnd = 11
        )
    }


    @Test
    fun paginatedTestAtStart8() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 8,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 8,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 4,
            rangeEnd = 12
        )
    }


    @Test
    fun paginatedTestAtStart9() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 9,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 9,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 5,
            rangeEnd = 13
        )
    }


    @Test
    fun paginatedTestAtStart10() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 10,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 10,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 6,
            rangeEnd = 14
        )
    }


    @Test
    fun paginatedTestAtStart11() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 11,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 11,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 7,
            rangeEnd = 15
        )
    }


    @Test
    fun paginatedTestAtStart12() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 12,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 12,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 7,
            rangeEnd = 15
        )
    }


    @Test
    fun paginatedTestAtStart13() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 13,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 13,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 7,
            rangeEnd = 15
        )
    }


    @Test
    fun paginatedTestAtStart14() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 14,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 14,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 7,
            rangeEnd = 15
        )
    }


    @Test
    fun paginatedTestAtStart15() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 15,
            activePage = 15,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 15,
            allPagesStart = 1,
            allPagesEnd = 15,
            rangeStart = 7,
            rangeEnd = 15
        )
    }

    @Test
    fun notPaginatedAtTooLittlePages1() {
        manager.getPaginatedPagesToDisplay(
            numberOfPages = 5,
            activePage = 1,
            maxDisplayedPages = 10
        ) shouldBeEqualTo PaginationManager.PaginationResult(
            activePage = 1,
            allPagesStart = 1,
            allPagesEnd = 5,
            rangeStart = 1,
            rangeEnd = 5
        )
    }


}