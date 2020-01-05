package com.xxun.watch.stepcountservices;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.database.ContentObserver;
import android.os.Handler;

import com.xiaoxun.sdk.ResponseData;
import com.xiaoxun.sdk.IResponseDataCallBack;
import com.xiaoxun.sdk.XiaoXunNetworkManager;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class StepsCountService extends Service {

    private XiaoXunNetworkManager nerservice;
    private stepsRanksApp mApp;

    public StepsCountService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("sercice","onCreate");

        nerservice = (XiaoXunNetworkManager)getSystemService("xun.network.Service");
        mApp = (stepsRanksApp)getApplication();
    //	getMapGetValue();
        String targetLevel = android.provider.Settings.System.getString(getContentResolver(),"steps_target_level");
        if(targetLevel== null || targetLevel.equals("")){
            targetLevel = "8000";
        }
        if(targetLevel != null){
            mApp.setValue(Const.STEPS_TARGET_LEVEL,targetLevel);
        }

        String sleepTime = android.provider.Settings.System.getString(getContentResolver(),"SleepList");
        StepsCountUtils.sdcardLog("sleepTime:"+sleepTime);
        Log.e("sleepTime",sleepTime+":stepService");
        if(sleepTime == null || sleepTime.equals("")){
            sleepTime = "{}";
        }
        if(sleepTime != null){
            mApp.setValue(Const.SLEEP_LIST, sleepTime);
        }

        //创建Intent对象
        Intent intent = new Intent(Const.STEPS_COUNT_ALARM_FLAGS);
        //定义一个PendingIntent对象，PendingIntent.getBroadcast包含了sendBroadcast的动作。
        //也就是发送了action 为"ELITOR_CLOCK"的intent
        PendingIntent pi = PendingIntent.getBroadcast(this,0,intent,0);

        //AlarmManager对象,注意这里并不是new一个对象，Alarmmanager为系统级服务
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);

        //设置闹钟从当前时间开始
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10*1000, pi);
        } else {
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10*1000, pi);
        }

        RegisterTargetStepObserve();
        RegisterSleepListStepObserve();
    }

    private void RegisterTargetStepObserve(){
        this.getContentResolver().registerContentObserver(
                android.provider.Settings.System.getUriFor("steps_target_level"),
                true, mTargetObserver);
    }

    final private ContentObserver mTargetObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            String targetLevel = android.provider.Settings.System.getString(getContentResolver(),"steps_target_level");
            Log.d("target change", targetLevel);
            if(targetLevel != null){
                mApp.setValue(Const.STEPS_TARGET_LEVEL,targetLevel);
            }
        }
    };

    private void RegisterSleepListStepObserve(){
        this.getContentResolver().registerContentObserver(
                android.provider.Settings.System.getUriFor("SleepList"),
                true, mSleepTimeObserver);
    }

    final private ContentObserver mSleepTimeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            String mSleepList = android.provider.Settings.System.getString(getContentResolver(),"SleepList");
            Log.d("target change", mSleepList);
            if(mSleepList != null){
                mApp.setValue(Const.SLEEP_LIST, mSleepList);
            }
        }
    };


    private void getMapGetValue(){
        JSONObject pl = new JSONObject();
        JSONArray plKeyList = new JSONArray();
        plKeyList.add(Const.STEPS_TARGET_LEVEL);
        plKeyList.add(Const.SLEEP_LIST);

        pl.put(Const.KEY_NAME_EID, nerservice.getWatchEid());
        pl.put(Const.KEY_NAME_KEYS, plKeyList);
        String sendData = mApp.obtainCloudMsgContent(
                Const.CID_MAPGET_MGET,nerservice.getMsgSN(),nerservice.getSID(),pl).toJSONString();
        Log.e("mainActivity"," senddata:mepget:"+sendData);
        nerservice.sendJsonMessage(sendData,
                new ResponseDataCallBack());
    }

    private class ResponseDataCallBack extends IResponseDataCallBack.Stub{

       	    @Override
	    public void onSuccess(ResponseData responseData) {
		Log.e("MainActivity","responseData:"+responseData.toString());
		JSONObject jsonData = (JSONObject)JSONValue.parse(responseData.getResponseData());
		int responRc = (int)jsonData.get("RC");
		if(responRc == 1){
		    JSONObject pl = (JSONObject) jsonData.get("PL");
		    String targetLevel = (String)pl.get(Const.STEPS_TARGET_LEVEL);
		    if(targetLevel != null){
		        mApp.setValue(Const.STEPS_TARGET_LEVEL,targetLevel);
		    }
		    String sleepTime = (String)pl.get(Const.SLEEP_LIST);
		    if(sleepTime != null){
		        mApp.setValue(Const.SLEEP_LIST, sleepTime);
		    }

		}
	    }

	    @Override
	    public void onError(int i, String s) {
		Log.e("MainActivity","onError"+i+":"+s);
	    }

    }

}
