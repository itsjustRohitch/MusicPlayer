package com.example.musicplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private val songViewModel: SongViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicPlayerTheme {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                val audioPermissionState = rememberPermissionState(permission)
                if (audioPermissionState.status.isGranted) {
                    songViewModel.loadSongs()
                    MusicPlayerApp(songViewModel, audioPermissionState)
                } else {
                    PermissionRequestScreen { audioPermissionState.launchPermissionRequest() }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicPlayerApp(songViewModel: SongViewModel, audioPermissionState: PermissionState) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Songs, Screen.Albums, Screen.Folders, Screen.Playlists)

    if (audioPermissionState.status.isGranted) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = Screen.Songs.route, Modifier.padding(innerPadding)) {
                composable(Screen.Songs.route) { SongListScreen(navController, songViewModel) }
                composable(Screen.Albums.route) { AlbumListScreen(navController, songViewModel) }
                composable(Screen.Folders.route) { FolderListScreen(navController, songViewModel) }
                composable(Screen.Playlists.route) { PlaylistListScreen(navController, songViewModel) }

                composable(
                    "album_detail/{albumId}/{albumTitle}",
                    arguments = listOf(navArgument("albumId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getLong("albumId")!!
                    val albumTitle = URLDecoder.decode(backStackEntry.arguments?.getString("albumTitle"), StandardCharsets.UTF_8.toString())
                    AlbumDetailScreen(navController, songViewModel, albumId, albumTitle)
                }
                composable(
                    "folder_detail/{folderPath}/{folderName}",
                ) { backStackEntry ->
                    val folderPath = URLDecoder.decode(backStackEntry.arguments?.getString("folderPath"), StandardCharsets.UTF_8.toString())
                    val folderName = URLDecoder.decode(backStackEntry.arguments?.getString("folderName"), StandardCharsets.UTF_8.toString())
                    FolderDetailScreen(navController, songViewModel, folderPath, folderName)
                }
                composable(
                    "playlist_detail/{playlistId}/{playlistName}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getLong("playlistId")!!
                    val playlistName = URLDecoder.decode(backStackEntry.arguments?.getString("playlistName"), StandardCharsets.UTF_8.toString())
                    PlaylistDetailScreen(navController, songViewModel, playlistId, playlistName)
                }
                composable("player_screen") { PlayerScreen(songViewModel) }
            }
        }
    } else {
        PermissionRequestScreen { audioPermissionState.launchPermissionRequest() }
    }
}

@Composable
fun PermissionRequestScreen(onPermissionGranted: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Permission required to access your music.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onPermissionGranted) { Text("Grant Permission") }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value = query, onValueChange = onQueryChange, label = { Text("Search") }, modifier = modifier.fillMaxWidth().padding(16.dp), singleLine = true)
}

@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    // We add an optional slot for a trailing button (like "Add" or "Remove")
    trailingContent: (@Composable () -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            AsyncImage(model = song.albumArtUri, contentDescription = "Album Art", modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
        },
        trailingContent = trailingContent,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// A dialog that shows a list of playlists to add a song to
@Composable
fun AddToPlaylistDialog(
    song: Song,
    songViewModel: SongViewModel,
    onDismiss: () -> Unit
) {
    val playlists by songViewModel.playlists.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add to playlist", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(playlists) { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name) },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null) },
                            modifier = Modifier.clickable {
                                songViewModel.addSongToPlaylist(playlist.id, song)
                                onDismiss()
                            }
                        )
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("CANCEL")
                }
            }
        }
    }
}


