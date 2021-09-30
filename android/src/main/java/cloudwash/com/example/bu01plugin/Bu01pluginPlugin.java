package cloudwash.com.example.bu01plugin;

import androidx.annotation.NonNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.util.Log;

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
  private static final String TAG = "BU01Plugin";

  private Context applicationContext;
  private MethodChannel method_channel;
  private EventChannel tag_channel;
  private EventChannel device_channel;

  private BU01_Service mService;
  private BU01_Reader mReader;

  private BU01_Reader.SingleInventoryCallback tagDetectCallback;
  private BU01_Service.Callback deviceDetectCallback;

  String sn;
  ArrayList<String> tagArray = new ArrayList<>();
  ArrayList<String> deviceArray = new ArrayList<>();
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

    tag_channel = new EventChannel(messenger, "tag_channel");
    tag_channel.setStreamHandler(new EventChannel.StreamHandler() {
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

    // device_channel = new EventChannel(messenger, "device_channel");
    // device_channel.setStreamHandler(new EventChannel.StreamHandler() {
    //   @Override
    //   public void onListen(Object arguments, final EventChannel.EventSink events) {
    //     deviceDetectCallback = new BU01_Service.Callback() {
    //       @Override
    //       public void onDiscoverBleReader(BU01_Reader reader) {
    //         Log.d("TAG", "new device detected");
    //       }
    //     };
    //   }

    //   @Override
    //   public void onCancel(Object arguments) {

    //   }
    // });
    
    deviceDetectCallback = new BU01_Service.Callback() {
      @Override
      public void onDiscoverBleReader(BU01_Reader reader) {
        Log.d("TAG", "new device detected");
      }
    };

    mService = BU01_Factory.bu01(this.applicationContext, reader -> {
      Log.d(TAG, "DEVICE DETECTED!!!!");
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

    tag_channel.setStreamHandler(null);
    tag_channel = null;

    // device_channel.setStreamHandler(null);
    // device_channel = null;
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

      case "startDeviceSearch":
        result.success(startDeviceSearch());
        break;

      case "stopDeviceSearch":
        result.success(stopDeviceSearch());
        break;

      default:
        result.notImplemented();
    }
  }

  private boolean startDeviceSearch() {
    mService.scanForBU01BleReader();
    Log.d("TAG", "searching for BU01 readers....");
    // mService.scanForBle();
    return true;
  }

  private boolean stopDeviceSearch() {
    mService.stopScan();
    return true;
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