// Copyright 2017 Michael Goderbauer. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package net.goderbauer.flutter.contactpicker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;
import java.util.HashMap;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.app.Activity.RESULT_OK;

public class ContactPickerPlugin implements MethodCallHandler, PluginRegistry.ActivityResultListener {
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "contact_picker");
    ContactPickerPlugin instance = new ContactPickerPlugin(registrar.activity());
    registrar.addActivityResultListener(instance);
    channel.setMethodCallHandler(instance);
  }

    private ContactPickerPlugin(Activity activity) {
        this.activity = activity;
    }

  private static int PICK_CONTACT = 2015;

  private Activity activity;
  private Result pendingResult;

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("selectContact")) {
      if (pendingResult == null) {
        this.pendingResult = result;
        Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        activity.startActivityForResult(i, PICK_CONTACT);
      } else {
        Log.v("ContactPickerPlugin", "Multiple requests may have been made");
      }
    } else {
      result.success(null);
    }
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != PICK_CONTACT) {
      return false;
    }
    if (resultCode != RESULT_OK) {
      if(pendingResult == null){
        return true;
      }
      pendingResult.success(null);
      pendingResult = null;
      return true;
    }
    Uri contactUri = data.getData();
    try{
      Cursor cursor = activity.getContentResolver().query(contactUri, null, null, null, null);
      if(cursor == null){
        pendingResult.success(null);
        pendingResult = null;
        return true;
      } else {
        cursor.moveToFirst();

        int phoneType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
        String customLabel = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
        String label = (String) ContactsContract.CommonDataKinds.Email.getTypeLabel(activity.getResources(), phoneType, customLabel);
        String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        String fullName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

        HashMap<String, Object> phoneNumber = new HashMap<>();
        phoneNumber.put("number", number);
        phoneNumber.put("label", label);

        HashMap<String, Object> contact = new HashMap<>();
        contact.put("fullName", fullName);
        contact.put("phoneNumber", phoneNumber);

        pendingResult.success(contact);
        pendingResult = null;
      }
    } catch (Exception e){
      e.printStackTrace();
    } finally {
      if(pendingResult != null){
        pendingResult.success(null);
        pendingResult = null;
      }
    }
    return true;
  }
}
