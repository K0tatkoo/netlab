package com.n3d.netlab.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3d.netlab.AppViewModel
import com.n3d.netlab.i18n.Block
import com.n3d.netlab.i18n.Lesson
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.ui.components.Banner
import com.n3d.netlab.ui.components.BitsLegend
import com.n3d.netlab.ui.components.BitsStrip
import com.n3d.netlab.ui.components.Dot
import com.n3d.netlab.ui.components.FormulaBlock
import com.n3d.netlab.ui.components.InfoRow
import com.n3d.netlab.ui.components.NeuCard
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuMotion
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset

@Composable
fun LearnScreen(vm: AppViewModel) {
    val s = vm.strings
    val neu = LocalNeu.current
    // One open at a time. Six lessons all expanded is a scroll bar the size of
    // a grain of rice and no sense of where you are in the material.
    var openIndex by rememberSaveable { mutableIntStateOf(-1) }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                s.learnTitle,
                style = NeuType.Title,
                color = neu.text,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
        itemsIndexed(s.lessons) { index, lesson ->
            LessonCard(
                lesson = lesson,
                index = index + 1,
                expanded = openIndex == index,
                s = s,
                onToggle = { openIndex = if (openIndex == index) -1 else index },
            )
        }
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    index: Int,
    expanded: Boolean,
    s: Strings,
    onToggle: () -> Unit,
) {
    val neu = LocalNeu.current
    NeuCard(onClick = onToggle, padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(28.dp).height(28.dp).neuInset(NeuRadius.Pill, NeuDepths.InsetSm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    index.toString(),
                    style = NeuType.Small.copy(fontWeight = FontWeight.Bold),
                    color = neu.accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    lesson.title,
                    style = NeuType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = neu.text,
                )
                Text(lesson.summary, style = NeuType.Small, color = neu.faint)
            }
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = neu.faint,
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(NeuMotion.SlideMs, easing = NeuMotion.Ease)) + fadeIn(tween(NeuMotion.FadeMs)),
            exit = shrinkVertically(tween(NeuMotion.FadeMs)) + fadeOut(tween(120)),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                lesson.body.forEach { block ->
                    LessonBlock(block, s)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun LessonBlock(block: Block, s: Strings) {
    val neu = LocalNeu.current
    when (block) {
        is Block.Para -> Text(
            block.text,
            style = NeuType.Body.copy(lineHeight = 22.sp),
            color = neu.dim,
        )

        is Block.Bullets -> Column(Modifier.fillMaxWidth()) {
            block.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Box(Modifier.padding(top = 7.dp)) { Dot(neu.accent, 6.dp) }
                    Spacer(Modifier.width(10.dp))
                    Text(item, style = NeuType.Small.copy(lineHeight = 19.sp), color = neu.dim)
                }
            }
        }

        is Block.Table -> Box(
            Modifier
                .fillMaxWidth()
                .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Column {
                block.rows.forEach { (label, value) -> InfoRow(label, value) }
            }
        }

        is Block.Formula -> FormulaBlock(block.text)

        is Block.Note -> Banner(block.text, neu.warn)

        is Block.Bits -> Column(Modifier.fillMaxWidth()) {
            BitsStrip(block.bits, block.networkBits, caption = block.caption)
            Spacer(Modifier.height(8.dp))
            BitsLegend(s.bitsLegendNetwork, s.bitsLegendHost)
        }
    }
}
