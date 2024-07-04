package me.bmax.apatch.ui.screen

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.system.Os
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.topjohnwu.superuser.Shell
import dev.utils.app.AppUtils.getPackageName
import me.bmax.apatch.*
import me.bmax.apatch.R
import me.bmax.apatch.services.MyService
import me.bmax.apatch.services.RootServices
import me.bmax.apatch.ui.component.ConfirmDialog
import me.bmax.apatch.ui.screen.destinations.PatchScreenDestination
import me.bmax.apatch.ui.screen.destinations.SettingScreenDestination
import me.bmax.apatch.util.APatchCli
import me.bmax.apatch.util.checkRoot
import me.bmax.apatch.util.fastCmdResult
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.reboot
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@RootNavGraph(start = true)
@Destination
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)

    Scaffold(
        topBar = {
            TopBar(onSettingsClick = {
                navigator.navigate(SettingScreenDestination)
            })
        }/*, floatingActionButton = {

        FloatButton(navigator)

    }*/
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WarningCard()
            //KStatusCard(state)
            //AStatusCard(state, navigator)
            InfoCard()
            LoginCard()
            LearnMoreCard()
            Spacer(Modifier)
            ConfirmDialog()
        }
    }
}

private suspend inline fun connectRootService(
    crossinline onDisconnect: () -> Unit = {}
): Pair<IBinder, ServiceConnection> = suspendCoroutine {
    val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            onDisconnect()
        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            it.resume(binder as IBinder to this)
        }
    }
    val intent = Intent(apApp, RootServices::class.java);
    val task = RootServices.bindOrTask(
        intent,
        Shell.EXECUTOR,
        connection,
    )
    val shell = APatchCli.SHELL
    task?.let { it1 -> shell.execTask(it1) }
}

private fun stopRootService() {
    val intent = Intent(apApp, RootServices::class.java);
    RootServices.stop(intent)
}

@Composable
fun LoginKey(showDialog: MutableState<Boolean>) {
    var key by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var enable by remember { mutableStateOf(false) }
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
                        label = { Text(stringResource(id = R.string.super_key)) },
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
                    apApp.updateLoginKey(key)
                }
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
    )
}

@Composable
fun AuthSuperKey(showDialog: MutableState<Boolean>) {
    var key by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(true) }
    var enable by remember { mutableStateOf(false) }
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
                        singleLine = true,
                        onValueChange = {
                            key = it
                            enable = key.trim().length == 20
                        },
                        label = { Text(stringResource(id = R.string.super_key)) },
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
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
                    apApp.updateLoginKey(key)
                }
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
    )
}

@Composable
fun StartPatch(showDialog: MutableState<Boolean>, navigator: DestinationsNavigator) {
    var key by remember { mutableStateOf("") }
    var enable by remember { mutableStateOf(false) }

    val selectBootimgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val data = it.data ?: return@rememberLauncherForActivityResult
        val uri = data.data ?: return@rememberLauncherForActivityResult
        navigator.navigate(PatchScreenDestination(uri, key))
    }

    AlertDialog(
        onDismissRequest = { showDialog.value = false },
        title = { Text(stringResource(id = R.string.home_patch_set_key_title)) },
        text = {
            Column {
                Text(stringResource(id = R.string.home_patch_set_key_desc))
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = key,
                    onValueChange = {
                        key = it;
                        enable = !it.isEmpty()
                    },
                    label = { Text(stringResource(id = R.string.super_key)) },
                    visualTransformation = VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            Button(
                enabled = enable,
                onClick = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "*/*"
                    selectBootimgLauncher.launch(intent)
                }
            ) {
                Text(stringResource(id = R.string.home_patch_next_step))
            }
        },
    )
}

