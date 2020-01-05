package com.xxun.watch.stepcountservices;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import java.util.Date;

import com.xiaoxun.sdk.ResponseData;
import com.xiaoxun.sdk.IResponseDataCallBack;
import com.xiaoxun.sdk.XiaoXunNetworkManager;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.util.Set;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by zhangjun5 on 2017/10/25.
 */

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MyTag", "onclock:"+intent.getAction());
		StepsCountUtils.sdcardLog("receive onclock:"+intent.getAction());
		String action = intent.getAction();
        if(action.equals(Const.STEPS_COUNT_ALARM_FLAGS)){
            Intent intent1 = new Intent(Const.STEPS_COUNT_ALARM_FLAGS);
            //定义一个PendingIntent对象，PendingIntent.getBroadcast包含了sendBroadcast的动作。
            //也就是发送了action 为"ELITOR_CLOCK"的intent
            PendingIntent pi = PendingIntent.getBroadcast(context,0,intent1,0);
            AlarmManager am = (AlarmManager)context.getSystemService(ALARM_SERVICE);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.e("sdk_int",Build.VERSION.SDK_INT+"" );
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 60*60*1000, pi);
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 60*60*1000, pi);
            }
            StepsCountUtils.initSensor(context,"0");
        }else if(action.equals(Const.ACTION_BROAST_BOOT_START)){
			StepsCountUtils.sdcardLog("RECEIVE FOR BOOT START:"+Const.ACTION_BROAST_BOOT_START);
			Intent _intent = new Intent(context,StepsCountService.class);
            context.stopService(_intent);
            context.startService(_intent);
	    	Log.e("RECEIVER FOR BOOT START","start watch boot!");
		}else if(action.equals("brocast.action.step.current.noti")) {
			StepsCountUtils.initSensor(context,"1");

		}else if(action.equals(Const.ACTION_BROAST_SENSOR_STEPS)){
            String sensorSteps = intent.getStringExtra("sensor_steps");
            String sensorType = intent.getStringExtra("sensor_type");
            String sensorTimeStamp = intent.getStringExtra("sensor_timestamp");
            final stepsRanksApp mApp = (stepsRanksApp) context.getApplicationContext();
	    	final XiaoXunNetworkManager nerservice = (XiaoXunNetworkManager)context.getSystemService
					("xun.network.Service");

            //判断是否是新的一天 1：更新达标标志位  2：发送前一天的计步数据（40111）  log
            boolean isYestoryData = StepsCountUtils.getPhoneStepsStatueByFirstSteps(context, sensorSteps);
            if(isYestoryData){
                mApp.setStep_half_flag(false);
                mApp.setStep_compele_flag(false);
                mApp.setStep_ranks_upload_flag(false);
                mApp.setStep_yesterday_upload_flag(false);
				String stepVaule = StepsCountUtils.getTimeStampLocal()+"_0";
				mApp.setValue(Const.STEPS_HALF_FLAG,stepVaule);
				mApp.setValue(Const.STEPS_COMPELE_FLAG,stepVaule);
				mApp.setValue(Const.STEPS_RANKS_UPLOAD_FLAG,stepVaule);
				mApp.setValue(Const.STEPS_YESTERDAY_UPLOAD_FLAG,stepVaule);
				StepsCountUtils.sdcardLog("STEP VAULE:"+stepVaule);
            }
			if(!mApp.getStep_yesterday_upload_flag() &&
					(StepsCountUtils.compareTimeHourDiff(new Date(),"0930") || sensorType.equals("1"))){
				String oldData = StepsCountUtils.getOldDataFromLocal(context,Const.PREF_STEPS_FILE);
				try {
					net.minidev.json.JSONObject jsonObject = (net.minidev.json.JSONObject) JSONValue.parse(oldData);
					Set<String> keys = jsonObject.keySet();
					net.minidev.json.JSONObject sendJson = new net.minidev.json.JSONObject();
					for(String key:keys){
						String sendKey = StepsCountUtils.getStepsKeyByData(nerservice.getWatchEid(),
								key.substring(0,8));
						net.minidev.json.JSONObject stepJson = new net.minidev.json.JSONObject();
						stepJson.put("Steps",Integer.valueOf((String)jsonObject.get(key)));
						sendJson.put(sendKey,stepJson);
					}
					StepsCountUtils.sdcardLog("oldData Upload:"+sendJson+nerservice.isWebSocketOK());
					if(sendJson.size() == 0){
						Log.e("stepsRanksApp:"," senddata size() = 0");
					}else{
						String sendData = mApp.obtainCloudMsgContent(
								Const.CID_STEPS_YESTERDAY_UPLOAD,nerservice.getMsgSN(),nerservice.getSID(),sendJson).toJSONString();
						Log.e("stepsRanksApp(40111):"," senddata:"+sendData+":"+sendJson.size());
						nerservice.paddingSendJsonMessage(sendData,
								new YestoryDataCallBack(context)
						);
					}
				}catch (Exception e){
					Log.e("oldData error:",e.toString());
				}
			}
            //获取当前步数，需要在计算前一天数据之前才可以
	    	String saveFirstPref = StepsCountUtils.getStringValue(context,Const.PREF_STEPS_FILE,Const.SHARE_PREF_PHONT_STEPS_NEW, "0");
            final String curSteps = StepsCountUtils.getPhoneStepsByFirstSteps(context, sensorSteps);
	    	StepsCountUtils.sdcardLog("RECEIVE FOR BOOT START:"+saveFirstPref+":"+curSteps+":"+sensorSteps);
	    	if(sensorType.equals("1")){//830 current Steps Upload
				JSONObject sendJson = new JSONObject();
				sendJson.put(Const.KEY_NAME_TGID, nerservice.getWatchGid());
				sendJson.put(Const.KEY_NAME_CUR_STEPS, StepsCountUtils.getTimeStampLocal()+"_"+curSteps);
				String sendData = mApp.obtainCloudMsgContent(
						Const.CID_STEPS_VALUE,nerservice.getMsgSN(),nerservice.getSID(),sendJson).toJSONString();
				Log.e("stepsRanksApp:"," senddata:"+sendData);
				StepsCountUtils.sdcardLog("sendData:"+sendData);
				nerservice.sendJsonMessage(sendData,
						new CurrentDataCallBack(context)
				);
				return;
			}


            //休眠时段之前，发送计步数据，用于第二天夜里2点计算第一天的地区和全国排名
			try{
				String sleepTime = mApp.getStringValue(Const.SLEEP_LIST,"{}");
				net.minidev.json.JSONObject jsonObject = (net.minidev.json.JSONObject) JSONValue.parse(sleepTime);
				String sleepOnOff = (String) jsonObject.get("onoff");
				String sleepStartHour = (String) jsonObject.get("starthour");
				Log.e("sleep:",sleepOnOff+":"+sleepStartHour);
				StepsCountUtils.sdcardLog("sleep:"+sleepOnOff+":"+sleepStartHour);
				if(sleepOnOff == null){
				sleepOnOff = "1";
				}
				if(sleepStartHour == null){
				sleepStartHour = "21";
				}
				int sleepStart = Integer.valueOf(sleepStartHour);
				int sendStepsData = 0;
				if(sleepOnOff.equals("0") || sleepStart == 0){
					sleepStart = 24;
				}
				sendStepsData = sleepStart - 12;
				if(sendStepsData <= 0){
					sendStepsData = 22;
				}else if(sendStepsData > 0){
					sendStepsData+=10;
				}
				String curTime = StepsCountUtils.getTimeStampLocal();
				final String hourTime = curTime.substring(8,10);
				Log.e("hour:",hourTime+":"+curTime+":"+sendStepsData);
				if(sendStepsData <= Integer.valueOf(hourTime) && Integer.valueOf(hourTime) <= sleepStart && !mApp.getStep_ranks_upload_flag()){
					Log.e("curSteps:", curSteps);
					net.minidev.json.JSONObject sendJson = new net.minidev.json.JSONObject();
					sendJson.put(Const.KEY_NAME_TGID, nerservice.getWatchEid());
					sendJson.put(Const.KEY_NAME_CUR_STEPS, StepsCountUtils.getTimeStampLocal()+"_"+curSteps);
					String sendData = mApp.obtainCloudMsgContent(
							Const.CID_STEPS_VALUE,nerservice.getMsgSN(),nerservice.getSID(),sendJson).toJSONString();
					Log.e("stepsRanksApp:"," senddata:"+sendData);
					StepsCountUtils.sdcardLog("hour:"+hourTime+":"+curTime+":"+sendStepsData+"sendData:"+sendData);
					nerservice.paddingSendJsonMessage(sendData,
							new RanksDataCallBack(context)
					);
				}
			}catch (Exception e){
						Log.e("oldData error:",e.toString());
			}

            //判断计步达标信息
            String phoneStepsPref = StepsCountUtils.getStringValue(context,Const.PREF_STEPS_FILE,
                    Const.SHARE_PREF_PHONT_STEPS_NEW, "0");
            //达标步数发送通知信息
            final String targetSteps = mApp.getStringValue(Const.STEPS_TARGET_LEVEL,"8000");
            Log.e("phoneSteps:",curSteps+":"+phoneStepsPref+
                    ":"+targetSteps+":"+mApp.getStep_half_flag()+":"+mApp.getStep_compele_flag());
            StepsCountUtils.sdcardLog("phoneStepspref:"+phoneStepsPref+"/////phoneSteps:"+curSteps
            +":"+targetSteps+":"+mApp.getStep_half_flag()+":"+mApp.getStep_compele_flag());
            if( ((Integer.valueOf(curSteps) > (Integer.valueOf(targetSteps)/2)
                    && Integer.valueOf(curSteps) < Integer.valueOf(targetSteps) && !mApp.getStep_half_flag())
                    ||(Integer.valueOf(curSteps) > Integer.valueOf(targetSteps) && !mApp.getStep_compele_flag()))
                    && nerservice.isLoginOK()){
                if(mApp.getStep_compele_flag()){
                    return ;
                }

                try {
                    Log.e("curSteps:", curSteps);
                    net.minidev.json.JSONObject sendJson = new net.minidev.json.JSONObject();
                    sendJson.put(Const.KEY_NAME_TGID, nerservice.getWatchGid());

                    StringBuilder key = new StringBuilder("GP/");
                    key.append(nerservice.getWatchGid());
                    key.append("/MSG/");
                    key.append(mApp.getReversedOrderTime());
                    sendJson.put(Const.KEY_NAME_KEY, key.toString());

                    net.minidev.json.JSONObject sendList = new net.minidev.json.JSONObject();
                    sendList.put(Const.KEY_NAME_EID, nerservice.getWatchEid());
                    sendList.put(Const.KEY_NAME_TYPE, "steps");
                    sendList.put(Const.KEY_NAME_CONTENT, curSteps+"_"+targetSteps);
                    sendList.put(Const.KEY_NAME_DURATION, 100);
                    sendJson.put("Value",sendList);
                    String sendData = mApp.obtainCloudMsgContent(
                            Const.CID_STEPS_UPLOAD,nerservice.getMsgSN(),nerservice.getSID(),sendJson).toJSONString();
                    Log.e("stepsRanksApp:"," senddata:"+sendData);
                    StepsCountUtils.sdcardLog("stepsRanksApp: (load target Steps)"+sendData);
                    nerservice.paddingSendJsonMessage(sendData,
                            new CompelePlanDataCallBack(context,curSteps,targetSteps)
                    );

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

	private class CurrentDataCallBack extends IResponseDataCallBack.Stub{

		private Context context;

		public CurrentDataCallBack(Context context){
			this.context = context;
		}

		@Override
		public void onSuccess(ResponseData responseData) {
			try{
				Log.e("MyReceiver","responseData:"+responseData.toString());
				StepsCountUtils.sdcardLog("responseData:"+responseData.toString());
				JSONObject jsonData = (JSONObject)JSONValue.parse(responseData.getResponseData());
				int responRc = (int)jsonData.get("RC");
				if(responRc == 1){

				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		@Override
		public void onError(int i, String s) {
			Log.e("MyReceiver","onError"+i+":"+s);
		}

	}

    private class YestoryDataCallBack extends IResponseDataCallBack.Stub{

	    private Context context;
            
	    public YestoryDataCallBack(Context context){
		this.context = context;
	    }

       	    @Override
	    public void onSuccess(ResponseData responseData) {
		try{
			Log.e("MyReceiver","responseData:"+responseData.toString());
			StepsCountUtils.sdcardLog("responseData:"+responseData.toString());
			JSONObject jsonData = (JSONObject)JSONValue.parse(responseData.getResponseData());
			int responRc = (int)jsonData.get("RC");
			if(responRc == 1){		    
			    StepsCountUtils.clearOldDataToLocal(context, Const.PREF_STEPS_FILE);
			    stepsRanksApp mApp = (stepsRanksApp)context.getApplicationContext();
			    mApp.setStep_yesterday_upload_flag(true);	
			    String stepVaule = StepsCountUtils.getTimeStampLocal()+"_1";
			    mApp.setValue(Const.STEPS_YESTERDAY_UPLOAD_FLAG,stepVaule);				

			}
		 }catch(Exception e){
                    e.printStackTrace();
                }
	    }

	    @Override
	    public void onError(int i, String s) {
		Log.e("MyReceiver","onError"+i+":"+s);
	    }

    }

    private class RanksDataCallBack extends IResponseDataCallBack.Stub{

	    private Context context;
            
	    public RanksDataCallBack(Context context){
		this.context = context;
	    }	

       	    @Override
	    public void onSuccess(ResponseData responseData) {
		try{
			Log.e("MyReceiver","responseData:"+responseData.toString());
			StepsCountUtils.sdcardLog("MyReceiver responseData:"+responseData.toString());
			JSONObject jsonData = (JSONObject)JSONValue.parse(responseData.getResponseData());
			int responRc = (int)jsonData.get("RC");
			if(responRc == 1){
	 	     	    stepsRanksApp mApp = (stepsRanksApp)context.getApplicationContext();
			    String stepVaule = StepsCountUtils.getTimeStampLocal()+"_1";
			    mApp.setValue(Const.STEPS_RANKS_UPLOAD_FLAG,stepVaule);
			}
		}catch(Exception e){
                    e.printStackTrace();
                }
	    }

	    @Override
	    public void onError(int i, String s) {
		Log.e("MyReceiver","onError"+i+":"+s);
	    }

    }

    private class CompelePlanDataCallBack extends IResponseDataCallBack.Stub{

	    private Context context;
	    private String  curSteps;
	    private String  targetSteps;
            
	    public CompelePlanDataCallBack(Context context,String curstep,String targetstep){
		this.context = context;
 		this.curSteps = curstep;
		this.targetSteps = targetstep;
	    }
		
       	    @Override
	    public void onSuccess(ResponseData responseData) {
		try{
			Log.e("MyReceiver","responseData:"+responseData.toString());
			StepsCountUtils.sdcardLog("MyReceiver responseData:"+responseData.toString());
			JSONObject jsonData = (JSONObject)JSONValue.parse(responseData.getResponseData());
			int responRc = (int)jsonData.get("RC");
			if(responRc == 1){
			    stepsRanksApp mApp = (stepsRanksApp)context.getApplicationContext();
			    if(Integer.valueOf(curSteps) > (Integer.valueOf(targetSteps)/2)
			       && Integer.valueOf(curSteps) < Integer.valueOf(targetSteps)){
			 	mApp.setStep_half_flag(true);
				String stepVaule = StepsCountUtils.getTimeStampLocal()+"_1";
				mApp.setValue(Const.STEPS_HALF_FLAG,stepVaule);
			   }
			   if(Integer.valueOf(curSteps) > Integer.valueOf(targetSteps)){
				mApp.setStep_compele_flag(true);
				String stepVaule = StepsCountUtils.getTimeStampLocal()+"_1";
				mApp.setValue(Const.STEPS_COMPELE_FLAG,stepVaule);
			   }

			}
		}catch(Exception e){
                    e.printStackTrace();
                }
	    }

	    @Override
	    public void onError(int i, String s) {
		Log.e("MyReceiver","onError"+i+":"+s);
	    }

    }

}
