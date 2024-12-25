package com.jacky.union_pay

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.StringCodec;
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.unionpay.UPPayAssistEx

import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap


/** FlutterUnionPayPlugin */
class FlutterUnionPayPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener  {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private var messageChannel: BasicMessageChannel<String>? = null
  private var activity: Activity? = null

  companion object {
    const val PAYMENT_CANCEL = 0
    const val PAYMENT_SUCCESS = 1
    const val PAYMENT_FAIL = 2

    const val PACKAGE_NAME = "flutter_union_pay"
    const val MESSAGE_CHANNEL_NAME = "flutter_union_pay.message"
  }


  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, PACKAGE_NAME)
    messageChannel = BasicMessageChannel(flutterPluginBinding.binaryMessenger, MESSAGE_CHANNEL_NAME, StringCodec.INSTANCE)
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "version" -> {
        result.success(UPPayAssistEx.VERSION)
      }
      "installed" -> {
        val mode = call.argument<String>("mode")
        val merchantInfo = call.argument<String>("merchantInfo")
        val installed = UPPayAssistEx.checkWalletInstalled(activity, mode,merchantInfo)
        Log.e("union app", "installed is $installed")
        result.success(installed)
      }
      "getBrand" -> {
        val brand = getSETypeForBrand()
        Log.i("返回手机品牌 Phone Brand", brand)
        result.success(brand) // 返回手机品牌
      }
      "pay" -> {
        val tn = call.argument<String>("tn")
        val env = call.argument<String>("env")
        Log.e("tn+env+seType", "$tn<<>>$env")
        val ret = UPPayAssistEx.startPay(activity, null, null, tn, env)
      }
      "sePay" -> {
        val tn = call.argument<String>("tn")
        val env = call.argument<String>("env")
        val seType = getSETypeForBrand()
        Log.e("tn+env+seType", "$tn<<>>$env<<>>$seType")
        val ret = UPPayAssistEx.startSEPay(activity, null, null, tn, env, seType)
        Log.e("ret", ret.toString())
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }


  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addActivityResultListener(this)
  }


  override fun onDetachedFromActivityForConfigChanges() {
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivity() {
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (data == null) {
      return true
    }
    Log.e("data==", data.extras.toString())
    val payload: HashMap<String, Any?> = HashMap()
    val paymentStatus = data.extras?.getString("pay_result")
    when (paymentStatus?.lowercase(Locale.ROOT)) {
      "success" -> payload["code"] = PAYMENT_SUCCESS
      "fail"-> payload["code"] = PAYMENT_FAIL
      "cancel"-> payload["code"] = PAYMENT_CANCEL
    }
    Log.i("payload",payload.toString())
    messageChannel?.send(JSONObject(payload).toString())
    return true
  }

  fun getSETypeForBrand(): String {
    val brand = android.os.Build.BRAND.lowercase() // 获取手机品牌并转为小写
    Log.i("手机品牌brand",brand)
    val seTypeMap = mapOf(
      "samsung" to "02", // 三星
      "huawei" to "04",  // 华为
      "honor" to "04",   // 荣耀
      "meizu" to "27",   // 魅族
      "leeco" to "30",   // 乐视
      "xiaomi" to "25",  // 小米
      "redmi" to "25",   // 红米与小米相同
      "oppo" to "29",    // OPPO
      "vivo" to "33",    // vivo
      "smartisan" to "32", // 锤子
      "realme" to "35",
      "oneplus" to "36"
    )

    // 根据品牌获取对应 seType 值，默认返回一个未定义值，例如 "00"
    return seTypeMap[brand] ?: "00"
  }
}
