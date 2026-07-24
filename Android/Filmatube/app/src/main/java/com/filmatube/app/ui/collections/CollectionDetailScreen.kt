package com.filmatube.app.ui.collections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.ui.components.EmptyView
import com.filmatube.app.ui.components.MoviePosterTile
import com.filmatube.app.ui.theme.FilmatubeBrandGreen
import com.filmatube.app.ui.theme.FilmatubeBrandGreenDeep
import com.filmatube.app.ui.theme.FilmatubeSpacing

/** A collection's movies — the Android view/editor for `/collections/[id]`. Owners can edit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    viewModel: CollectionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = com.filmatube.app.util.LocaleController.currentTag()

    var menuOpen by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    var addingMovies by rememberSaveable { mutableStateOf(false) }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setCover(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.title.ifBlank { stringResource(R.string.library_collections) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                actions = {
                    if (state.isOwner) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.movie_options_title))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.collection_rename)) },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = { menuOpen = false; renaming = true },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(if (state.isPublic) R.string.collection_make_private else R.string.collection_make_public))
                                },
                                leadingIcon = {
                                    Icon(if (state.isPublic) Icons.Outlined.Lock else Icons.Outlined.Public, contentDescription = null)
                                },
                                onClick = { menuOpen = false; viewModel.setPublic(!state.isPublic) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.collection_change_cover)) },
                                leadingIcon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.collection_delete)) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                onClick = { menuOpen = false; confirmingDelete = true },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.isOwner) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.collection_add_movies)) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = { addingMovies = true },
                )
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(FilmatubeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CollectionHeader(
                    title = state.title,
                    coverUrl = state.coverUrl,
                    isPublic = state.isPublic,
                    isOwner = state.isOwner,
                    uploading = state.coverUploading,
                    movieCount = state.movies.size,
                    onChangeCover = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                )
            }

            if (state.movies.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyView(
                        icon = Icons.Outlined.Layers,
                        title = stringResource(R.string.library_collections),
                        message = stringResource(R.string.collection_empty),
                    )
                }
            } else {
                items(state.movies, key = { it.id }) { movie ->
                    Box {
                        MoviePosterTile(movie = movie, language = language, width = null, onClick = { onMovieClick(movie.id) })
                        if (state.isOwner) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { viewModel.removeMovie(movie.id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.collection_remove), tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (renaming) {
        RenameDialog(initial = state.title, onDismiss = { renaming = false }, onConfirm = { viewModel.rename(it); renaming = false })
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.collection_delete)) },
            text = { Text(stringResource(R.string.collection_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; viewModel.delete(onDeleted = onBack) }) {
                    Text(stringResource(R.string.collection_delete))
                }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (addingMovies) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { addingMovies = false; viewModel.clearSearch() }, sheetState = sheetState) {
            AddMoviesSheet(
                query = state.searchQuery,
                results = state.searchResults,
                inCollection = state.movies.map { it.id }.toSet(),
                language = language,
                onQueryChange = viewModel::onSearchQueryChange,
                onAdd = viewModel::addMovie,
            )
        }
    }
}

@Composable
private fun CollectionHeader(
    title: String,
    coverUrl: String,
    isPublic: Boolean,
    isOwner: Boolean,
    uploading: Boolean,
    movieCount: Int,
    onChangeCover: () -> Unit,
) {
    // Image with the title to its right — the app's page-header pattern.
    Row(
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .width(150.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .then(if (isOwner) Modifier.clickable(onClick = onChangeCover) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (coverUrl.isNotBlank()) {
                AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(FilmatubeBrandGreen, FilmatubeBrandGreenDeep))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Layers, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(32.dp))
                }
            }
            if (uploading) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                }
            } else if (isOwner) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(FilmatubeSpacing.xs).size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = stringResource(R.string.collection_change_cover), tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                stringResource(R.string.collection_eyebrow).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                title.ifBlank { stringResource(R.string.collection_untitled) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                Icon(
                    if (isPublic) Icons.Outlined.Public else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    stringResource(if (isPublic) R.string.collection_public else R.string.collection_private) +
                        "  •  " + stringResource(R.string.collection_movie_count, movieCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.collection_rename)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(R.string.collection_title_label)) },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun AddMoviesSheet(
    query: String,
    results: List<Movie>,
    inCollection: Set<String>,
    language: String,
    onQueryChange: (String) -> Unit,
    onAdd: (Movie) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = FilmatubeSpacing.lg).padding(bottom = FilmatubeSpacing.xl)) {
        Text(
            stringResource(R.string.collection_add_movies),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = FilmatubeSpacing.sm),
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.collection_search_hint)) },
        )
        Column(
            modifier = Modifier.fillMaxWidth().height(320.dp).padding(top = FilmatubeSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                lazyItems(results, key = { it.id }) { movie ->
                    val added = movie.id in inCollection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !added) { onAdd(movie) }
                            .padding(FilmatubeSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                    ) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(width = 36.dp, height = 54.dp).clip(RoundedCornerShape(6.dp)),
                        )
                        Text(movie.title.get(language), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(
                            if (added) Icons.Outlined.Close else Icons.Filled.Add,
                            contentDescription = null,
                            tint = if (added) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
