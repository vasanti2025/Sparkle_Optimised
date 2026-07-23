package com.loyalstring.rfid.ui.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

const val LIST_PAGE_SIZE = 10

@Composable
fun LazyListLoadMoreEffect(
    listState: LazyListState,
    loadedCount: Int,
    totalCount: Int,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(listState, loadedCount, totalCount) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.collect { lastVisibleIndex ->
            if (loadedCount > 0 &&
                lastVisibleIndex >= loadedCount - 1 &&
                loadedCount < totalCount
            ) {
                onLoadMore()
            }
        }
    }
}