/*@Composable
fun FloatButton(navigator: DestinationsNavigator) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)

    var showAuthKeyDialog = remember { mutableStateOf(false) }
    var showSetKeyDialog = remember { mutableStateOf(false) }
    var permissionRequest = remember { mutableStateOf(false) }

    if (showAuthKeyDialog.value) {
        AuthSuperKey(showDialog = showAuthKeyDialog)
    }

    if (permissionRequest.value) {
        PermissionUtils.permission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
            .callback(object : PermissionCallback {
                override fun onGranted() {
                    permissionRequest.value = false
                    showSetKeyDialog.value = true
                }

                override fun onDenied(
                    grantedList: MutableList<String>?,
                    deniedList: MutableList<String>?,
                    notFoundList: MutableList<String>?
                ) {
                    permissionRequest.value = false
                }
            })
            .request(LocalContext.current as Activity)
    }

    if (showSetKeyDialog.value) {
        StartPatch(showDialog = showSetKeyDialog, navigator)
    }

    Column(horizontalAlignment = Alignment.End) {
        Box() {
            ExtendedFloatingActionButton(
                onClick = {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        permissionRequest.value = true
                    } else {
                        showSetKeyDialog.value = true
                    }
                },
                icon = { Icon(Icons.Filled.InstallMobile, "install") },
                text = { Text(text = stringResource(id = R.string.patch)) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Box() {
            ExtendedFloatingActionButton(
                onClick = {
                    if (state != APApplication.State.UNKNOWN_STATE) {
                        apApp.updateLoginKey("")
                    } else {
                        showAuthKeyDialog.value = true
                    }
                },
                icon = { Icon(Icons.Filled.Key, "") },
                text = {
                    Text(
                        text = if (state != APApplication.State.UNKNOWN_STATE) {
                            stringResource(id = R.string.clear_super_key)
                        } else {
                            stringResource(id = R.string.super_key)
                        }
                    )
                },
            )
        }
    }
}*/

@Composable
fun RebootDropdownItem(@StringRes id: Int, reason: String = "") {
    DropdownMenuItem(text = {
        Text(stringResource(id))
    }, onClick = {
        reboot(reason)
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onSettingsClick: () -> Unit) {
    TopAppBar(title = { Text(stringResource(R.string.app_name)) }, actions = {
        var showDropdown by remember { mutableStateOf(false) }
        IconButton(onClick = {
            showDropdown = true
        }) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(id = R.string.reboot)
            )

            DropdownMenu(expanded = showDropdown, onDismissRequest = {
                showDropdown = false
            }) {
                RebootDropdownItem(id = R.string.reboot)

                val pm =
                    LocalContext.current.getSystemService(Context.POWER_SERVICE) as PowerManager?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true) {
                    RebootDropdownItem(id = R.string.reboot_userspace, reason = "userspace")
                }
                RebootDropdownItem(id = R.string.reboot_recovery, reason = "recovery")
                RebootDropdownItem(id = R.string.reboot_bootloader, reason = "bootloader")
                RebootDropdownItem(id = R.string.reboot_download, reason = "download")
                RebootDropdownItem(id = R.string.reboot_edl, reason = "edl")
            }
        }

        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(id = R.string.settings)
            )
        }
    })
}

