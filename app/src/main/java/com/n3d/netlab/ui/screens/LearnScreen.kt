package com.n3d.netlab.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3d.netlab.AppViewModel
import com.n3d.netlab.i18n.Chapter
import com.n3d.netlab.i18n.Page
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.ui.components.Banner
import com.n3d.netlab.ui.components.ButtonTone
import com.n3d.netlab.ui.components.LessonBlock
import com.n3d.netlab.ui.components.NeuButton
import com.n3d.netlab.ui.components.NeuCard
import com.n3d.netlab.ui.components.NeuIconButton
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset

/**
 * The course.
 *
 * Two screens, not one: a list of chapters, and a reader that shows exactly one
 * page at a time. The old version was six accordions, which let a reader flick
 * past three screens of binary in one gesture and then wonder why the mask
 * chapter made no sense. Paging is the only thing that makes the material
 * cumulative in practice as well as on paper.
 */
@Composable
fun LearnScreen(vm: AppViewModel) {
    val s = vm.strings
    var openChapter by rememberSaveable { mutableIntStateOf(-1) }

    val chapter = s.course.getOrNull(openChapter)
    if (chapter != null) {
        ChapterReader(
            chapter = chapter,
            number = openChapter + 1,
            total = s.course.size,
            s = s,
            onClose = { openChapter = -1 },
            onNextChapter = if (openChapter < s.course.lastIndex) {
                { openChapter += 1 }
            } else {
                null
            },
            onRead = { vm.markChapterRead(openChapter) },
        )
    } else {
        ChapterList(s, read = vm.settings.chaptersRead, onOpen = { openChapter = it })
    }
}

@Composable
internal fun ChapterList(s: Strings, read: Set<Int>, onOpen: (Int) -> Unit) {
    val neu = LocalNeu.current
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                Text(s.learnIntro, style = NeuType.Small.copy(lineHeight = 18.sp), color = neu.faint)
            }
        }
        itemsIndexed(s.course) { index, chapter ->
            NeuCard(onClick = { onOpen(index) }, padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(32.dp).neuInset(NeuRadius.Pill, NeuDepths.InsetSm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${index + 1}",
                            style = NeuType.Label.copy(fontWeight = FontWeight.Bold),
                            color = neu.accent,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            chapter.title,
                            style = NeuType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 15.5.sp),
                            color = neu.text,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(chapter.summary, style = NeuType.Small, color = neu.faint)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (index in read) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = s.chapterRead,
                            tint = neu.ok,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = neu.faint,
                    )
                }
                // Only until it has been read: a course that keeps telling a
                // reader where to start is talking past somebody who started.
                if (index == 0 && 0 !in read) {
                    Spacer(Modifier.height(10.dp))
                    Banner(s.learnStartHere, neu.accent)
                }
            }
        }
    }
}

@Composable
internal fun ChapterReader(
    chapter: Chapter,
    number: Int,
    total: Int,
    s: Strings,
    onClose: () -> Unit,
    onNextChapter: (() -> Unit)?,
    onRead: () -> Unit = {},
) {
    val neu = LocalNeu.current
    // Reset to page one whenever a different chapter is opened, rather than
    // dropping the reader into the middle of material they have not read.
    var pageIndex by rememberSaveable(chapter.title) { mutableIntStateOf(0) }
    val lastPage = chapter.pages.lastIndex
    val page = chapter.pages.getOrNull(pageIndex.coerceIn(0, lastPage)) ?: return
    val atEnd = pageIndex >= lastPage

    // Reaching the last page is what counts as having read the chapter. It is
    // the only signal there is, and it is the honest one: the reader got there.
    LaunchedEffect(chapter.title, atEnd) { if (atEnd) onRead() }

    Column(Modifier.fillMaxSize().background(neu.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    s.chapterOf(number, total),
                    style = NeuType.Small,
                    color = neu.faint,
                )
                Text(chapter.title, style = NeuType.Title, color = neu.text, maxLines = 2)
            }
            Spacer(Modifier.width(8.dp))
            NeuIconButton(Icons.Rounded.Close, s.actionClose, onClose)
        }

        PageProgress(chapter.pages.size, pageIndex)

        val listState = rememberLazyListState()
        LaunchedEffect(pageIndex) { listState.scrollToItem(0) }

        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PageCard(page, s) }
            if (atEnd) {
                item { ChapterEnd(s, onNextChapter, onClose) }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeuButton(
                text = s.actionBack,
                onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                enabled = pageIndex > 0,
                modifier = Modifier.weight(1f),
                fill = true,
            )
            NeuButton(
                text = if (atEnd) s.actionClose else s.actionNext,
                onClick = { if (atEnd) onClose() else pageIndex += 1 },
                icon = if (atEnd) null else Icons.AutoMirrored.Rounded.ArrowForward,
                tone = ButtonTone.Accent,
                modifier = Modifier.weight(1f),
                fill = true,
            )
        }

        Text(
            s.pageOf(pageIndex + 1, chapter.pages.size),
            style = NeuType.Small,
            color = neu.faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun PageCard(page: Page, s: Strings) {
    val neu = LocalNeu.current
    NeuCard(padding = 18.dp) {
        Text(
            page.title,
            style = NeuType.Title.copy(fontSize = 18.sp),
            color = neu.text,
        )
        Spacer(Modifier.height(14.dp))
        page.blocks.forEachIndexed { index, block ->
            if (index > 0) Spacer(Modifier.height(14.dp))
            LessonBlock(block, s)
        }
    }
}

@Composable
private fun ChapterEnd(s: Strings, onNextChapter: (() -> Unit)?, onClose: () -> Unit) {
    val neu = LocalNeu.current
    NeuCard(padding = 18.dp) {
        Text(
            s.chapterDone,
            style = NeuType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
            color = neu.ok,
        )
        Spacer(Modifier.height(6.dp))
        Text(s.chapterDoneBody, style = NeuType.Small.copy(lineHeight = 18.sp), color = neu.dim)
        Spacer(Modifier.height(14.dp))
        NeuButton(
            text = if (onNextChapter != null) s.actionNext else s.actionClose,
            onClick = onNextChapter ?: onClose,
            tone = ButtonTone.Accent,
            fill = true,
        )
    }
}

@Composable
private fun PageProgress(total: Int, current: Int) {
    val neu = LocalNeu.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val filled = i <= current
            val alpha by animateFloatAsState(if (filled) 1f else 0.22f, tween(220), label = "page")
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(NeuRadius.Pill))
                    .background(neu.accent.copy(alpha = alpha)),
            )
        }
    }
}
