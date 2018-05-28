/*******************************************************************************
 * Copyright (c) 1999, 2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 */
package com.hongyun.cordova.plugin.messagenotify;

import android.content.Intent;

import android.media.MediaPlayer;

import org.apache.cordova.PluginResult;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.os.Bundle;

import android.util.Log;


import com.iyanbian.app.R;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MessageNotify extends CordovaPlugin{


    private  MqttAndroidClient mqttAndroidClient;
    private String serverUri;
    private String clientId;
    private String subscriptionTopic;

    private String AutomaticReconnect;
    private String CleanSession;
    private String KeepAliveInterval;
    private String ConnectionTimeout;
    private String account;
    private String password;
    private MediaPlayer mediaPlayer = null;

    private String activityClass;

    private String logTag = "MessageNotify";

    //连接服务器回调
    private CallbackContext callbackContext;

    //当收到消息后
    private CallbackContext messageListCall = null;

    //程序再次启动的回调
    public static CallbackContext notifiClick = null;


    private HashMap<String,String> topicMap = new HashMap<String, String>();


    public void connect(CordovaArgs args){

        JSONObject orderInfoArgs = null;

        try {
            orderInfoArgs = args.getJSONObject(0);
            serverUri = orderInfoArgs.getString("serverUri");
            account   = orderInfoArgs.getString("account");
            password  = orderInfoArgs.getString("password");
            clientId  = orderInfoArgs.getString("clientId");
            AutomaticReconnect = orderInfoArgs.getString("AutomaticReconnect");
            CleanSession = orderInfoArgs.getString("CleanSession");
            ConnectionTimeout = orderInfoArgs.getString("ConnectionTimeout");
            KeepAliveInterval = orderInfoArgs.getString("KeepAliveInterval");

        } catch (JSONException e) {
            e.printStackTrace();
        }



        mqttAndroidClient = new MqttAndroidClient(cordova.getActivity().getApplicationContext(), serverUri, clientId);

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.i(logTag, "mqttAndroidClient.MqttCallbackExtended.connectComplete");
                if(reconnect){
                    reSubscribe();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.i(logTag, "mqttAndroidClient.MqttCallbackExtended.connectionLost :" + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(logTag, "mqttAndroidClient.MqttCallbackExtended.messageArrived : " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i(logTag, "mqttAndroidClient.MqttCallbackExtended.deliveryComplete");
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(Boolean.parseBoolean(AutomaticReconnect));
        mqttConnectOptions.setCleanSession(Boolean.parseBoolean(CleanSession));
        mqttConnectOptions.setConnectionTimeout(Integer.parseInt((ConnectionTimeout)));
        mqttConnectOptions.setKeepAliveInterval(Integer.parseInt((KeepAliveInterval)));
        mqttConnectOptions.setUserName(account);
        mqttConnectOptions.setPassword(password.toCharArray());
        try {
            mqttAndroidClient.connect(mqttConnectOptions, cordova.getActivity().getApplicationContext(), new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    Log.i(logTag, "mqttAndroidClient.IMqttActionListener.onSuccess");
                    callbackContext.success();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(logTag, "mqttAndroidClient.IMqttActionListener.onFailure : " +exception.getMessage());
                    callbackContext.error(exception.getMessage());
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    public void disconnect(CordovaArgs args){
        try {
            mqttAndroidClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(CordovaArgs args){

        JSONObject orderInfoArgs = null;

        try {

            orderInfoArgs = args.getJSONObject(0);

            subscriptionTopic = orderInfoArgs.getString("subscriptionTopic");

        } catch (JSONException e) {

            e.printStackTrace();

        }

        subscriptionTopic = saveGetTopic(subscriptionTopic);

        subscribeByStr(subscriptionTopic);
    }

    public void unsubscribe(CordovaArgs args){
        JSONObject orderInfoArgs = null;
        try {
            orderInfoArgs = args.getJSONObject(0);
            subscriptionTopic = orderInfoArgs.getString("subscriptionTopic");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            mqttAndroidClient.unsubscribe(subscriptionTopic, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(logTag, "mqttAndroidClient.unsubscribe.onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(logTag, "mqttAndroidClient.unsubscribe.onFailure : " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        // save the current callback context
       this.callbackContext = callbackContext;
        if (action.equals("connect")) {
            connect(args);
        }else if(action.equals("disconnect")){
            disconnect(args);
        }else if(action.equals("subscribe")){
            subscribe(args);
        }else if(action.equals("unsubscribe")){
            unsubscribe(args);
        }else if(action.equals("onmessage")){
            messageListCall =callbackContext;
        }else if(action.equals("notifyclick")){
            notifiClick =callbackContext;
        }
        return true;
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mediaPlayer = null;
        activityClass = "com.iyanbian.app.MainActivity";
        //start(null);
    }

    public MediaPlayer createLocalMp3(){
        /**
         * 创建音频文件的方法：
         * 1、播放资源目录的文件：MediaPlayer.create(MainActivity.this,R.raw.beatit);//播放res/raw 资源目录下的MP3文件
         * 2:播放sdcard卡的文件：mediaPlayer=new MediaPlayer();
         *   mediaPlayer.setDataSource("/sdcard/beatit.mp3");//前提是sdcard卡要先导入音频文件
         */

        //cordova.getActivity().findViewById(android.R.raw.hit);
       // MediaPlayer mp=MediaPlayer.create(this,R.raw.hint);
        MediaPlayer mp= MediaPlayer.create(cordova.getActivity(),R.raw.hint);
        mp.stop();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mp) {
                    mp.release();//释放音频资源
                    mediaPlayer = null;

            }
         });
        return mp;
    }

    public static void notifiClickStart(String str){

        JSONObject json_test = null;
        if(str == null)return;
        if(str.isEmpty()) return;
        try {
            json_test = new JSONObject(str);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            JSONObject jsonData = json_test;//getJsonObjectResult(result);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonData);
            pluginResult.setKeepCallback(true);

            if (notifiClick != null) {
                notifiClick.sendPluginResult(pluginResult);
            }
        }catch ( Exception ex){

        }
    }


    //为了在,再次连接的时候重新订阅这些主题
    private String saveGetTopic(String str){
        String result = "";
        if(topicMap.containsKey(str)){
            result = topicMap.get(str);
        }else{
            topicMap.put(str,str);
            result = str;
        }
        return result;
    }

    private void reSubscribe(){
        for (Map.Entry<String, String> entry : topicMap.entrySet()) {
            subscribeByStr(entry.getValue());
        }
    };

    private void subscribeByStr(String str){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken){
                    Log.i(logTag, "mqttAndroidClient.IMqttActionListener.Subscribed");
                    callbackContext.success();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(logTag, "mqttAndroidClient.IMqttActionListener.onFailure :" + exception.getMessage());
                    callbackContext.error(exception.getMessage());
                }
            });

            mqttAndroidClient.subscribe(str, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    if(mediaPlayer==null) {
                        mediaPlayer = createLocalMp3();
                        mediaPlayer.prepare();
                    }
                    if (mediaPlayer.isPlaying()){

                    }else{
                        mediaPlayer.start();
                    }

                    String mes = new String(message.getPayload());

                    JSONObject json_test = new JSONObject(mes);

                    String title ="默认标题";
                    String content = "默认内容";
                    String jso = "默认json";

                    try{
                        title = json_test.getString("title");
                        content = json_test.getString("content");
                        jso = json_test.getString("data").toString();
                    }catch(Exception ex) {

                    }

                    Intent intent = new Intent();
                    intent.setClassName(cordova.getActivity().getApplication(), activityClass);
                    Bundle bundle = new Bundle();
                    bundle.putString("data", mes);
                    //这个地方是可以传递消息信息的
                    intent.putExtras(bundle);


                    com.hongyun.cordova.plugin.messagenotify.Notify.notifcation(cordova.getActivity(),
                            content,
                            intent,
                            title);

                    if(messageListCall != null) {
                        JSONObject jsonData = json_test ;//getJsonObjectResult(result);
                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonData);
                        pluginResult.setKeepCallback(true);
                        messageListCall.sendPluginResult(pluginResult);
                    }
                }
            });

        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }
}
