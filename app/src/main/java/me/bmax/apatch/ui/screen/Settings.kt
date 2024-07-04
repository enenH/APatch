package me.bmax.apatch.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.APApplication.Companion.sharedPreferences
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.AboutDialog
import me.bmax.apatch.ui.component.LoadingDialog
import me.bmax.apatch.ui.component.SwitchItem
import me.bmax.apatch.util.LocalDialogHost
import me.bmax.apatch.util.createRootShell
import me.bmax.apatch.util.getBugreportFile
import me.bmax.apatch.util.isGlobalNamespaceEnabled
import me.bmax.apatch.util.setGlobalNamespaceEnabled
import me.bmax.apatch.util.setRootShell
import java.util.Locale


var appDraw: Boolean = false
var antiRecord: Boolean = true
var recordBlack: Boolean = false

@Destination
@Composable
fun SettingScreen(navigator: DestinationsNavigator) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady = (state == APApplication.State.ANDROIDPATCH_INSTALLING ||
            state == APApplication.State.ANDROIDPATCH_INSTALLED ||
            state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)
    var isGlobalNamespaceEnabled by rememberSaveable {
        mutableStateOf(false)
    }
    if (kPatchReady && aPatchReady) {
        isGlobalNamespaceEnabled = isGlobalNamespaceEnabled()
    }
    Scaffold(
        topBar = {
            TopBar(onBack = {
                navigator.popBackStack()
            })
        }
    ) { paddingValues ->
        LoadingDialog()

        val showAboutDialog = remember { mutableStateOf(false) }
        AboutDialog(showAboutDialog)

       /* val showLanguageDialog = rememberSaveable { mutableStateOf(false) }
        LanguageDialog(showLanguageDialog)*/

        val showSuDialog = rememberSaveable { mutableStateOf(false) }
        if (showSuDialog.value) {
            CustomSuDialog(showSuDialog)
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
        ) {

            /*val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val dialogHost = LocalDialogHost.current

            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.BugReport,
                        stringResource(id = R.string.send_log)
                    )
                },
                headlineContent = { Text(stringResource(id = R.string.send_log)) },
                modifier = Modifier.clickable {
                    scope.launch {
                        val bugreport = dialogHost.withLoading {
                            withContext(Dispatchers.IO) {
                                getBugreportFile(context)
                            }
                        }
                        val uri: Uri =
                            FileProvider.getUriForFile(
                                context,
                                "${BuildConfig.APPLICATION_ID}.fileprovider",
                                bugreport
                            )
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                        shareIntent.setDataAndType(uri, "application/zip")
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                context.getString(R.string.send_log)
                            )
                        )
                    }
                }
            )*/

            var anti_record by rememberSaveable {
                mutableStateOf(
                    sharedPreferences.getBoolean("anti_record", true)
                )
            }

            SwitchItem(
                icon = Icons.Filled.HideImage,
                title = stringResource(id = R.string.anti_record),
                summary = stringResource(id = R.string.anti_record_summary),
                checked = anti_record
            ) {
                sharedPreferences.edit().putBoolean("anti_record", it).apply()
                anti_record = it
                antiRecord = it
            }

            antiRecord = anti_record
            var record_black by rememberSaveable {
                mutableStateOf(
                    sharedPreferences.getBoolean("record_black", false)
                )
            }

            SwitchItem(
                icon = Icons.Filled.HideSource,
                title = stringResource(id = R.string.record_black),
                summary = stringResource(id = R.string.record_black_summary),
                checked = record_black
            ) {
                sharedPreferences.edit().putBoolean("record_black", it).apply()
                record_black = it
                recordBlack = it
            }
            recordBlack = record_black

            var app_draw by rememberSaveable {
                mutableStateOf(
                    sharedPreferences.getBoolean("app_draw", false)
                )
            }

            SwitchItem(
                icon = Icons.Filled.Draw,
                title = stringResource(id = R.string.app_draw),
                summary = stringResource(id = R.string.app_draw_summary),
                checked = app_draw
            ) {
                sharedPreferences.edit().putBoolean("app_draw", it).apply()
                app_draw = it
                appDraw = it
            }
            appDraw = app_draw
            /*ListItem(
                headlineContent = {
                    Text(text = stringResource(id = R.string.settings_app_language))
                },
                modifier = Modifier.clickable {
                    showLanguageDialog.value = true
                },
                supportingContent = {
                    Text(
                        text = AppCompatDelegate.getApplicationLocales()[0]?.displayLanguage?.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.getDefault()
                            ) else it.toString()
                        } ?: stringResource(id = R.string.system_default)
                    )
                },
                leadingContent = { Icon(Icons.Filled.Translate, null) }
            )*/
            ListItem(
                headlineContent = {
                    Text(text = stringResource(id = R.string.custom_su_cmd))
                },
                modifier = Modifier.clickable {
                    showSuDialog.value = true
                },
                leadingContent = { Icon(Icons.Filled.Code, null) }
            )
            /* if (kPatchReady && aPatchReady) {
                 SwitchItem(
                     icon = Icons.Filled.Engineering,
                     title = stringResource(id = R.string.settings_global_namespace_mode),
                     summary = stringResource(id = R.string.settings_global_namespace_mode_summary),
                     checked = isGlobalNamespaceEnabled,
                     onCheckedChange = {
                         setGlobalNamespaceEnabled(
                             if (isGlobalNamespaceEnabled) {
                                 "0"
                             } else {
                                 "1"
                             }
                         )
                         isGlobalNamespaceEnabled = it
                     }
                 )
             }*/

            val about = stringResource(id = R.string.about)
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.ContactPage,
                        stringResource(id = R.string.about)
                    )
                },
                headlineContent = { Text(about) },
                modifier = Modifier.clickable {
                    showAboutDialog.value = true
                }
            )
        }
    }
}

@Composable
fun LanguageDialog(showLanguageDialog: MutableState<Boolean>) {

    val languages = stringArrayResource(id = R.array.languages)
    val languagesValues = stringArrayResource(id = R.array.languages_values)

    if (showLanguageDialog.value) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog.value = false },
            text = {
                LazyColumn {
                    itemsIndexed(languages) { index, item ->
                        ListItem(
                            headlineContent = { Text(item) },
                            modifier = Modifier.clickable {
                                showLanguageDialog.value = false
                                if (index == 0) {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.getEmptyLocaleList()
                                    )
                                } else {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(
                                            languagesValues[index]
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@Composable
fun CustomSuDialog(showDialog: MutableState<Boolean>) {
    var key by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var enable by remember { mutableStateOf(false) }
    val context = LocalContext.current as Activity
    AlertDialog(
        onDismissRequest = { showDialog.value = false },
        title = { Text(stringResource(id = R.string.home_auth_key_title)) },
        text = {
            Column {
                Text(stringResource(id = R.string.home_auth_key_desc))
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    TextField(
                        value = key,
                        onValueChange = {
                            key = it
                            enable = !key.isEmpty()
                        },
                        label = { Text(stringResource(id = R.string.default_su_cmd)) },
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    IconButton(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(end = 12.dp),
                        onClick = { keyVisible = !keyVisible }
                    ) {
                        Icon(
                            imageVector = if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    showDialog.value = false
                }
            ) {
                Text(stringResource(id = android.R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                enabled = enable,
                onClick = {
                    showDialog.value = false
                    sharedPreferences.edit().putString("su_cmd", key).apply()
                    setRootShell(createRootShell(key))
                    /*context.finish()
                    System.exit(0)*/
                }
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onBack: () -> Unit = {}) {
    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
        },
    )
}