@Composable
fun SongListScreen(navController: NavController, songViewModel: SongViewModel) {
    val filteredSongs by songViewModel.filteredSongs.collectAsStateWithLifecycle()
    val searchQuery by songViewModel.searchQuery.collectAsStateWithLifecycle()

    // State to manage which song (if any) is being added to a playlist
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    // If a song is selected, show the "Add to Playlist" dialog
    songToAddToPlaylist?.let { song ->
        AddToPlaylistDialog(
            song = song,
            songViewModel = songViewModel,
            onDismiss = { songToAddToPlaylist = null }
        )
    }

    Column(Modifier.fillMaxSize()) {
        SearchBar(query = searchQuery, onQueryChange = { songViewModel.onSearchQueryChanged(it) })
        LazyColumn {
            items(filteredSongs) { song ->
                SongItem(
                    song = song,
                    onClick = {
                        songViewModel.setPlayQueue(filteredSongs, filteredSongs.indexOf(song))
                        navController.navigate("player_screen")
                    },
                    // Add a trailing button to add this song to a playlist
                    trailingContent = {
                        IconButton(onClick = { songToAddToPlaylist = song }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AlbumListScreen(navController: NavController, songViewModel: SongViewModel) {
    val filteredAlbums by songViewModel.filteredAlbums.collectAsStateWithLifecycle()
    val searchQuery by songViewModel.searchQuery.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SearchBar(query = searchQuery, onQueryChange = { songViewModel.onSearchQueryChanged(it) })
        LazyVerticalGrid(columns = GridCells.Adaptive(128.dp), modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredAlbums) { album ->
                Card(modifier = Modifier.clickable { navController.navigate("album_detail/${album.id}/${URLEncoder.encode(album.title, StandardCharsets.UTF_8.toString())}") }) {
                    Column {
                        AsyncImage(model = album.albumArtUri, contentDescription = "Album Art", modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                        Text(album.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp))
                        Text(album.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FolderListScreen(navController: NavController, songViewModel: SongViewModel) {
    val filteredFolders by songViewModel.filteredFolders.collectAsStateWithLifecycle()
    val searchQuery by songViewModel.searchQuery.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SearchBar(query = searchQuery, onQueryChange = { songViewModel.onSearchQueryChanged(it) })
        LazyColumn {
            items(filteredFolders) { folder ->
                ListItem(
                    // --- FIX 1: "Shabby Naming" ---
                    // We display `folder.path` (the short name) instead of `folder.name` (the full path)
                    // This assumes the view model is creating folders as MusicFolder(name = fullPath, path = shortName)
                    // which seems to be the case from your screenshot.
                    headlineContent = { Text(folder.path) },
                    leadingContent = { Icon(Icons.Default.Folder, null) },
                    modifier = Modifier.clickable {
                        // --- FIX 2: "Shabby Naming" (Navigation) ---
                        // We pass the full path (folder.name) as the path, and the short name (folder.path) as the name.
                        navController.navigate("folder_detail/${URLEncoder.encode(folder.name, StandardCharsets.UTF_8.toString())}/${URLEncoder.encode(folder.path, StandardCharsets.UTF_8.toString())}")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(navController: NavController, songViewModel: SongViewModel) {
    val playlists by songViewModel.playlists.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Playlist")
            }
        }
    ) {paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn {
                items(playlists) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null) },
                        modifier = Modifier.clickable { navController.navigate("playlist_detail/${playlist.id}/${URLEncoder.encode(playlist.name, StandardCharsets.UTF_8.toString())}") }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Create new playlist") },
            text = { OutlinedTextField(value = playlistName, onValueChange = { playlistName = it }, label = { Text("Playlist Name") }) },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            songViewModel.createPlaylist(playlistName)
                            playlistName = ""
                            showDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = { Button(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AlbumDetailScreen(navController: NavController, songViewModel: SongViewModel, albumId: Long, albumTitle: String) {
    val songs = songViewModel.getSongsForAlbum(albumId)

    // State to manage which song (if any) is being added to a playlist
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    songToAddToPlaylist?.let { song ->
        AddToPlaylistDialog(
            song = song,
            songViewModel = songViewModel,
            onDismiss = { songToAddToPlaylist = null }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Text(albumTitle, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        LazyColumn {
            items(songs) { song ->
                SongItem(
                    song = song,
                    onClick = {
                        songViewModel.setPlayQueue(songs, songs.indexOf(song))
                        navController.navigate("player_screen")
                    },
                    // Add a trailing button to add this song to a playlist
                    trailingContent = {
                        IconButton(onClick = { songToAddToPlaylist = song }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun FolderDetailScreen(navController: NavController, songViewModel: SongViewModel, folderPath: String, folderName: String) {
    // --- FIX 3: "Empty Folders" ---

    // 1. Get songs that are *directly* in this folder
    val songs = songViewModel.getSongsForFolder(folderPath)

    // 2. Get *all* folders from the view model
    val allFolders by songViewModel.filteredFolders.collectAsStateWithLifecycle()

    // 3. Find *direct* sub-folders of the current folder
    val subFolders = allFolders.filter { folder ->
        // A sub-folder's name (full path) must start with the current folder's path + "/"
        folder.name.startsWith(folderPath + "/") &&
                // We only want direct children, so we check if there are any *more* slashes
                // e.g., "THEMES/MARVEL" is a child, but "THEMES/MARVEL/POSTERS" is not
                !folder.name.substringAfter(folderPath + "/").contains('/')
    }

    // State to manage the "Add to Playlist" dialog
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    songToAddToPlaylist?.let { song ->
        AddToPlaylistDialog(
            song = song,
            songViewModel = songViewModel,
            onDismiss = { songToAddToPlaylist = null }
        )
    }

    Column(Modifier.fillMaxSize()) {
        // Display the short, clean name we passed in
        Text(folderName, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        LazyColumn {
            // 4. List all the sub-folders first
            items(subFolders) { subFolder ->
                ListItem(
                    // Display the sub-folder's short name
                    headlineContent = { Text(subFolder.path) },
                    leadingContent = { Icon(Icons.Default.Folder, null) },
                    modifier = Modifier.clickable {
                        // Allow navigating deeper into sub-folders
                        navController.navigate("folder_detail/${URLEncoder.encode(subFolder.name, StandardCharsets.UTF_8.toString())}/${URLEncoder.encode(subFolder.path, StandardCharsets.UTF_8.toString())}")
                    }
                )
            }

            // 5. Add a divider if we have both sub-folders and songs
            if (subFolders.isNotEmpty() && songs.isNotEmpty()) {
                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            }

            // 6. List all the songs in this folder
            items(songs) { song ->
                SongItem(
                    song = song,
                    onClick = {
                        songViewModel.setPlayQueue(songs, songs.indexOf(song))
                        navController.navigate("player_screen")
                    },
                    // Add a trailing button to add this song to a playlist
                    trailingContent = {
                        IconButton(onClick = { songToAddToPlaylist = song }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                        }
                    }
                )
            }

            // 7. Show a message if the folder is truly empty
            if (subFolders.isEmpty() && songs.isEmpty()) {
                item {
                    Text(
                        "This folder is empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        // --- HERE IS THE FIX ---
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        // --- END FIX ---
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(navController: NavController, songViewModel: SongViewModel, playlistId: Long, playlistName: String) {
    val songs = songViewModel.getSongsForPlaylist(playlistId)
    Column(Modifier.fillMaxSize()) {
        Text(playlistName, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        LazyColumn {
            items(songs) { song ->
                SongItem(
                    song = song,
                    onClick = {
                        songViewModel.setPlayQueue(songs, songs.indexOf(song))
                        navController.navigate("player_screen")
                    },
                    // Add a trailing button to REMOVE this song from the playlist
                    trailingContent = {
                        IconButton(onClick = {
                            songViewModel.removeSongFromPlaylist(playlistId, song)
                        }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove from playlist")
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
