
import 'dart:async';

import 'package:flutter/services.dart';

class Bu01plugin {
  static const MethodChannel _channel = const MethodChannel('bu01plugin');
  final tagStream = const EventChannel('tag_channel');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
  
  static Future<bool> connectReader(String macAddress) async {
    final bool isSuccess = await _channel.invokeMethod('connectToReader', <String, String>{'mac_address': macAddress});
    return isSuccess;
  }

  static Future<String?> get getSerialNumber async {
    final String? sn = await _channel.invokeMethod('getSerialNumber');
    return sn;
  }

  static Future<List<dynamic>?> get scanTags async {
    List<dynamic>? tagList = <dynamic>[];
    tagList = await _channel.invokeMethod('scanTags');

    return tagList;
  }
}
