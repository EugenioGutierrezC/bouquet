package com.rizzi.bouquet

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.rizzi.bouquet.network.getDownloadInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt


@Composable
fun VerticalPDFReaderZoomable(
    state: VerticalPdfReaderState,
    modifier: Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        val ctx = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val lazyState = state.lazyState
        DisposableEffect(key1 = Unit) {
            load(
                coroutineScope,
                ctx,
                state,
                constraints.maxWidth,
                constraints.maxHeight,
                true
            )
            onDispose {
                state.close()
            }
        }
        state.pdfRender?.let { pdf ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .toZoom(state),
                horizontalAlignment = Alignment.CenterHorizontally,
                state = lazyState
            ) {
                items(pdf.pageCount) {
                    val pageContent = pdf.pageLists[it].stateFlow.collectAsState().value
                    DisposableEffect(key1 = Unit) {
                        pdf.pageLists[it].load()
                        onDispose {
                            pdf.pageLists[it].recycle()
                        }
                    }
                    when (pageContent) {
                        is PageContentInt.PageContent -> {
                            PdfImage(
                                bitmap = { pageContent.bitmap.asImageBitmap() },
                                contentDescription = pageContent.contentDescription
                            )
                        }

                        is PageContentInt.BlankPage -> BlackPage(
                            width = pageContent.width,
                            height = pageContent.height
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalPDFReader(
    state: VerticalPdfReaderState,
    modifier: Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        val ctx = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val lazyState = state.lazyState
        DisposableEffect(key1 = Unit) {
            load(
                coroutineScope,
                ctx,
                state,
                constraints.maxWidth,
                constraints.maxHeight,
                true
            )
            onDispose {
                state.close()
            }
        }
        state.pdfRender?.let { pdf ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .tapToZoomVertical(state, constraints),
                horizontalAlignment = Alignment.CenterHorizontally,
                state = lazyState
            ) {
                items(pdf.pageCount) {
                    val pageContent = pdf.pageLists[it].stateFlow.collectAsState().value
                    DisposableEffect(key1 = Unit) {
                        pdf.pageLists[it].load()
                        onDispose {
                            pdf.pageLists[it].recycle()
                        }
                    }
                    when (pageContent) {
                        is PageContentInt.PageContent -> {
                            PdfImage(
                                bitmap = { pageContent.bitmap.asImageBitmap() },
                                contentDescription = pageContent.contentDescription
                            )
                        }

                        is PageContentInt.BlankPage -> BlackPage(
                            width = pageContent.width,
                            height = pageContent.height
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Int.dp(): Dp {
    val density = LocalDensity.current.density
    return (this / density).dp
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun HorizontalPDFReader(
    state: HorizontalPdfReaderState,
    modifier: Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        val ctx = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        DisposableEffect(key1 = Unit) {
            load(
                coroutineScope,
                ctx,
                state,
                constraints.maxWidth,
                constraints.maxHeight,
                constraints.maxHeight > constraints.maxWidth
            )
            onDispose {
                state.close()
            }
        }
        state.pdfRender?.let { pdf ->
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .tapToZoomHorizontal(state, constraints),
                count = state.pdfPageCount,
                state = state.pagerState,
                userScrollEnabled = state.scale == 1f
            ) { page ->
                val pageContent = pdf.pageLists[page].stateFlow.collectAsState().value
                DisposableEffect(key1 = Unit) {
                    pdf.pageLists[page].load()
                    onDispose {
                        pdf.pageLists[page].recycle()
                    }
                }
                when (pageContent) {
                    is PageContentInt.PageContent -> {
                        PdfImage(
                            bitmap = { pageContent.bitmap.asImageBitmap() },
                            contentDescription = pageContent.contentDescription
                        )
                    }

                    is PageContentInt.BlankPage -> BlackPage(
                        width = pageContent.width,
                        height = pageContent.height
                    )
                }
            }
        }
    }
}

private fun load(
    coroutineScope: CoroutineScope,
    context: Context,
    state: PdfReaderState,
    width: Int,
    height: Int,
    portrait: Boolean
) {
    runCatching {
        if (state.isLoaded) {
            coroutineScope.launch(Dispatchers.IO) {
                runCatching {
                    val pFD =
                        ParcelFileDescriptor.open(state.mFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val textForEachPage =
                        if (state.isAccessibleEnable) getTextByPage(context, pFD) else emptyList()
                    state.pdfRender =
                        BouquetPdfRender(pFD, textForEachPage, width, height, portrait)
                }.onFailure {
                    state.mError = it
                }
            }
        } else {
            when (val res = state.resource) {
                is ResourceType.Local -> {
                    coroutineScope.launch(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openFileDescriptor(res.uri, "r")?.let {
                                val textForEachPage = if (state.isAccessibleEnable) {
                                    getTextByPage(context, it)
                                } else emptyList()
                                state.pdfRender =
                                    BouquetPdfRender(it, textForEachPage, width, height, portrait)
                                state.mFile = context.uriToFile(res.uri)
                            } ?: throw IOException("File not found")
                        }.onFailure {
                            state.mError = it
                        }
                    }
                }

                is ResourceType.Remote -> {
                    coroutineScope.launch(Dispatchers.IO) {
                        runCatching {
                            val bufferSize = 8192
                            var downloaded = 0
                            val file = File(context.cacheDir, generateFileName())
                            val response = getDownloadInterface(
                                res.headers
                            ).downloadFile(
                                res.url
                            )
                            val byteStream = response.byteStream()
                            byteStream.use { input ->
                                file.outputStream().use { output ->
                                    val totalBytes = response.contentLength()
                                    var data = ByteArray(bufferSize)
                                    var count = input.read(data)
                                    while (count != -1) {
                                        if (totalBytes > 0) {
                                            downloaded += bufferSize
                                            state.mLoadPercent =
                                                (downloaded * (100 / totalBytes.toFloat())).toInt()
                                        }
                                        output.write(data, 0, count)
                                        data = ByteArray(bufferSize)
                                        count = input.read(data)
                                    }
                                }
                            }
                            val pFD = ParcelFileDescriptor.open(
                                file,
                                ParcelFileDescriptor.MODE_READ_ONLY
                            )
                            val textForEachPage = if (state.isAccessibleEnable) {
                                getTextByPage(context, pFD)
                            } else emptyList()
                            state.pdfRender =
                                BouquetPdfRender(pFD, textForEachPage, width, height, portrait)
                            state.mFile = file
                        }.onFailure {
                            state.mError = it
                        }
                    }
                }

                is ResourceType.Base64 -> {
                    coroutineScope.launch(Dispatchers.IO) {
                        runCatching {
                            val file = context.base64ToPdf(res.file)
                            val pFD = ParcelFileDescriptor.open(
                                file,
                                ParcelFileDescriptor.MODE_READ_ONLY
                            )
                            val textForEachPage = if (state.isAccessibleEnable) {
                                getTextByPage(context, pFD)
                            } else emptyList()
                            state.pdfRender =
                                BouquetPdfRender(pFD, textForEachPage, width, height, portrait)
                            state.mFile = file
                        }.onFailure {
                            state.mError = it
                        }
                    }
                }

                is ResourceType.Asset -> {
                    coroutineScope.launch(Dispatchers.IO) {
                        runCatching {
                            val bufferSize = 8192
                            val inputStream = context.resources.openRawResource(res.assetId)
                            val outFile = File(context.cacheDir, generateFileName())
                            inputStream.use { input ->
                                outFile.outputStream().use { output ->
                                    var data = ByteArray(bufferSize)
                                    var count = input.read(data)
                                    while (count != -1) {
                                        output.write(data, 0, count)
                                        data = ByteArray(bufferSize)
                                        count = input.read(data)
                                    }
                                }
                            }
                            val pFD = ParcelFileDescriptor.open(
                                outFile,
                                ParcelFileDescriptor.MODE_READ_ONLY
                            )
                            val textForEachPage = if (state.isAccessibleEnable) {
                                getTextByPage(context, pFD)
                            } else emptyList()
                            state.pdfRender =
                                BouquetPdfRender(pFD, textForEachPage, width, height, portrait)
                            state.mFile = outFile
                        }.onFailure {
                            state.mError = it
                        }
                    }
                }
            }
        }
    }.onFailure {
        state.mError = it
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tapToZoomVertical(
    state: VerticalPdfReaderState,
    constraints: Constraints
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "verticalTapToZoom"
        properties["state"] = state
    }
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    this
        .combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { },
            onDoubleClick = {
                if (scale >= 2f) {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                } else {
                    scale = 3f
                }
            },
        )
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown()
                do {
                    val event = awaitPointerEvent()
                    scale *= event.calculateZoom()
                    if (scale > 1) {
                        val offset = event.calculatePan()
                        offsetX += offset.x
                        offsetY += offset.y
                    } else {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                } while (event.changes.any { it.pressed })
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offsetX
            translationY = offsetY
        }
}

fun Modifier.tapToZoomHorizontal(
    state: HorizontalPdfReaderState,
    constraints: Constraints
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "horizontalTapToZoom"
        properties["state"] = state
    }
) {
    this
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { tapCenter ->
                    if (!state.isZoomEnable) return@detectTapGestures
                    if (state.mScale > 1.0f) {
                        state.mScale = 1.0f
                        state.offset = Offset(0f, 0f)
                    } else {
                        state.mScale = 3.0f
                        val center = Pair(constraints.maxWidth / 2, constraints.maxHeight / 2)
                        val xDiff = (tapCenter.x - center.first) * state.scale
                        val yDiff = ((tapCenter.y - center.second) * state.scale).coerceIn(
                            minimumValue = -(center.second * 2f),
                            maximumValue = (center.second * 2f)
                        )
                        state.offset = Offset(-xDiff, -yDiff)
                    }
                }
            )
        }
        .pointerInput(Unit) {
            detectTransformGestures(false) { centroid, pan, zoom, rotation ->
                state.mScale = max(state.mScale * zoom, 1f)
                val nOffset = if (state.scale > 1f) {
                    val maxT = (constraints.maxWidth * state.scale) - constraints.maxWidth
                    val maxY = (constraints.maxHeight * state.scale) - constraints.maxHeight
                    Offset(
                        x = (state.offset.x + pan.x).coerceIn(
                            minimumValue = (-maxT / 2) * 1.3f,
                            maximumValue = (maxT / 2) * 1.3f
                        ),
                        y = (state.offset.y + pan.y).coerceIn(
                            minimumValue = (-maxY / 2) * 1.3f,
                            maximumValue = (maxY / 2) * 1.3f
                        )
                    )
                } else {
                    Offset(0f, 0f)
                }
                state.offset = nOffset
            }
        }
        .graphicsLayer {
            scaleX = state.scale
            scaleY = state.scale
            translationX = state.offset.x
            translationY = state.offset.y
        }
}

fun Modifier.toZoom(
    state: VerticalPdfReaderState,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "horizontalTapToZoom"
        properties["state"] = state
    }
) {
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transState = rememberTransformableState(onTransformation = { zoomChange, panChange, rotationChange ->
        scale = max(scale * zoomChange, 1f)
        rotation += 0
        offset += panChange
    })

    this
        .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
        .transformable(transState, lockRotationOnZoomPan = true , enabled = true)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationZ = 0f
        }
}

@Composable
fun BlackPage(
    width: Int,
    height: Int
) {
    Box(
        modifier = Modifier
            .size(
                width = width.dp(),
                height = height.dp()
            )
            .background(color = Color.White)
    )
}
