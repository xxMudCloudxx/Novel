package com.novel.page.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.novel.page.component.NovelText
import com.novel.page.home.viewmodel.CategoryInfo
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp
import com.novel.utils.ssp

/**
 * 首页筛选器组件
 */
@Composable
fun HomeFilterBar(
    filters: List<CategoryInfo>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(15.wdp),
        contentPadding = PaddingValues(horizontal = 15.wdp)
    ) {
        items(filters) { filter ->
            FilterChip(
                filter = filter.name,
                isSelected = filter.name == selectedFilter,
                onClick = { onFilterSelected(filter.name) }
            )
        }
    }
}

@Composable
private fun FilterChip(
    filter: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NovelText(
        text = filter,
        fontSize = 16.ssp,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 8.wdp),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) NovelColors.NovelText else NovelColors.NovelTextGray
    )
}

@Preview
@Composable
fun HomeFilterBarPreview() {
    HomeFilterBar(
        filters = listOf(
            CategoryInfo("0", "推荐"),
            CategoryInfo("1", "玄幻奇幻"),
            CategoryInfo("2", "武侠仙侠")
        ),
        selectedFilter = "推荐",
        onFilterSelected = {}
    )
}