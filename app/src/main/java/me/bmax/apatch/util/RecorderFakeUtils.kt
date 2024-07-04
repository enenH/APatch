package me.bmax.apatch.util

import android.annotation.SuppressLint
import android.os.Build
import android.text.TextUtils
import android.view.WindowManager
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale

object RecorderFakeUtils {
    const val ROM_MIUI = "MIUI"
    const val ROM_EMUI = "EMUI"
    const val ROM_FLYME = "FLYME"
    const val ROM_OPPO = "OPPO"
    const val ROM_SMARTISAN = "SMARTISAN"
    const val ROM_VIVO = "VIVO"
    const val ROM_QIKU = "QIKU"
    const val ROM_NUBIAUI = "NUBIAUI"
    const val ROM_ONEPLUS = "HYDROGEN"
    const val ROM_SAMSUNG = "ONEUI"
    const val ROM_BLACKSHARK = "JOYUI"
    const val ROM_ROG = "REPLIBLIC"
    private const val KEY_VERSION_MIUI = "ro.miui.ui.version.name"
    private const val KEY_VERSION_EMUI = "ro.build.version.emui"
    private const val KEY_VERSION_OPPO = "ro.build.version.opporom"
    private const val KEY_VERSION_SMARTISAN = "ro.smartisan.version"
    private const val KEY_VERSION_VIVO = "ro.vivo.os.version"
    private const val KEY_VERSION_NUBIA = "ro.build.nubia.rom.name"
    private const val KEY_VERSION_ONEPLIS = "ro.build.ota.versionname"
    private const val KEY_VERSION_SAMSUNG = "ro.channel.officehubrow"
    private const val KEY_VERSION_BLACKSHARK = "ro.blackshark.rom"
    private const val KEY_VERSION_ROG = "ro.build.fota.version"
    private var sName: String? = null
    val isEmui: Boolean
        //华为
        get() = check(ROM_EMUI)
    val isMiui: Boolean
        //小米
        get() = check(ROM_MIUI)
    val isVivo: Boolean
        //vivo
        get() = check(ROM_VIVO)
    val isOppo: Boolean
        //oppo
        get() = check(ROM_OPPO)
    val isFlyme: Boolean
        //魅族
        get() = check(ROM_FLYME)
    val isNubia: Boolean
        //红魔
        get() = check(ROM_NUBIAUI)
    val isOnePlus: Boolean
        //一加
        get() = check(ROM_ONEPLUS)
    val isSanSung: Boolean
        //三星
        get() = check(ROM_SAMSUNG)
    val isBLACKSHARK: Boolean
        //黑鲨
        get() = check(ROM_BLACKSHARK)
    val isRog: Boolean
        //ROG
        get() = check(ROM_ROG)
    val isActivice: Boolean
        get() = false

