import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter_union_pay/enum/union_pay_enum.dart';
import 'package:flutter_union_pay/union_pay.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => HomePageState();
}

class HomePageState extends State<HomePage> {
  String _platformVersion = 'Unknown';
  String _brandCode = '00';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    String code = '00';
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion = await UnionPay.uPayVersion();
      code = await UnionPay.seBrandCode();
      print("platformVersion===$platformVersion");
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
      _brandCode = code;
    });

    UnionPay.payListener((result) {
      showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text('TEST'),
            content: Text(result.status.toString()),
          );
        },
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          appBar: AppBar(
            title: const Text('Plugin example app'),
          ),
          body: Column(
            children: [
              Center(
                child: Text('Running on: $_platformVersion\n'),
              ),
              TextButton(
                  onPressed: () {
                    UnionPay.isInstalled(mode: PaymentEnv.DEVELOPMENT, merchantInfo: "").then((value) {
                      print("安装成功$value");
                    });
                  },
                  child: Text('是否安装云支付App')),
              TextButton(
                  onPressed: () {
                    UnionPay.pay(
                      mode: PaymentEnv.DEVELOPMENT,
                      tn: "669802785406247611910",
                      scheme: "UnionPayTest",
                    ).then((value) {
                      print("##########$value");
                    });
                  },
                  child: Text('调起支付')),
              TextButton(onPressed: () async {}, child: Text('获取手机品牌')),
              Text(_brandCode),
              TextButton(
                  onPressed: () {
                    UnionPay.sePay(
                      mode: PaymentEnv.DEVELOPMENT,
                      tn: "669802785406247611910",
                      scheme: "UnionPayTest",
                    ).then((value) {
                      print("##########$value");
                    });
                  },
                  child: Text('调起品牌支付')),
            ],
          )),
    );
  }
}