@Composable
private fun KStatusCard(state: APApplication.State) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = run {
            MaterialTheme.colorScheme.secondaryContainer
        })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(R.string.kernel_patch),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    !state.equals(APApplication.State.UNKNOWN_STATE) -> {
                        val kernelPatchVersion = Natives.kerenlPatchVersion()
                        Icon(Icons.Outlined.CheckCircle, stringResource(R.string.home_working))
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(
                                text = stringResource(R.string.home_working),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.kpatch_version,
                                    kernelPatchVersion.and(0xff0000).shr(16),
                                    kernelPatchVersion.and(0xff00).shr(8),
                                    kernelPatchVersion.and(0xff)
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            when {
                                state.equals(APApplication.State.ANDROIDPATCH_INSTALLED) || state.equals(
                                    APApplication.State.ANDROIDPATCH_NEED_UPDATE
                                ) -> {
                                    Text(
                                        text = stringResource(
                                            R.string.home_su_path_ex,
                                            Natives.suPath(),
                                            APApplication.APD_PATH
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.kpatch_shadow_path,
                                            APApplication.KPATCH_SHADOW_PATH,
                                            APApplication.KPATCH_PATH
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                !state.equals(APApplication.State.UNKNOWN_STATE) -> {
                                    Text(
                                        text = stringResource(
                                            R.string.home_su_path,
                                            Natives.suPath()
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Icon(Icons.Outlined.Block, stringResource(R.string.home_install_unknown))
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(
                                text = stringResource(R.string.home_install_unknown),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginCard() {
    var showLoginDialog = remember { mutableStateOf(false) }

    if (showLoginDialog.value) {
        AuthSuperKey(showDialog = showLoginDialog)
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = run {
            MaterialTheme.colorScheme.primaryContainer
        })
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    showLoginDialog.value = true
                }
                .fillMaxWidth()
                .padding(24.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Key, stringResource(R.string.enter_key))
            Column(Modifier.padding(start = 20.dp)) {
                Text(
                    text = stringResource(R.string.enter_key),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = apApp.getLoginKey().let { it.ifEmpty { "..." } },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (!apApp.getLoginKey().isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val context = LocalContext.current as Activity
                val isStarted = rememberSaveable { mutableStateOf(false) }
                /*  val isClicked = rememberSaveable { mutableStateOf(false) }*/

                val buttonColor =
                    animateColorAsState(if (isStarted.value) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                /*  if (isClicked.value) {
                      CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                  } else {*/
                ElevatedButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = onClick@{
                        if (!Settings.canDrawOverlays(apApp)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.getPackageName()}")
                            )
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            context.finish()
                            System.exit(0)
                            return@onClick
                        }

                        if (isStarted.value) return@onClick
                        isStarted.value = true

                        if (checkRoot()) {
                            if (!appDraw) {
                                var cmd = apApp.applicationInfo.nativeLibraryDir + "/libentryy.so"
                                if (!antiRecord) cmd += " -h"
                                if (recordBlack) cmd += " -s"
                                cmd += " -k ${apApp.getLoginKey()}"
                                thread {
                                    fastCmdResult("pkill libentryy.so")
                                    fastCmdResult("nsenter --mount=/proc/1/ns/mnt -- " + cmd)
                                    isStarted.value = false
                                }
                                return@onClick
                            }

                            var binder: IBinder? = null
                            val connection = object : ServiceConnection {
                                override fun onServiceDisconnected(name: ComponentName?) {
                                    Log.w(TAG, "RootService disconnected")
                                    context.stopService(Intent(context, MyService::class.java))
                                    isStarted.value = false
                                }

                                override fun onServiceConnected(
                                    pname: ComponentName?,
                                    pbinder: IBinder?
                                ) {
                                    binder = pbinder
                                }
                            }
                            val intent = Intent(apApp, RootServices::class.java);
                            val task = RootServices.bindOrTask(
                                intent,
                                Shell.EXECUTOR,
                                connection,
                            )
                            val shell = APatchCli.SHELL
                            task?.let { it1 -> shell.execTask(it1) }

                            thread {
                                while (binder == null) {
                                    Thread.sleep(100)
                                }
                                val ipc = IAPRootService.Stub.asInterface(binder)
                                Native.ipc = ipc
                                Native.init(
                                    apApp.getLoginKey(),
                                    context.filesDir.absolutePath,
                                    antiRecord,
                                    recordBlack,
                                    appDraw
                                )
                                context.runOnUiThread {
                                    context.startForegroundService(
                                        Intent(
                                            context,
                                            MyService::class.java
                                        )
                                    )
                                }
                                while (true) {
                                    if (Native.getWindowSize()[0] == -100f) {
                                        break
                                    }
                                    Thread.sleep(2000)
                                }
                                context.runOnUiThread {
                                    context.stopService(Intent(context, MyService::class.java))
                                    isStarted.value = false
                                }
                            }
                            return@onClick
                        }

                        context.startForegroundService(
                            Intent(
                                context,
                                MyService::class.java
                            )
                        )
                        thread {
                            Native.init(
                                apApp.getLoginKey(),
                                context.filesDir.absolutePath,
                                antiRecord,
                                recordBlack,
                                appDraw
                            )
                            while (true) {
                                if (Native.getWindowSize()[0] == -100f) {
                                    break
                                }
                                Thread.sleep(2000)
                            }
                            context.runOnUiThread {
                                context.stopService(Intent(context, MyService::class.java))
                                isStarted.value = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value)
                ) {
                    Text(
                        text = if (isStarted.value) stringResource(R.string.running) else stringResource(
                            R.string.start
                        )
                    )
                }
            }
        }
    }
}

/*if (!Settings.canDrawOverlays(apApp)) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    context.finish()
    System.exit(0)
}*/
@Composable
private fun AStatusCard(state: APApplication.State, navigator: DestinationsNavigator) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = run {
            MaterialTheme.colorScheme.secondaryContainer
        })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(R.string.android_patch),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    state.equals(APApplication.State.KERNELPATCH_READY) -> {
                        Icon(Icons.Outlined.Block, stringResource(R.string.home_not_installed))
                    }

                    state.equals(APApplication.State.ANDROIDPATCH_INSTALLING) -> {
                        Icon(Icons.Outlined.InstallMobile, stringResource(R.string.home_installing))
                    }

                    state.equals(APApplication.State.ANDROIDPATCH_INSTALLED) -> {
                        Icon(Icons.Outlined.CheckCircle, stringResource(R.string.home_working))
                    }

                    state.equals(APApplication.State.ANDROIDPATCH_NEED_UPDATE) -> {
                        Icon(Icons.Outlined.SystemUpdate, stringResource(R.string.home_need_update))
                    }

                    else -> {
                        Icon(Icons.Outlined.Block, stringResource(R.string.home_install_unknown))
                    }
                }
                Column(
                    Modifier
                        .weight(2f)
                        .padding(start = 16.dp)
                ) {
                    val managerVersion = getManagerVersion()
                    when {
                        state.equals(APApplication.State.KERNELPATCH_READY) -> {
                            Text(
                                text = stringResource(R.string.home_not_installed),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        state.equals(APApplication.State.ANDROIDPATCH_INSTALLING) -> {
                            Text(
                                text = stringResource(R.string.home_installing),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        state.equals(APApplication.State.ANDROIDPATCH_INSTALLED) -> {
                            Text(
                                text = stringResource(R.string.home_working),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.apatch_version,
                                    "${managerVersion.first} (${managerVersion.second})"
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        state.equals(APApplication.State.ANDROIDPATCH_NEED_UPDATE) -> {
                            Text(
                                text = stringResource(R.string.home_need_update),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.apatch_version_update,
                                    "${APApplication.apatchVersion}",
                                    "${managerVersion.first} (${managerVersion.second})"
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        else -> {
                            Text(
                                text = stringResource(R.string.home_install_unknown),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                if (!state.equals(APApplication.State.UNKNOWN_STATE)) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    ) {
                        Button(
                            onClick = {
                                when {
                                    state.equals(APApplication.State.KERNELPATCH_READY)
                                            || state.equals(APApplication.State.ANDROIDPATCH_NEED_UPDATE) -> {
                                        APApplication.install()
//                                        if (state.equals(APApplication.State.ANDROIDPATCH_NEED_UPDATE)) {
//                                            navigator.navigate(PatchScreenDestination(null, apApp.getSuperKey()))
//                                        }
                                    }

                                    state.equals(APApplication.State.ANDROIDPATCH_INSTALLED) -> {
                                        APApplication.uninstall()
                                    }

                                    else -> {

                                    }
                                }
                            },
                            content = {
                                when {
                                    state.equals(APApplication.State.KERNELPATCH_READY) -> {
                                        Text(
                                            text = stringResource(id = R.string.home_ap_cando_install),
                                            color = Color.Black
                                        )
                                    }

                                    state.equals(APApplication.State.ANDROIDPATCH_UNINSTALLING)
                                            || state.equals(APApplication.State.ANDROIDPATCH_UNINSTALLING) -> {
                                        Icon(Icons.Outlined.Cached, contentDescription = "busy")
                                    }

                                    state.equals(APApplication.State.ANDROIDPATCH_NEED_UPDATE) -> {
                                        Text(
                                            text = stringResource(id = R.string.home_ap_cando_update),
                                            color = Color.Black
                                        )
                                    }

                                    state.equals(APApplication.State.ANDROIDPATCH_INSTALLED) -> {
                                        Text(
                                            text = stringResource(id = R.string.home_ap_cando_uninstall),
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun WarningCard() {
    var show by rememberSaveable { mutableStateOf(!checkRoot()) }
    if (show) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = run {
                MaterialTheme.colorScheme.error
            })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "warning")
                }
                Column(
                    modifier = Modifier
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.no_root),
                        )

                        Spacer(Modifier.width(12.dp))

                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = "",
                            modifier = Modifier
                                .clickable {
                                    show = false
                                    //apApp.updateBackupWarningState(false)
                                },
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun InfoCard() {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = run {
            MaterialTheme.colorScheme.secondaryContainer
        })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            val contents = StringBuilder()
            val uname = Os.uname()

            @Composable
            fun InfoCardItem(label: String, content: String) {
                contents.appendLine(label).appendLine(content).appendLine()
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                Text(text = content, style = MaterialTheme.typography.bodyMedium)
            }

            InfoCardItem(stringResource(R.string.home_kernel), uname.release)

            Spacer(Modifier.height(16.dp))
            InfoCardItem(stringResource(R.string.home_fingerprint), Build.FINGERPRINT)

            Spacer(Modifier.height(16.dp))
            InfoCardItem(stringResource(R.string.home_selinux_status), getSELinuxStatus())
        }
    }
}


@Composable
fun LearnMoreCard() {
    /* val uriHandler = LocalUriHandler.current
     val url = stringResource(R.string.home_learn_android_patch_url)*/
    val context = LocalContext.current
    ElevatedCard {
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Toast
                    .makeText(context, "(¬_¬)", Toast.LENGTH_SHORT)
                    .show()
                //uriHandler.openUri(url)
            }
            .padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column() {
                Text(
                    text = stringResource(R.string.home_common_question),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_common_question_text),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


fun getManagerVersion(): Pair<String, Int> {
    val packageInfo = apApp.packageManager.getPackageInfo(apApp.packageName, 0)
    return Pair(packageInfo.versionName, packageInfo.versionCode)
}
