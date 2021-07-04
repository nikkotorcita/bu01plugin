package cloudwash.com.example.bu01plugin;

import androidx.annotation.NonNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import com.reader.ble.BU01_Factory;
import com.reader.ble.BU01_Reader;
import com.reader.ble.BU01_Service;
import com.reader.ble.impl.EpcReply;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Bu01pluginPlugin */
public class Bu01pluginPlugin implements FlutterPlugin, MethodCallHandler {

  private Context applicationContext;
  private MethodChannel method_channel;
  private EventChannel event_channel;

  private BU01_Service mService;
  private BU01_Reader mReader;

  private BU01_Reader.SingleInventoryCallback tagDetectCallback;

  String sn;
  ArrayList<String> tagArray = new ArrayList<>();
  int counter = 0;

  public static int readerStatus;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    onAttachedToEngine(flutterPluginBinding.getApplicationContext(), flutterPluginBinding.getBinaryMessenger());
  }

  private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
    this.applicationContext = applicationContext;
    method_channel = new MethodChannel(messenger, "bu01plugin");
    method_channel.setMethodCallHandler(this);

    event_channel = new EventChannel(messenger, "tag_channel");
    event_channel.setStreamHandler(new EventChannel.StreamHandler() {
      @Override
      public void onListen(Object arguments, final EventChannel.EventSink events) {
        tagDetectCallback = new BU01_Reader.SingleInventoryCallback() {
          @Override
          public void onResult(int i, List<EpcReply> list) {
            if(i == 0) {
              String tag;
              for(EpcReply epcReply : list) {
                tag = epcBytes2Hex(epcReply.getEpc());
                tagArray.add(tag);
                events.success(tag);
              }
            }
          }
        };
      }

      @Override
      public void onCancel(Object arguments) {

      }
    });
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    handleMethods(call, result);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    applicationContext = null;

    method_channel.setMethodCallHandler(null);
    method_channel = null;

    event_channel.setStreamHandler(null);
    event_channel = null;
  }

  private void handleMethods(@NonNull MethodCall call, @NonNull Result result) {
    switch(call.method) {
      case "connectToReader":
        final String mac_address = call.argument("mac_address");
        result.success(connectToReader(mac_address));
        break;
      case "getSerialNumber":
        result.success(getReaderSerialNumber());
        break;
      case "scanTags":
        result.success(bulkScan());
        break;


      default:
        result.notImplemented();
    }
  }

  private boolean connectToReader(String readerAddress) {
    boolean result;

    //readerAddress = "A4:06:E9:BC:2B:A5";

    if(readerAddress != null) {
      try{
        mReader = BU01_Factory.bu01ByAddress(this.applicationContext, readerAddress.toUpperCase());
      } catch (Exception e) {
        return false;
      }
    }
    else {
      System.out.println("Reader address provided is null");
      return false;
    }

    mReader.connect(this.applicationContext, new BU01_Reader.Callback() {
      @Override
      public void onConnect() {
        readerStatus = 0;
      }

      @Override
      public void onDisconnect() {
        readerStatus = -1;
      }
    });

    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
    }

    if(readerStatus == 0)
      return true;
    else {
      return false;
    }
  }

  private String getReaderSerialNumber() {
    if(readerStatus == 0) {
      mReader.getSerialNumber(new BU01_Reader.GetSerialNumberCallback() {
        @Override
        public void onResult(int status, long serialNumber) {
          if (status == 0) {
            sn = String.format(Locale.getDefault(), "%010d", serialNumber);
          }
        }
      });

      return sn;
    }
    else
      return null;
  }

  private static String getBatteryLevel() {
    String batteryLevel = null;

    return batteryLevel;
  }

  private ArrayList<String> bulkScan() {
    ArrayList<String> scanned_tags = new ArrayList<>();
    if(readerStatus == 0) {
      /*
      mReader.singleInventory(new BU01_Reader.SingleInventoryCallback() {
        @Override
        public void onResult(int i, List<EpcReply> list) {
          if(i == 0) {
            String tag;
            for(EpcReply epcReply : list) {
              tag = epcBytes2Hex(epcReply.getEpc());
              tagArray.add(tag);
            }
          }
        }
      });
       */

      mReader.singleInventory(tagDetectCallback);
    }

    for(String tag : scanned_tags)
      System.out.println(tag);

    return scanned_tags;
  }

  private static String epcBytes2Hex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for(int i = 0; i < bytes.length; i++) {
      sb.append(String.format("%02X", bytes[i]));
      if((i + 1) % 4 ==0) {
        sb.append(" ");
      }
    }
    return sb.toString().trim();
  }
}