    fun setFakeRecorderWindowLayoutParams(layoutParams: WindowManager.LayoutParams) {
        try {
            /* 华为 OPPO VIVO 红魔 设置 Title 就行*/
            layoutParams.title = fakeRecordWindowTitle
            /* 下面是特殊的 */if (check(ROM_FLYME)) {
                /* 理论全系魅族 */
                if (!setMeizuParams(layoutParams, 0x2000)) {
                    if (isActivice) {
                        setMeizuParams_new(layoutParams, 1024) //最新魅族
                    }
                }
            } else if (check(ROM_MIUI) || check(ROM_BLACKSHARK)) {
                /* 理论全系小米 黑鲨 */
                setXiaomiParams(layoutParams, 6666)
            } else if (check(ROM_ONEPLUS) && (isActivice || Build.VERSION.SDK_INT == 30)) {
                /* 一加 Android 11 测试通过 其他需要Xposed */
                @SuppressLint("SoonBlockedPrivateApi") val privateflagField =
                    layoutParams.javaClass.getDeclaredField("PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY")
                privateflagField.isAccessible = true
                setOnePulsParams(layoutParams, privateflagField[layoutParams.javaClass] as Int)
            } else if (isSanSung) {
                /* 三星s7+平板 测试通过 */
                setSamsungFlags(layoutParams)
            } else if (check(ROM_ROG)) {
                /* ROG 测试通过 */
                layoutParams.memoryType = layoutParams.memoryType or 0x10000000
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setXiaomiParams(params: WindowManager.LayoutParams, flagValue: Int): Boolean {
        return try {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_DITHER
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @SuppressLint("PrivateApi")
    private fun setMeizuParams(params: WindowManager.LayoutParams, flagValue: Int): Boolean {
        return try {
            val MeizuParamsClass = Class.forName("android.view.MeizuLayoutParams")
            val flagField = MeizuParamsClass.getDeclaredField("flags")
            flagField.isAccessible = true
            val MeizuParams = MeizuParamsClass.newInstance()
            flagField.setInt(MeizuParams, flagValue)
            val mzParamsField = params.javaClass.getField("meizuParams")
            mzParamsField[params] = MeizuParams
            true
        } catch (e: IllegalAccessException) {
            false
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: NoSuchFieldException) {
            false
        } catch (e: InstantiationException) {
            false
        }
    }

    private fun setMeizuParams_new(params: WindowManager.LayoutParams, flagValue: Int): Boolean {
        return try {
            val mzParamsField = params.javaClass.getDeclaredField("meizuFlags")
            mzParamsField.isAccessible = true
            mzParamsField.setInt(params, flagValue)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun setOnePulsParams(params: WindowManager.LayoutParams, flagValue: Int) {
        try {
            @SuppressLint("DiscouragedPrivateApi") val flagField =
                params.javaClass.getDeclaredField("privateFlags")
            flagField.isAccessible = true
            flagField[params] = flagValue
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setSamsungFlags(params: WindowManager.LayoutParams) {
        try {
            val semAddExtensionFlags =
                params.javaClass.getMethod("semAddExtensionFlags", Integer.TYPE)
            val semAddPrivateFlags = params.javaClass.getMethod("semAddPrivateFlags", Integer.TYPE)
            semAddExtensionFlags.invoke(params, -2147352576)
            semAddPrivateFlags.invoke(params, params.flags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val fakeRecordWindowTitle: String
        /**
         * 总结下可以操作的是 华为，魅族，OPPO，VIVO，红魔，小米，一加 ORG
         *
         * @return
         */
        private get() {
            if (sName == null) {
                check("")
            }
            if (sName == null) {
                return ""
            }
            when (sName) {
                ROM_MIUI -> return "com.miui.screenrecorder"
                ROM_EMUI -> return "ScreenRecoderTimer"
                ROM_OPPO -> return "com.coloros.screenrecorder.FloatView"
                ROM_VIVO -> return "screen_record_menu"
                ROM_ONEPLUS -> return "op_screenrecord"
                ROM_FLYME -> return "SysScreenRecorder"
                ROM_NUBIAUI -> return "NubiaScreenDecorOverlay"
                ROM_BLACKSHARK -> return "com.blackshark.screenrecorder"
                ROM_ROG -> return "com.asus.force.layer.transparent.SR.floatingpanel"
            }
            return ""
        }

    private fun check(rom: String): Boolean {
        if (sName != null) {
            return sName == rom
        }
        if (!TextUtils.isEmpty(getProp(KEY_VERSION_MIUI))) {
            sName = ROM_MIUI
        } else if (!TextUtils.isEmpty(getProp(KEY_VERSION_BLACKSHARK))) {
            sName = ROM_BLACKSHARK
        } else if (!TextUtils.isEmpty(getProp(KEY_VERSION_EMUI))) {
            sName = ROM_EMUI
        } else if (!TextUtils.isEmpty(getProp(KEY_VERSION_OPPO))) {
            sName = ROM_OPPO
        } else if (!TextUtils.isEmpty(getProp(KEY_VERSION_VIVO))) {
            sName = ROM_VIVO
        } else if (!TextUtils.isEmpty(getProp(KEY_VERSION_SMARTISAN))) {
            sName = ROM_SMARTISAN
        } else if (!TextUtils.isEmpty(getProp(KEY_VERSION_NUBIA))) {
            sName = ROM_NUBIAUI
        } else if (!TextUtils.isEmpty(getProp(KEY_VERSION_ONEPLIS)) && getProp(KEY_VERSION_ONEPLIS)!!
                .lowercase(Locale.getDefault()).contains("hydrogen")
        ) {
            sName = ROM_ONEPLUS
        } else if (!TextUtils.isEmpty(getProp(KEY_VERSION_ROG)) && getProp(KEY_VERSION_ROG)!!
                .lowercase(Locale.getDefault()).contains("CN_Phone")
        ) {
            sName = ROM_ROG
        } else if (!TextUtils.isEmpty(getProp(KEY_VERSION_SAMSUNG))) {
            sName = ROM_SAMSUNG
        } else {
            val sVersion = Build.DISPLAY
            if (sVersion.uppercase(Locale.getDefault()).contains(ROM_FLYME)) {
                sName = ROM_FLYME
            } else {
                sName = Build.MANUFACTURER.uppercase(Locale.getDefault())
            }
        }
        return sName == rom
    }

    private fun getProp(name: String): String? {
        val line: String
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop $name")
            input = BufferedReader(InputStreamReader(p.inputStream), 1024)
            line = input.readLine()
            input.close()
        } catch (ex: IOException) {
            return null
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return line
    }
}