package com.novel.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.novel.R
import com.novel.page.component.NovelText
import com.novel.page.home.HomePage
import com.novel.page.login.LoginPage
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.utils.ssp
import com.novel.utils.wdp
import kotlinx.coroutines.launch

@Composable
fun MainPage() {
    // 页面内容列表——注意中间第三项是 ChatScreen
    val labels = listOf("首页", "分类", "福利", "书架", "我的")
    val imageId = listOf(
        R.drawable.home,
        R.drawable.clarify,
        R.drawable.welfare,
        R.drawable.bookshelf,
        R.drawable.my
    )
    val pageCount = labels.size

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false
        ) { pageIndex ->
            when (pageIndex) {
                0 -> HomePage()
                2 -> LoginPage()
                4 -> ReactNativePage()
                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { NovelText("Page Not Found") }
            }
        }

        BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.wdp),
            containerColor = MaterialTheme.colorScheme.background,
            contentPadding = PaddingValues(0.wdp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.wdp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                labels.forEachIndexed { index, _ ->
                    NavButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        isSelect = pagerState.currentPage == index,
                        text = labels[index],
                        id = imageId[index]
                    )
                }
            }
        }
    }
}

@Composable
fun NavButton(
    onClick: () -> Unit = {},
    isSelect: Boolean = false,
    text: String,
    id: Int
) {
    val color = if (isSelect) NovelColors.NovelText else NovelColors.NovelTextGray
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
    ) {
        Image(
            painter = painterResource(id = id),
            contentDescription = "back",
            modifier = Modifier
                .size(20.wdp, 20.wdp),
            colorFilter = ColorFilter.tint(color)
        )
        NovelText(
            text = text,
            fontSize = 10.ssp,
            lineHeight = 14.ssp,
            fontWeight = if (isSelect) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.wdp),
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }

}

@Preview
@Composable
fun MainPagePreview() {
    NovelTheme {
        MainPage()
    }
}
