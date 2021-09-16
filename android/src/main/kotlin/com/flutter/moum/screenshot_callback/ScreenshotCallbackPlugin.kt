package com.flutter.moum.screenshot_callback
import android.os.*
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File

class ScreenshotCallbackPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {

    private var handler: Handler? = null
    private var fileObserver: FileObserver? = null
    lateinit var channel: MethodChannel
    override fun onMethodCall(call: MethodCall, result: Result) {
        when {
            call.method.equals("initialize") -> {
                handler = Handler(Looper.getMainLooper())
                if (Build.VERSION.SDK_INT >= 29) {
                    //Log.d(TAG, "android x");
                    val files = mutableListOf<File>()
                    for (path in Path.values()) {
                        files.add(File(path.path))
                    }
                    fileObserver = object : FileObserver(files, CREATE) {
                        override fun onEvent(event: Int, path: String?) {
                            //Log.d(TAG, "androidX onEvent");
                            if (event == CREATE) {
                                handler?.post { channel.invokeMethod("onCallback", null) }
                            }
                        }
                    }
                    fileObserver?.startWatching()
                } else {
                    //Log.d(TAG, "android others");
                    for (path in Path.values()) {
                        fileObserver = object : FileObserver(File(path.path), CREATE) {
                            override fun onEvent(event: Int, path: String?) {
                                //Log.d(TAG, "android others onEvent");
                                if (event == CREATE) {
                                    handler?.post { channel.invokeMethod("onCallback", null) }
                                }
                            }
                        }
                        fileObserver?.startWatching()
                    }
                }
                result.success("initialize")
            }
            call.method.equals("dispose") -> {
                fileObserver?.stopWatching()
                result.success("dispose")
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    companion object {
        private const val channelName = "flutter.moum/screenshot_callback"
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, channelName)
        channel.setMethodCallHandler(this)

    }

    override fun onDetachedFromEngine(p0: FlutterPlugin.FlutterPluginBinding) {

    }
}