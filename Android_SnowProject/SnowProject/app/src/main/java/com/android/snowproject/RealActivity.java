package com.android.snowproject;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.android.snowproject.ui.apicall.GetThingShadow;
import com.android.snowproject.ui.apicall.UpdateShadow;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;


public class RealActivity extends AppCompatActivity {
    final static String TAG = "SnowProject";

    String urlStr1;

    Timer timer;

    TextView weather_text;
    TextView home_text;
    String weather_data;
    String home_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real);

        urlStr1 = "https://kbxxyomk13.execute-api.ap-northeast-2.amazonaws.com/prod/devices/SnowProject";

        weather_text = (TextView)findViewById(R.id.weather);
        home_text = (TextView)findViewById(R.id.home);

        timer = new Timer();
        timer.schedule(new TimerTask() {
                           @Override
                           public void run() {
                               new GetThingShadow(RealActivity.this, urlStr1).execute();
                           }
                       },
                0,2000);


        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                weather_data= getWeatherXmlData(); //아래 메소드를 호출하여 XML data를 파싱해서 String 객체로 얻어오기
                home_data = getHomeXmlData();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        weather_text.setText(weather_data); //TextView에 문자열  data 출력
                        home_text.setText(home_data);
                    }
                });

            }
        }).start();


        Button onBtn = findViewById(R.id.onBtn);
        onBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject payload = new JSONObject();

                try {
                    JSONArray jsonArray = new JSONArray();
                    String ledbuzzer_input = "ON";
                    if (ledbuzzer_input != null && !ledbuzzer_input.equals("")) {
                        JSONObject tag2 = new JSONObject();
                        tag2.put("tagName", "LED");
                        tag2.put("tagValue", ledbuzzer_input);

                        jsonArray.put(tag2);
                    }

                    if (jsonArray.length() > 0)
                        payload.put("tags", jsonArray);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONEXception");
                }
                Log.i(TAG,"payload="+payload);
                if (payload.length() >0 )
                    new UpdateShadow(RealActivity.this,urlStr1).execute(payload);
                else
                    Toast.makeText(RealActivity.this,"변경할 상태 정보 입력이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        });

        Button offBtn = findViewById(R.id.offBtn);
        offBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject payload = new JSONObject();
                try {
                    JSONArray jsonArray = new JSONArray();
                    String ledbuzzer_input = "OFF";
                    if (ledbuzzer_input != null && !ledbuzzer_input.equals("")) {
                        JSONObject tag2 = new JSONObject();
                        tag2.put("tagName", "LED");
                        tag2.put("tagValue", ledbuzzer_input);

                        jsonArray.put(tag2);
                    }

                    if (jsonArray.length() > 0)
                        payload.put("tags", jsonArray);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONEXception");
                }
                Log.i(TAG,"payload="+payload);
                if (payload.length() >0 )
                    new UpdateShadow(RealActivity.this,urlStr1).execute(payload);
                else
                    Toast.makeText(RealActivity.this,"변경할 상태 정보 입력이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    String getHomeXmlData(){
        StringBuffer buffer=new StringBuffer();

        String sigungu = "41287";
        String bjdong = "10600";
        String bun = "0506";
        String ji = "0000";
        String numOfRows = "1";
        String serviceKey = "RX7aR5ElWyrg2wMgs11xz87jTWd4fp6DCnx8YKeWWxIM0rvTOoIE6Kh4LbP0XXesm9MDj90DZk6pwfVjFb4shQ%3D%3D";

        String queryUrl = "http://apis.data.go.kr/1611000/BldRgstService/getBrTitleInfo?sigunguCd="+sigungu+"&bjdongCd="+bjdong
                +"&bun="+bun+"&ji="+ji+"&numOfRows="+numOfRows+"&ServiceKey="+serviceKey;

        try {
            URL url= new URL(queryUrl);//문자열로 된 요청 url을 URL 객체로 생성.
            InputStream is= url.openStream(); //url위치로 입력스트림 연결

            XmlPullParserFactory factory= XmlPullParserFactory.newInstance();
            XmlPullParser xpp= factory.newPullParser();
            xpp.setInput( new InputStreamReader(is, "UTF-8") ); //inputstream 으로부터 xml 입력받기

            String tag;
            xpp.next();
            int eventType= xpp.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT){
                switch(eventType){
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    case XmlPullParser.START_TAG:
                        tag= xpp.getName();//태그 이름 얻어오기

                        if(tag.equals("item")) ;
                        else if(tag.equals("newPlatPlc")) {
                            buffer.append("주소 : ");
                            xpp.next();
                            buffer.append(xpp.getText());
                            buffer.append("\n\n");
                        }
                        else if(tag.equals("mainPurpsCdNm")) {
                            buffer.append("용도 : ");
                            xpp.next();
                            buffer.append(xpp.getText());
                            buffer.append("\n\n");
                        }
                        else if(tag.equals("grndFlrCnt")) {
                            buffer.append("지상층수 : ");
                            xpp.next();
                            buffer.append(xpp.getText() +"층");
                            buffer.append("\n\n");
                        }
                        else if(tag.equals("ugrndFlrCnt")) {
                            buffer.append("지하층수 : ");
                            xpp.next();
                            buffer.append(xpp.getText() +"층");
                            buffer.append("\n\n");
                        }
                        else if(tag.equals("strctCdNm")) {
                            buffer.append("구조 : ");
                            xpp.next();
                            buffer.append(xpp.getText());
                            buffer.append("\n\n");
                        }
                        else if(tag.equals("etcRoof")) {
                            buffer.append("지붕 : ");
                            xpp.next();
                            buffer.append(xpp.getText());
                            buffer.append("\n\n");
                        }
                        break;
                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag= xpp.getName();
                        if(tag.equals("item")) buffer.append("\n");
                        break;
                }
                eventType= xpp.next();
            }

        } catch (Exception e) {
            // TODO Auto-generated catch blocke.printStackTrace();
        }
        return buffer.toString();//StringBuffer 문자열 객체 반환
    }


   String getWeatherXmlData(){
        StringBuffer buffer=new StringBuffer();

        String serviceKey = "RX7aR5ElWyrg2wMgs11xz87jTWd4fp6DCnx8YKeWWxIM0rvTOoIE6Kh4LbP0XXesm9MDj90DZk6pwfVjFb4shQ%3D%3D";
        String numOfRows = "7";
        String pageNo = "1";

        SimpleDateFormat real_time = new SimpleDateFormat( "yyyyMMdd");
        Date time = new Date();
        String base_date = real_time.format(time);

        String base_time = "0230";
        String nx = "56";
        String ny = "129";


       String queryUrl = "http://apis.data.go.kr/1360000/VilageFcstInfoService/getVilageFcst?serviceKey="+serviceKey+
               "&numOfRows="+numOfRows+"&pageNo="+pageNo+"&base_date="+base_date+"&base_time="+base_time+"&nx="+nx+"&ny="+ny;

        try {
            URL url= new URL(queryUrl);//문자열로 된 요청 url을 URL 객체로 생성.
            InputStream is= url.openStream(); //url위치로 입력스트림 연결

            XmlPullParserFactory factory= XmlPullParserFactory.newInstance();
            XmlPullParser xpp= factory.newPullParser();
            xpp.setInput( new InputStreamReader(is, "UTF-8") ); //inputstream 으로부터 xml 입력받기

            String tag;
            int i =0;
            xpp.next();
            int eventType= xpp.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT){
                switch(eventType){
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    case XmlPullParser.START_TAG:
                        tag= xpp.getName();//태그 이름 얻어오기

                        if(tag.equals("item")) ; // 첫번째 검색결과
                        else if(tag.equals("fcstValue")) {
                            i++;
                            if (i == 1) {
                                buffer.append("강수확률 : ");
                                xpp.next();
                                buffer.append(xpp.getText()+"%");
                                buffer.append("\n");
                            }
                            if (i == 2) {
                                buffer.append("강수형태 : ");
                                xpp.next();
                                if(xpp.getText().equals("0")){
                                    buffer.append("없음");
                                    buffer.append("\n");
                                }
                                else if(xpp.getText().equals("1")){
                                    buffer.append("비");
                                    buffer.append("\n");
                                }
                                else if(xpp.getText().equals("2")){
                                    buffer.append("비+눈(진눈개비)");
                                    buffer.append("\n");
                                }
                                else if(xpp.getText().equals("3")){
                                    buffer.append("눈");
                                    buffer.append("\n");
                                }
                                else if(xpp.getText().equals("4")){
                                    buffer.append("소나기");
                                    buffer.append("\n");
                                }
                                else if(xpp.getText().equals("5")){
                                    buffer.append("빗방울");
                                    buffer.append("\n");
                                }
                                else if(xpp.getText().equals("6")){
                                    buffer.append("빗방울+눈날림");
                                    buffer.append("\n");
                                }
                                else if(xpp.getText().equals("7")){
                                    buffer.append("눈날림");
                                    buffer.append("\n");
                                }
                            }
                            if(i==3){
                                buffer.append("6시간 강수량 : ");
                                xpp.next();
                                buffer.append(xpp.getText()+"mm");
                                buffer.append("\n");
                            }
                            if(i==4){
                                buffer.append("습도 : ");
                                xpp.next();
                                buffer.append(xpp.getText()+"%");
                                buffer.append("\n");
                            }
                            if(i==5){
                                buffer.append("6시간 신적설 : ");
                                xpp.next();
                                buffer.append(xpp.getText()+"cm");
                             //   buffer.append("\n");
                            }
                            if(i==7){
                                buffer.append("3시간 기온 : ");
                                xpp.next();
                                buffer.append(xpp.getText()+"℃");
                                buffer.append("\n");
                            }
                        }
                        break;

                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag= xpp.getName(); //태그 이름 얻어오기
                        if(tag.equals("item")) buffer.append("\n");
                        break;
                }
                eventType= xpp.next();
           }

        } catch (Exception e) {
            // TODO Auto-generated catch blocke.printStackTrace();
        }
        return buffer.toString();//StringBuffer 문자열 객체 반환
    }
}