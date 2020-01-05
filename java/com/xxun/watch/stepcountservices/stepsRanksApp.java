package com.xxun.watch.stepcountservices;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zhangjun5 on 2017/9/15.
 */

public class stepsRanksApp extends Application {
    private final String TAG = "stepsRanksApp";
    private boolean step_half_flag = false ;  //一半的达标的通知标志
    private boolean step_compele_flag = false ;//全部达标的通知标志
    private boolean step_ranks_upload_flag  = false;//休眠时段前上传数据成功标志
    private boolean step_yesterday_upload_flag = false;//昨天及之前的数据上传成功标志

    public void setStep_half_flag(boolean flag){
        step_half_flag = flag;
    }
    public void setStep_compele_flag(boolean flag){
        step_compele_flag = flag;
    }
    public void setStep_ranks_upload_flag(boolean flag){
        step_ranks_upload_flag = flag;
    }
    public void setStep_yesterday_upload_flag(boolean flag){
        step_yesterday_upload_flag = flag;
    }
    public boolean getStep_half_flag(){
        return step_half_flag;
    }
    public boolean getStep_compele_flag(){
        return step_compele_flag;
    }
    public boolean getStep_ranks_upload_flag(){
        return step_ranks_upload_flag;
    }
    public boolean getStep_yesterday_upload_flag(){
        return step_yesterday_upload_flag;
    }

    @Override
    public void onCreate() {
        super.onCreate();
	initStepsFlag();
    }

    private void initStepsFlag(){
	String stepHalf = getStringValue(Const.STEPS_HALF_FLAG,"0");
	String stepComp = getStringValue(Const.STEPS_COMPELE_FLAG,"0");
	String stepRankupload = getStringValue(Const.STEPS_RANKS_UPLOAD_FLAG,"0");
	String stepYesUpload = getStringValue(Const.STEPS_YESTERDAY_UPLOAD_FLAG,"0");

	step_half_flag = stepStateValue(stepHalf);
	step_compele_flag = stepStateValue(stepComp);
	step_ranks_upload_flag = stepStateValue(stepRankupload);
	step_yesterday_upload_flag = stepStateValue(stepYesUpload);
    }   

    private boolean stepStateValue(String stepValue){
	boolean retValue = false;
	if(stepValue.equals("0")){
		retValue = false;
	}
	String strArray[] = stepValue.split("_");

	if(strArray.length == 2 && StepsCountUtils.compareTodayToLastInfo(strArray[0])){
		if(strArray[1].equals("0")){
			retValue = false;
		}else{
			retValue = true;
		}
	}
	return retValue;	
    }
    
    public static JSONObject obtainCloudMsgContent(int cid, int sn, String sid, Object pl) {
        JSONObject msg = new JSONObject();
        msg.put("Version", "00190000");
        msg.put("CID", cid);
        if (sid != null) {
            msg.put(Const.KEY_NAME_SID, sid);
        }
        msg.put("SN", sn);
        if (pl != null) {
            msg.put("PL", pl);
        }
        return msg;
    }

    public void setValue(String key, String value) {
        final SharedPreferences preferences = getSharedPreferences(Const.PREF_STEPS_FILE, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();
    }
    public String getStringValue(String key, String defValue) {
        String str = getSharedPreferences(Const.PREF_STEPS_FILE, Context.MODE_PRIVATE )
                .getString(key, defValue);
        return str;
    }
    public static String getReversedOrderTime() {
        StringBuilder timeStamp = new StringBuilder();
        String test = null;
        test = getTimeStamp();

        timeStamp.append(String.format("%1$08d", Const.YMD_REVERSED_MASK_8 - Integer.parseInt(test.substring(0, 8))));
        timeStamp.append(String.format("%1$09d", Const.HMSS_REVERSED_MASK_9 - Integer.parseInt(test.substring(8, 17))));
        return timeStamp.toString();
    }
    public static String getTimeStamp() {
        Date d = new Date();
        DateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        return format.format(d).toString();
    }

}
