package com.example.virture_fishing_rod;

import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class GameActivity extends AppCompatActivity implements SensorEventListener, TextToSpeech.OnInitListener {
    TextToSpeech tts;       // TTS 사용
    MediaPlayer player;     // 배경음악을 담당하는 객체
    Intent intent, intent2; // intent = LoadingActivity 에서 받은 인텐트, intent2 = TTS에 사용하는 RecognizerIntent
    SensorManager m,m2; Sensor sen,sen2; ImageView rod1,rod2,rod3,bupyo; // m = 방향, m2 = 가속도 , rod1~3 = 왼,중,오른쪽 낚싯대, bupyo = 부표
    SQLiteDatabase db;      // 물고기 정보 테이블 (이름 TEXT, 정보 TEXT)
    String Wonju = "", wether = ""; // HTML Parsing에 사용하는 문자열
    Handler h,h2;           // h = HTML Parsing 핸들러, h2 = 물고기 낚을 때 쓰는 Thread 핸들러
    WorkerThread a;         // 물고기 낚을 때 쓰는 Thread
    ImageView s;            // 배경화면 이미지 뷰
    TextView quiz, answer;  // quiz = 물고기 낚았을 때 등장하는 TextView , answer = 퀴즈 맞힌 갯수
    RelativeLayout quiz_back;   // 퀴즈 TextView(quiz)가 보이기 위한 Layout
    long old=0;                 // 낚싯대가 들어올려질때의 텀 (0.5s)
    int count=0;                // Thread 에서 사용하는 증감변수 = Random check와 매칭되는 순간 물고기가 낚인다는 판정
    boolean flag = false;       // 낚싯대가 이미 들려있는지..
    boolean rodcheck[] = new boolean[3];    // 어떤 낚싯대가 들려있는지..
    boolean isroding = false;   // 들린 상태에선 더이상 안움직이게..
    boolean isfishing = false;  // 낚는 중일때는 안움직이게..
    Locale locale;              // TTS 언어설정을 위한 객체
    Thread t = null;            // Thread
    Random rand = new Random(); // 랜덤변수 생성 시 사용 (10~15초 랜덤변수, 0~4 DB 물고기 인덱스)
    int check;
    int query = rand.nextInt(5);
    Cursor c;                   // RawQuery에 사용
    String info="<문제> \n\n", fishname;  // info = 물고기 정보, fishname = 물고기 이름
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        tts = new TextToSpeech(this,this);

        player = MediaPlayer.create(this,R.raw.splash02);
        player.setLooping(true);
        player.setVolume(0.2f,0.2f);
        player.start();

        for(int i=0; i<3; i++)
            rodcheck[i] = false;

        s = findViewById(R.id.iv2);
        rod1 = findViewById(R.id.iv3);
        rod2 = findViewById(R.id.iv4);
        rod3 = findViewById(R.id.iv5);
        bupyo = findViewById(R.id.iv6);
        quiz = findViewById(R.id.quiz);
        quiz_back = findViewById(R.id.quiz_back);
        answer = findViewById(R.id.answer);

        h = new Handler() {
            public void handleMessage(Message msg) {
                HTMLParsing();
            }
        };
        a = new WorkerThread(h);
        a.start();

        m = (SensorManager) getSystemService(SENSOR_SERVICE);
        sen = m.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        m2 = (SensorManager) getSystemService(SENSOR_SERVICE);
        sen2 = m2.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(sen==null){
            Toast.makeText(this,"방향센서 없음->프로그램 종료",Toast.LENGTH_LONG).show();
            finish();
        }
        m.registerListener(this,sen,SensorManager.SENSOR_DELAY_UI);
        m2.registerListener(this,sen2,SensorManager.SENSOR_DELAY_UI);

        dbHelper helper = new dbHelper(this);

        if(db == null) {
            db = helper.getWritableDatabase();
            db.execSQL("INSERT INTO 물고기 VALUES ('" + "가다랑어" + "','" + "고등어과의 어류 가운데 소형 종에 속한다. 몸은 굵고 통통한 방추형임" +
                    " 등은 청흑색, 배는 광택을 띤 은백색이고 그 위에 4~10줄의 검은 세로띠가 있는 것이 특징이다." + "')");
            db.execSQL("INSERT INTO 물고기 VALUES ('" + "갈치" + "','" + "광택이 나는 은백색을 띠며 등지느러미는 연한 황록색을 띤다." +
                    " 눈이 머리에 비해 큰 편이며, 입 또한 크다. " + "칼치 또는 도어 라고도 한다" + "')");
            db.execSQL("INSERT INTO 물고기 VALUES ('" + "감성돔" + "','" + "지느러미가 크게 발달되어 있으먀, 갑각류나 패류 등 작은 수생물을 포식한다." +
                    " 참돔에 비하면 성장속도가 느리며, 우리나라 근해, 일본의 북해 등 널리 분포하며 서식한다." + "')");
            db.execSQL("INSERT INTO 물고기 VALUES ('" + "고등어" + "','" + "가을철에 맛이 제일 좋으며, 연해에 분포되어 비교적 많이 잡히는 생선이다." +
                    " 오메가-3 지방산이 많은 생선으로 유명하다." + "')");
            db.execSQL("INSERT INTO 물고기 VALUES ('" + "대구" + "','" + "먹성이 대단한 포식성 어류로서, 입과 머리가 크다 해서 ㅁㅁ로 불리우는 한류성 어종이다." +
                    "뒷지느러미는 두 개로 검고, 등지느러미는 세 개로 넓게 퍼져 있으며 가슴지느러미와 함께 노란색을 띤다." + "')");
        }
        check = rand.nextInt(6)+10;
    }

    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS)
        {
            locale = Locale.getDefault();
            if(tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE)
                tts.setLanguage(locale);
            else
                Toast.makeText(this, "지원하지 않는 언어 오류", Toast.LENGTH_SHORT).show();
        }
        else if( status == TextToSpeech.ERROR) {
            Toast.makeText(this, "음성 합성 초기화 오류", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if(tts != null)
            tts.shutdown();
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == 0 && resultCode == RESULT_OK) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String str = result.get(0);
                if (str.length() > 0 && str.equals(fishname)) {
                    if (tts.isSpeaking())
                        tts.stop();
                    tts.speak("정답입니다!", TextToSpeech.QUEUE_FLUSH, null);
                }
                else {
                    tts.speak("오답입니다! 정답은 " + fishname+ " 입니다.", TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        } catch (Exception e) {
        }
        quiz.setVisibility(View.INVISIBLE);
        bupyo.setVisibility(View.INVISIBLE);
        quiz_back.setVisibility(View.INVISIBLE);
    }

    private void getfish() {
        Toast.makeText(getApplicationContext(), "랜덤시간 = " + count, Toast.LENGTH_SHORT).show();
        quiz.setVisibility(View.VISIBLE);
        quiz_back.setVisibility(View.VISIBLE);
        info = "<문제>\n\n";

        c = db.rawQuery("SELECT 정보, 이름 FROM 물고기 ",null);
        c.moveToPosition(query);
        info += c.getString(0);
        fishname = c.getString(1);

        quiz.setText(info);

        quiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //player.pause();
                    intent2 = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    startActivityForResult(intent2, 0);
                } catch (Exception e)  {
                    Toast.makeText(getApplicationContext(), "구글 앱이 설치되지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 나중에 다 초기화
        isfishing = false;
        flag = false;
        isroding = false;
        count = 0;
        query = rand.nextInt(5);
        check = rand.nextInt(6)+10;
    }
    void HTMLParsing() {
        try {
            int start = wether.indexOf("class=\"main\"");
            int end = wether.indexOf("어제보다");
            wether = wether.substring(start + 13, end-2);
            GlideDrawableImageViewTarget gif = new GlideDrawableImageViewTarget(s);
            if(wether.equals("흐림") || wether.equals("눈"))
                Glide.with(this).load(R.drawable.raining).into(gif);
            else
                Glide.with(this).load(R.drawable.ocean).into(gif);
            GlideDrawableImageViewTarget png1 = new GlideDrawableImageViewTarget(rod1);
            Glide.with(this).load(R.drawable.exrod).into(png1);

        } catch (Exception e) { Toast.makeText(getApplicationContext(), "파싱에러",Toast.LENGTH_SHORT).show(); }
    }

    class WorkerThread extends Thread {
        Handler h;
        WorkerThread(Handler h) {
            this.h = h;
        }
        public void run() {
            try {
                URL url = new URL("https://search.naver.com/search.naver?sm=top_hty&fbm=0&ie=utf8&query=%EC%9B%90%EC%A3%BC%EB%82%A0%EC%94%A8");
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                while ((Wonju = in.readLine()) != null)
                    if (Wonju.contains("어제보다")) {
                        wether += Wonju;
                        break;
                    }
                in.close();
                h.sendMessage(new Message());
            } catch (Exception e) {
            }
        }
    }

    class dbHelper extends SQLiteOpenHelper {
        public dbHelper(Context context) {
            super(context, "fish.db", null, 1);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE 물고기 (이름 TEXT, 정보 TEXT);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS 물고기");
            onCreate(db);
        }
    }
    public void onBackPressed(){
        player.stop();
        intent = new Intent(GameActivity.this, LoadingActivity.class);
        startActivity(intent);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION){
            float roll = event.values[2];
            if(roll >= 20 && isroding == false) {
                GlideDrawableImageViewTarget png2 = new GlideDrawableImageViewTarget(rod2);
                Glide.with(this).load(R.drawable.exrod).into(png2);
                rod2.setVisibility(View.VISIBLE);
                rod1.setVisibility(View.INVISIBLE);
                rod3.setVisibility(View.INVISIBLE);
                rodcheck[0] = false;
                rodcheck[1] = true;
                rodcheck[2] = false;
            }else if(roll<-20 && isroding == false){
                GlideDrawableImageViewTarget png3 = new GlideDrawableImageViewTarget(rod3);
                Glide.with(this).load(R.drawable.exrod).into(png3);
                rod3.setVisibility(View.VISIBLE);
                rod1.setVisibility(View.INVISIBLE);
                rod2.setVisibility(View.INVISIBLE);
                rodcheck[0] = false;
                rodcheck[1] = false;
                rodcheck[2] = true;

            }else{
                if(isroding == false) {
                    rod1.setVisibility(View.VISIBLE);
                    rod2.setVisibility(View.INVISIBLE);
                    rod3.setVisibility(View.INVISIBLE);
                    rodcheck[0] = true;
                    rodcheck[1] = false;
                    rodcheck[2] = false;
                }
            }
            rod1.invalidate();
            rod2.invalidate();
            rod3.invalidate();
        }
        else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float value = (event.values[0]*event.values[0]) + (event.values[1]*event.values[1]) + (event.values[2]*event.values[2]);
            long time = System.currentTimeMillis();
            long gap = (time - old);
            if(gap > 500) {
                old = time;
                if (value > 200 && flag == false && isfishing == false) {
                    bupyo.setVisibility(View.INVISIBLE);
                    for (int i = 0; i < 3; i++) {
                        if (rodcheck[i] == true && i == 0) {
                            rod1.setRotation(30.0f);
                            break;
                        } else if (rodcheck[i] == true && i == 1) {
                            rod2.setRotation(30.0f);
                            break;
                        } else if (rodcheck[i] == true && i == 2) {
                            rod3.setRotation(30.0f);
                            break;
                        }
                    }
                    count=0;
                    flag = true;
                    isroding = true;
                }
                else if (value > 400 && flag == true) {
                    GlideDrawableImageViewTarget bupyo1 = new GlideDrawableImageViewTarget(bupyo);
                    Glide.with(this).load(R.drawable.bupyo1).into(bupyo1);
                    bupyo.setVisibility(View.VISIBLE);
                    rod1.setRotation(0.0f);
                    rod2.setRotation(0.0f);
                    rod3.setRotation(0.0f);
                    isfishing = true;

                    t = new Thread(new t());
                    t.start();

                    h2 = new Handler() {
                        public void handleMessage(Message msg) {
                            int sec = msg.arg1;
                            if(sec == check) {
                                getfish();
                            }
                        }
                    };
                }
            }
        }
    }
    class t implements Runnable {
        public void run() {
            while(isfishing) {
                try {
                    Thread.sleep(1000);   // 0.01초
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message msg = new Message();
                msg.arg1 = count++;
                h2.sendMessage(msg);
            }
        }
    }
}