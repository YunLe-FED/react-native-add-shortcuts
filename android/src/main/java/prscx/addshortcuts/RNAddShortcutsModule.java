
package prscx.addshortcuts;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.views.text.ReactFontManager;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RNAddShortcutsModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public RNAddShortcutsModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNAddShortcuts";
  }

  public static Bitmap getBitmapFromURL(String src) {
    try {
      Log.d("vk21", src);
      //uncomment below line in image name have spaces.
      //src = src.replaceAll(" ", "%20");

      URL url = new URL(src);

      HttpURLConnection connection = (HttpURLConnection) url
              .openConnection();
      connection.setDoInput(true);
      connection.connect();
      InputStream input = connection.getInputStream();
      Bitmap myBitmap = BitmapFactory.decodeStream(input);
      return myBitmap;
    } catch (Exception e) {
      Log.d("vk21", e.toString());
      Log.d("vk21", src);
      return null;
    }
  }

  @ReactMethod
  @TargetApi(26)
  private void AddPinnedShortcut(ReadableMap shortcut, final Callback onDone, final Callback onCancel) {
    if (!isShortcutSupported()) {
      onCancel.invoke();
      return;
    }
    if (ShortcutManagerCompat.isRequestPinShortcutSupported(reactContext)) {
      String label = shortcut.getString("label");
      String description = shortcut.getString("description");
//      String icon = shortcut.getString("icon");
      ReadableMap link = shortcut.getMap("link");
      ShortcutInfoCompat shortcut_ = new ShortcutInfoCompat.Builder(reactContext, link.getString("url"))
              .setShortLabel(label)
              .setShortLabel(description)
//              .setIcon(IconCompat.createWithBitmap(getBitmapFromURL(icon)))
              .setIntent(new Intent(Intent.ACTION_VIEW,
                      Uri.parse(link.getString("url"))))
              .build();
      Intent pinnedShortcutCallbackIntent = ShortcutManagerCompat.createShortcutResultIntent(reactContext, shortcut_);
      PendingIntent successCallback = PendingIntent.getBroadcast(reactContext, /* request code */ 0,
              pinnedShortcutCallbackIntent, /* flags */ 0);
      ShortcutManagerCompat.requestPinShortcut(reactContext, shortcut_, successCallback.getIntentSender());
    }
    onDone.invoke();
  }

  @ReactMethod
  @TargetApi(25)
  private void AddDynamicShortcut(ReadableMap shortcut, final Callback onDone, final Callback onCancel) {
    if (!isShortcutSupported()) {
      onCancel.invoke();
      return;
    }

    ShortcutManager mShortcutManager = getReactApplicationContext().getSystemService(ShortcutManager.class);

    String label = shortcut.getString("label");
    String description = shortcut.getString("description");
    ReadableMap icon = shortcut.getMap("icon");
    ReadableMap link = shortcut.getMap("link");

    BitmapDrawable drawable = null;
    try {
      Class<?> clazz = Class.forName("prscx.imagehelper.RNImageHelperModule"); // Controller A or B
      Class params[] = { ReadableMap.class };
      Method method = clazz.getDeclaredMethod("GenerateImage", params);

      drawable = (BitmapDrawable) method.invoke(null, icon);
    } catch (Exception e) {
    }

    Intent shortcutIntent = new Intent(getReactApplicationContext(), RNAddShortcutsModule.class);
    shortcutIntent.setAction(Intent.ACTION_MAIN);
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(link.getString("url")));

    ShortcutInfo shortcutInfo = null;
    if (drawable != null) {
      shortcutInfo = new ShortcutInfo.Builder(getReactApplicationContext(), label).setShortLabel(label)
              .setLongLabel(description).setIntent(intent).setIcon(Icon.createWithBitmap(drawable.getBitmap())).build();
    } else {
      shortcutInfo = new ShortcutInfo.Builder(getReactApplicationContext(), label).setShortLabel(label)
              .setLongLabel(description).setIntent(intent).build();
    }

    List<ShortcutInfo> list = new ArrayList<>();
    list.add(shortcutInfo);

    if (mShortcutManager != null) {
      mShortcutManager.setDynamicShortcuts(list);

      onDone.invoke();
      return;
    }

    onCancel.invoke();
  }

  @ReactMethod
  @TargetApi(25)
  public void RemoveAllDynamicShortcuts(final Callback onDone, final Callback onCancel) {
    if (!isShortcutSupported()) {
      onCancel.invoke();
      return;
    }

    getReactApplicationContext().getSystemService(ShortcutManager.class).removeAllDynamicShortcuts();

    onDone.invoke();
  }

  @ReactMethod
  @TargetApi(25)
  public void PopDynamicShortcuts(ReadableMap props, final Callback onDone, final Callback onCancel) {
    if (!isShortcutSupported()) {
      onCancel.invoke();
      return;
    }

    ReadableArray shortcutArray = props.getArray("shortcuts");
    List shortcuts = shortcutArray.toArrayList();

    getReactApplicationContext().getSystemService(ShortcutManager.class).removeDynamicShortcuts(shortcuts);

    onDone.invoke();
  }

  @ReactMethod
  @TargetApi(25)
  public void GetDynamicShortcuts(final Callback onDone, final Callback onCancel) {
    if (!isShortcutSupported()) {
      onCancel.invoke();
      return;
    }

    List<ShortcutInfo> shortcuts = getReactApplicationContext().getSystemService(ShortcutManager.class)
            .getDynamicShortcuts();
    List finalShortcuts = new ArrayList(shortcuts.size());

    for (ShortcutInfo shortcut : shortcuts) {
      HashMap<String, String> shortcutMap = new HashMap<>();
      shortcutMap.put("id", shortcut.getId());
      shortcutMap.put("label", shortcut.getShortLabel().toString());
      shortcutMap.put("description", shortcut.getLongLabel().toString());

      finalShortcuts.add(shortcutMap);
    }

    onDone.invoke(finalShortcuts.toString());
  }

  private boolean isShortcutSupported() {
    return Build.VERSION.SDK_INT >= 25;
  }
}
