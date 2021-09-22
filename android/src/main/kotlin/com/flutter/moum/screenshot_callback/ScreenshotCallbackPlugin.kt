package com.flutter.moum.screenshot_callback

import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import java.io.File
import java.util.ArrayList

class ScreenshotCallbackPlugin : FlutterPlugin, MethodCallHandler {
    private var handler: Handler? = null
    private var fileObserver: FileObserver? = null
    private var channel: MethodChannel? = null

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "flutter.moum/screenshot_callback")
        channel!!.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        channel = null
        if (fileObserver != null) {
            fileObserver!!.stopWatching()
        }
        fileObserver = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "initialize" -> {
                handler = Handler(Looper.getMainLooper())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val files: MutableList<File> = ArrayList()
                    for (path in Path.values()) {
                        files.add(File(path.path))
                    }
                    fileObserver = object : FileObserver(files, CREATE) {
                        override fun onEvent(event: Int, path: String?) {
                            if (event == CREATE) {
                                handler!!.post {
                                    if (channel != null) {
                                        channel!!.invokeMethod("onCallback", null)
                                    }
                                }
                            }
                        }
                    }
                    fileObserver?.startWatching()
                } else {
                    for (path in Path.values()) {
                        fileObserver = object : FileObserver(path.path, CREATE) {
                            override fun onEvent(event: Int, path: String?) {
                                if (event == CREATE) {
                                    handler!!.post {
                                        if (channel != null) {
                                            channel!!.invokeMethod("onCallback", null)
                                        }
                                    }
                                }
                            }
                        }
                        fileObserver?.startWatching()
                    }
                }
                result.success("initialize")
            }
            "dispose" -> {
                handler = null
                if (fileObserver != null) {
                    fileObserver!!.stopWatching()
                }
                result.success("dispose")
            }
            else -> {
                result.notImplemented()
            }
        }
    }
}