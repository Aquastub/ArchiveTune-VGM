/*
 * ArchiveTune (2026)
 * Â© Chartreux Westia â€” github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.ui.component

import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.LyricsClickKey
import moe.koiverse.archivetune.constants.LyricsLineBlurKey
import moe.koiverse.archivetune.constants.LyricsLineSpacingKey
import moe.koiverse.archivetune.constants.LyricsRomanizeChineseKey
import moe.koiverse.archivetune.constants.LyricsRomanizeHindiKey
import moe.koiverse.archivetune.constants.LyricsRomanizeJapaneseKey
import moe.koiverse.archivetune.constants.LyricsRomanizeKoreanKey
import moe.koiverse.archivetune.constants.LyricsRomanizeOtherLanguagesKey
import moe.koiverse.archivetune.constants.LyricsTextSizeKey
import moe.koiverse.archivetune.constants.PlayerBackgroundStyle
import moe.koiverse.archivetune.constants.PlayerBackgroundStyleKey
import moe.koiverse.archivetune.constants.UseSystemFontKey
import moe.koiverse.archivetune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import moe.koiverse.archivetune.lyrics.LyricsRomanizationPreferences
import moe.koiverse.archivetune.lyrics.LyricsUtils.applyRomanization
import moe.koiverse.archivetune.lyrics.LyricsUtils.lineTextWithTranslation
import moe.koiverse.archivetune.lyrics.LyricsUtils.parseSyncedLyricsDocument
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.ui.component.shimmer.ShimmerHost
import moe.koiverse.archivetune.ui.component.shimmer.TextPlaceholder
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference

private const val ManualScrollTimeoutMs = 3_000L
private const val MaxSelectedLyricsLines = 5

@Composable
fun LyricsV2(
    sliderPositionProvider: () -> Long?,
    lyricsSyncOffset: Int,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val rawLyrics = currentLyrics?.lyrics?.trim()

    val lyricsClick by rememberPreference(LyricsClickKey, defaultValue = true)
    val lyricsTextSize by rememberPreference(LyricsTextSizeKey, defaultValue = 26f)
    val lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val lyricsLineBlur by rememberPreference(LyricsLineBlurKey, defaultValue = true)
    val useSystemFont by rememberPreference(UseSystemFontKey, defaultValue = false)
    val romanizeChinese by rememberPreference(LyricsRomanizeChineseKey, defaultValue = true)
    val romanizeHindi by rememberPreference(LyricsRomanizeHindiKey, defaultValue = true)
    val romanizeJapanese by rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val romanizeKorean by rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val romanizeOtherLanguages by rememberPreference(LyricsRomanizeOtherLanguagesKey, defaultValue = true)
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)

    val romanizationPreferences = remember(
        romanizeJapanese,
        romanizeKorean,
        romanizeChinese,
        romanizeHindi,
        romanizeOtherLanguages,
    ) {
        LyricsRomanizationPreferences(
            romanizeJapanese = romanizeJapanese,
            romanizeKorean = romanizeKorean,
            romanizeChinese = romanizeChinese,
            romanizeHindi = romanizeHindi,
            romanizeOther = romanizeOtherLanguages,
        )
    }

    val lyricsFontFamily = remember(useSystemFont) {
        if (useSystemFont) null else FontFamily(Font(R.font.sfprodisplaybold))
    }

    val textColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) {
        MaterialTheme.colorScheme.onBackground
    } else {
        Color.White
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        when {
            rawLyrics == null -> LyricsLoadingState()
            rawLyrics == LYRICS_NOT_FOUND -> LyricsEmptyState(textColor = textColor)
            else -> LyricsContent(
                rawLyrics = rawLyrics,
                mediaMetadata = mediaMetadata,
                playerPositionProvider = { player.currentPosition },
                currentPositionProvider = sliderPositionProvider,
                lyricsSyncOffset = lyricsSyncOffset,
                lyricsClick = lyricsClick,
                lyricsTextSize = lyricsTextSize,
                lyricsLineSpacing = lyricsLineSpacing,
                lyricsLineBlur = lyricsLineBlur,
                lyricsFontFamily = lyricsFontFamily,
                textColor = textColor,
                romanizationPreferences = romanizationPreferences,
                onSeek = { player.seekTo(it.toLong()) },
            )
        }
    }
}

@Composable
private fun LyricsLoadingState() {
    ShimmerHost {
        repeat(6) {
            TextPlaceholder()
        }
    }
}

@Composable
private fun LyricsEmptyState(textColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.lyrics_not_found),
            style = MaterialTheme.typography.bodyLarge,
            color = textColor.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LyricsContent(
    rawLyrics: String,
    mediaMetadata: MediaMetadata?,
    playerPositionProvider: () -> Long,
    currentPositionProvider: () -> Long?,
    lyricsSyncOffset: Int,
    lyricsClick: Boolean,
    lyricsTextSize: Float,
    lyricsLineSpacing: Float,
    lyricsLineBlur: Boolean,
    lyricsFontFamily: FontFamily?,
    textColor: Color,
    romanizationPreferences: LyricsRomanizationPreferences,
    onSeek: (Int) -> Unit,
) {
    val context = LocalContext.current
    val parsedDocument = remember(rawLyrics) {
        parseSyncedLyricsDocument(rawLyrics)
    }

    if (parsedDocument.lyrics.lines.isEmpty()) {
        LyricsEmptyState(textColor = textColor)
        return
    }

    var renderedLyrics by remember(parsedDocument.lyrics) {
        mutableStateOf(parsedDocument.lyrics)
    }

    LaunchedEffect(parsedDocument.lyrics, romanizationPreferences) {
        renderedLyrics = applyRomanization(parsedDocument.lyrics, romanizationPreferences)
    }

    val latestPositionProvider = rememberUpdatedState(currentPositionProvider)
    val latestPlayerPositionProvider = rememberUpdatedState(playerPositionProvider)
    var currentPositionMs by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    var isManualScrolling by remember { mutableStateOf(false) }
    var manualScrollPositionMs by remember { mutableIntStateOf(0) }
    var lastManualScrollTimestamp by remember { mutableLongStateOf(0L) }
    var selectionSheetVisible by remember { mutableStateOf(false) }
    var selectionSheetAnchorIndex by remember { mutableIntStateOf(0) }
    val selectedLineIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }
    val selectionLines = renderedLyrics.lines
    val nestedScrollConnection = remember(parsedDocument.isSynced) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (parsedDocument.isSynced && source == NestedScrollSource.UserInput) {
                    manualScrollPositionMs = currentPositionMs
                    isManualScrolling = true
                    lastManualScrollTimestamp = System.currentTimeMillis()
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(parsedDocument.isSynced, lyricsSyncOffset) {
        if (!parsedDocument.isSynced) {
            currentPositionMs = 0
            return@LaunchedEffect
        }

        while (isActive) {
            val position = latestPositionProvider.value() ?: latestPlayerPositionProvider.value()
            val syncedPosition = (position + lyricsSyncOffset)
                .coerceAtLeast(0L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            if (currentPositionMs != syncedPosition) {
                currentPositionMs = syncedPosition
            }
            delay(16L)
        }
    }

    LaunchedEffect(isManualScrolling, lastManualScrollTimestamp, parsedDocument.isSynced) {
        if (!parsedDocument.isSynced || !isManualScrolling) {
            return@LaunchedEffect
        }
        val capturedTimestamp = lastManualScrollTimestamp
        delay(ManualScrollTimeoutMs)
        if (lastManualScrollTimestamp == capturedTimestamp) {
            isManualScrolling = false
        }
    }

    LaunchedEffect(rawLyrics) {
        selectedLineIndices.clear()
        selectionSheetVisible = false
        isManualScrolling = false
    }

    LaunchedEffect(showMaxSelectionToast) {
        if (!showMaxSelectionToast) {
            return@LaunchedEffect
        }
        Toast.makeText(
            context,
            "${context.getString(R.string.max_selection_limit)}: $MaxSelectedLyricsLines",
            Toast.LENGTH_SHORT,
        ).show()
        showMaxSelectionToast = false
    }

    BackHandler(enabled = selectionSheetVisible) {
        selectedLineIndices.clear()
        selectionSheetVisible = false
    }

    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var shareImageDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    val normalTextStyle = rememberLyricsTextStyle(
        lyricsTextSize = lyricsTextSize,
        lyricsLineSpacing = lyricsLineSpacing,
        lyricsFontFamily = lyricsFontFamily,
    )
    val accompanimentTextStyle = rememberLyricsTextStyle(
        lyricsTextSize = lyricsTextSize * 0.68f,
        lyricsLineSpacing = lyricsLineSpacing,
        lyricsFontFamily = lyricsFontFamily,
    )
    val phoneticTextStyle = rememberLyricsTextStyle(
        lyricsTextSize = lyricsTextSize * 0.5f,
        lyricsLineSpacing = 1.1f,
        lyricsFontFamily = lyricsFontFamily,
        fontWeight = FontWeight.Normal,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        KaraokeLyricsView(
            listState = listState,
            lyrics = renderedLyrics,
            currentPosition = { if (isManualScrolling) manualScrollPositionMs else currentPositionMs },
            onLineClicked = { line ->
                if (lyricsClick && parsedDocument.isSynced && line.start > 0) {
                    isManualScrolling = false
                    onSeek(line.start)
                }
            },
            onLinePressed = { line ->
                val selectedIndex = resolveLyricsLineIndex(selectionLines, line)
                if (selectedIndex < 0) {
                    return@KaraokeLyricsView
                }
                selectionSheetAnchorIndex = selectedIndex
                selectedLineIndices.clear()
                selectedLineIndices.add(selectedIndex)
                selectionSheetVisible = true
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            normalLineTextStyle = normalTextStyle,
            accompanimentLineTextStyle = accompanimentTextStyle,
            phoneticTextStyle = phoneticTextStyle,
            textColor = textColor,
            blendMode = BlendMode.SrcOver,
            useBlurEffect = lyricsLineBlur && parsedDocument.isSynced,
            showTranslation = true,
            showPhonetic = romanizationPreferences.isEnabled,
            offset = 56.dp,
            keepAliveZone = 120.dp,
            blurDelta = 3f,
        )

        if (parsedDocument.isSynced && isManualScrolling && !selectionSheetVisible) {
            FilledTonalButton(
                onClick = { isManualScrolling = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            ) {
                Text(text = stringResource(R.string.resume_autoscroll))
            }
        }
    }

    if (selectionSheetVisible) {
        LyricsSelectionSheet(
            lines = selectionLines,
            selectedLineIndices = selectedLineIndices,
            initialIndex = selectionSheetAnchorIndex,
            lyricsTextSize = lyricsTextSize,
            lyricsFontFamily = lyricsFontFamily,
            onDismissRequest = {
                selectedLineIndices.clear()
                selectionSheetVisible = false
            },
            onToggleLine = { index ->
                if (selectedLineIndices.remove(index)) {
                    if (selectedLineIndices.isEmpty()) {
                        selectionSheetVisible = false
                    }
                } else if (selectedLineIndices.size < MaxSelectedLyricsLines) {
                    selectedLineIndices.add(index)
                } else {
                    showMaxSelectionToast = true
                }
            },
            onShare = {
                val payloadText = selectedLineIndices
                    .sorted()
                    .mapNotNull { selectionLines.getOrNull(it)?.let(::lineTextWithTranslation) }
                    .filter(String::isNotBlank)
                    .joinToString("\n")
                val metadata = mediaMetadata
                if (payloadText.isNotBlank() && metadata != null) {
                    shareDialogData = Triple(
                        payloadText,
                        metadata.title,
                        metadata.artists.joinToString { it.name },
                    )
                }
                selectedLineIndices.clear()
                selectionSheetVisible = false
            },
        )
    }

    val shareData = shareDialogData
    if (shareData != null) {
        LyricsShareChoiceDialog(
            payload = shareData,
            songId = mediaMetadata?.id,
            onDismiss = { shareDialogData = null },
            onShareImage = {
                shareImageDialogData = shareData
                shareDialogData = null
            },
        )
    }

    val imageShareData = shareImageDialogData
    if (imageShareData != null && mediaMetadata != null) {
        val (lyricsText, songTitle, artists) = imageShareData
        LyricsShareImageDialog(
            mediaMetadata = mediaMetadata,
            payload = LyricsSharePayload(lyricsText, songTitle, artists),
            onDismissRequest = { shareImageDialogData = null },
        )
    }
}

@Composable
private fun rememberLyricsTextStyle(
    lyricsTextSize: Float,
    lyricsLineSpacing: Float,
    lyricsFontFamily: FontFamily?,
    fontWeight: FontWeight = FontWeight.ExtraBold,
): TextStyle =
    MaterialTheme.typography.headlineMedium.copy(
        fontSize = lyricsTextSize.sp,
        lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
        fontWeight = fontWeight,
        fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
        textMotion = TextMotion.Animated,
    )

private fun resolveLyricsLineIndex(
    lines: List<ISyncedLine>,
    target: ISyncedLine,
): Int {
    val identityMatch = lines.indexOfFirst { it === target }
    if (identityMatch >= 0) {
        return identityMatch
    }

    val targetText = lineTextWithTranslation(target)
    return lines.indexOfFirst { candidate ->
        candidate.start == target.start &&
            candidate.end == target.end &&
            lineTextWithTranslation(candidate) == targetText
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsSelectionSheet(
    lines: List<ISyncedLine>,
    selectedLineIndices: List<Int>,
    initialIndex: Int,
    lyricsTextSize: Float,
    lyricsFontFamily: FontFamily?,
    onDismissRequest: () -> Unit,
    onToggleLine: (Int) -> Unit,
    onShare: () -> Unit,
) {
    val listState = rememberLazyListState()
    val selectedIndices = selectedLineIndices.toSet()

    LaunchedEffect(lines, initialIndex) {
        if (lines.isNotEmpty()) {
            listState.scrollToItem(initialIndex.coerceIn(0, lines.lastIndex))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.share_selected),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.n_element,
                            selectedLineIndices.size,
                            selectedLineIndices.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.close))
                }
            }

            HorizontalDivider()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
            ) {
                itemsIndexed(
                    items = lines,
                    key = { index, line -> "${line.start}_${line.end}_$index" },
                ) { index, line ->
                    val isSelected = index in selectedIndices
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .background(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                shape = RoundedCornerShape(24.dp),
                            )
                            .clickable { onToggleLine(index) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = lineTextWithTranslation(line),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = (lyricsTextSize * 0.72f).sp,
                                    lineHeight = (lyricsTextSize * 0.98f).sp,
                                    fontFamily = lyricsFontFamily
                                        ?: MaterialTheme.typography.bodyLarge.fontFamily,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            Box(
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .size(12.dp)
                                    .background(
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    enabled = selectedLineIndices.isNotEmpty(),
                    onClick = onShare,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.share_selected))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsShareChoiceDialog(
    payload: Triple<String, String, String>,
    songId: String?,
    onDismiss: () -> Unit,
    onShareImage: () -> Unit,
) {
    val context = LocalContext.current
    val (lyricsText, songTitle, artists) = payload

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.85f),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.share_lyrics),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))

                LyricsShareAction(
                    text = stringResource(R.string.share_as_text),
                    onClick = {
                        shareLyricsAsText(
                            context = context,
                            payload = LyricsSharePayload(lyricsText, songTitle, artists),
                            songId = songId,
                        )
                        onDismiss()
                    },
                )

                LyricsShareAction(
                    text = stringResource(R.string.share_as_image),
                    onClick = onShareImage,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsShareAction(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.share),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
