package me.iacn.biliroaming.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import me.iacn.biliroaming.log
import me.iacn.biliroaming.network.BiliRoamingApi
import me.iacn.biliroaming.network.StreamUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * Created by Meolunr on 2020/7/16
 * Email meolunr@gmail.com
 */
class BangumiDownloadHook(classLoader: ClassLoader) : BaseHook(classLoader) {
    override fun startHook() {
//        if (!XposedInit.sPrefs.getBoolean("allow_download", false)) return
        log("startHook: BangumiDownload")

        findAndHookMethod("com.bilibili.lib.okhttp.huc.OkHttpURLConnection", mClassLoader, "getInputStream", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                // Found from "b.ecy" in version 5.39.1
                val connection = param.thisObject as HttpURLConnection
                val urlString = connection.url.toString()

                if (urlString.startsWith("https://api.bilibili.com/pgc/player/api/playurl")) {
                    val queryString = urlString.substring(urlString.indexOf("?"))
                    if (queryString.contains("ep_id=")) {
                        onBangumiDownload(param, queryString)
                    }
                }
            }
        })
    }

    private fun onBangumiDownload(param: MethodHookParam, queryString: String) {
        val inputStream = param.result as InputStream
        var content = StreamUtils.getContent(inputStream)

        if (isLoadingLimited(content)) {
            println(queryString)
            content = BiliRoamingApi.getPlayUrl(queryString)
            log("Has replaced play url with proxy server")
        }
        param.result = ByteArrayInputStream(content.toByteArray())
    }

    private fun isLoadingLimited(content: String): Boolean {
        return try {
            val json = JSONObject(content)
            val code = json.optInt("code")
            log("Loading play url: code = $code")
            code == -10403
        } catch (e: JSONException) {
            e.printStackTrace()
            false
        }
    }
}