import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:bu01plugin/bu01plugin.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

late final List<String> taglist = [""];

class _MyAppState extends State<MyApp> {
  var _isConnected;
  final textController = TextEditingController();

  Future<void> _scanTags() async {
    try {
      List<dynamic> tags = (await Bu01plugin.scanTags)!;
      for (String tag in tags) print(tag);
    } on PlatformException {}
  }

  void _showTags(dynamic o) {
    print("tag detected " + o);
    setState(() => taglist.add(o));
  }

  @override
  void initState() {
    super.initState();

    StreamSubscription tagSubscription = Bu01plugin()
        .tagStream
        .receiveBroadcastStream()
        .distinct()
        .listen(_showTags);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          appBar: AppBar(
            title: Text('Cloudwash UHF Demo'),
          ),
          body: Center(
              child: Column(
            children: <Widget>[
              Container(
                margin: EdgeInsets.all(25),
                child: TextField(
                  controller: textController,
                ),
              ),
              Container(
                margin: EdgeInsets.all(25),
                child: FlatButton(
                  child: Text(
                    'Scan for Reader Device',
                    style: TextStyle(fontSize: 20.0),
                  ),
                  color: Colors.blueAccent,
                  textColor: Colors.white,
                  onPressed: () {
                    Bu01plugin.startDeviceSearch();
                  },
                ),
              ),
              Container(
                margin: EdgeInsets.all(25),
                child: FlatButton(
                  child: Text(
                    'Stop searching for device',
                    style: TextStyle(fontSize: 20.0),
                  ),
                  color: Colors.blueAccent,
                  textColor: Colors.white,
                  onPressed: () {
                    Bu01plugin.stopDeviceSearch();
                  },
                ),
              ),
              Container(
                margin: EdgeInsets.all(25),
                child: FlatButton(
                  child: Text(
                    'Connect to UHF Reader',
                    style: TextStyle(fontSize: 20.0),
                  ),
                  color: Colors.blueAccent,
                  textColor: Colors.white,
                  onPressed: () {
                    print(
                        "connecting to reader address " + textController.text);
                    _isConnected =
                        Bu01plugin.connectReader(textController.text);
                  },
                ),
              ),
              Container(
                margin: EdgeInsets.all(25),
                child: FlatButton(
                  child: Text(
                    'Scan',
                    style: TextStyle(fontSize: 20.0),
                  ),
                  color: Colors.blueAccent,
                  textColor: Colors.white,
                  onPressed: () {
                    _scanTags();
                  },
                ),
              ),
              Center(child: Text("Detected Tags (EPC)")),
              Expanded(
                child: ListView.builder(
                    itemCount: taglist.length,
                    itemBuilder: (context, index) {
                      return ListTile(
                        title: Text(taglist[index]),
                      );
                    }),
              )
            ],
          ))),
    );
  }
}
