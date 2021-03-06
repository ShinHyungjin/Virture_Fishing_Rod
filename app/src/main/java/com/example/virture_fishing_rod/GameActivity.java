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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class GameActivity extends AppCompatActivity implements SensorEventListener, TextToSpeech.OnInitListener {
    TextToSpeech tts;       // TTS 사용
    MediaPlayer player;     // 배경음악을 담당하는 객체
    Intent intent, intent2; // intent = LoadingActivity 에서 받은 인텐트, intent2 = TTS에 사용하는 RecognizerIntent
    SensorManager m,m2; Sensor sen,sen2; ImageView rod1,rod2,rod3,bupyo; // m = 방향, m2 = 가속도 , rod1~3 = 왼,중,오른쪽 낚싯대, bupyo = 부표
    SQLiteDatabase db;      // 물고기 정보 테이블 (이름 TEXT, 정보 TEXT)
    String Wonju = "", wether = ""; // HTML Parsing에 사용하는 문자열
    Handler h,h2,h3;           // h = HTML Parsing 핸들러, h2 = 물고기 낚을 때 쓰는 Thread 핸들러, h3 = 배경음악, fishImage 컨트롤 핸들러
    WorkerThread a;         // 물고기 낚을 때 쓰는 Thread
    ImageView s, fish;            // 배경화면 이미지 뷰, 물고기 이미지 뷰
    TextView quiz, answer;  // quiz = 물고기 낚았을 때 등장하는 TextView , answer = 퀴즈 맞힌 갯수
    RelativeLayout quiz_back;   // 퀴즈 TextView(quiz)가 보이기 위한 Layout
    long old=0;                 // 낚싯대가 들어올려질때의 텀 (0.5s)
    int count=0;                // Thread 에서 사용하는 증감변수 = Random check와 매칭되는 순간 물고기가 낚인다는 판정
    int bgcount=0, fishcount=0; // 정답이면 bgcount가 1이면 다시실행, 오답이면 3이면 다시실행, fishcount는 2이면 INVISIBLE
    boolean flag = false;       // 낚싯대가 이미 들려있는지..
    boolean rodcheck[] = new boolean[3];    // 어떤 낚싯대가 들려있는지..
    boolean isroding = false;   // 들린 상태에선 더이상 안움직이게..
    boolean isfishing = false;  // 낚는 중일때는 안움직이게..
    boolean iscorrect = false;  // 정답인지.. = 정답 쓰레드 실행
    boolean iswrong = false;    // 오답인지.. = 오답 쓰레드 실행
    boolean isquizing = false;  // 퀴즈 푸는중이면 쓰레드가 멈추게..
    Locale locale;              // TTS 언어설정을 위한 객체
    Thread t = null, t2 = null; // t = 10~15초 쓰레드, t2 = fishImage, 배경음악 컨트롤 쓰레드
    Random rand = new Random(); // 랜덤변수 생성 시 사용 (10~15초 랜덤변수, 0~4 DB 물고기 인덱스)
    int check;                  // 10~15초 랜덤변수
    int query = rand.nextInt(5); // 0~4 DB 물고기 인덱스
    int score = 0;              // 점수
    Cursor c;                   // RawQuery에 사용
    String info="<문제> \n\n", fishname;  // info = 물고기 정보, fishname = 물고기 이름
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game); // 앱 실행시 첫 화면은 activity_loading.xml 이다 (Manifests file에 정의됨)

        tts = new TextToSpeech(this,this);  // TTS 사용

        player = MediaPlayer.create(this,R.raw.splash02);   // 배경음 사용
        player.setLooping(true);                                   // 무한 루핑
        player.start();                                            // 화면 전환시 바로 배경음악 실행

        for(int i=0; i<3; i++)
            rodcheck[i] = false;                                   // 낚싯대(rod1~3)의 체크사항을 알 수 있음

        s = findViewById(R.id.iv2);     // 배경화면
        rod1 = findViewById(R.id.iv3);  // 중앙 낚싯대
        rod2 = findViewById(R.id.iv4);  // 왼쪽 낚싯대
        rod3 = findViewById(R.id.iv5);  // 오른쪽 낚싯대
        bupyo = findViewById(R.id.iv6); // 중앙 부표
        quiz = findViewById(R.id.quiz); // 물고기를 낚았을 때 나오는 문제 TextView
        quiz_back = findViewById(R.id.quiz_back);   // TextView 를 꾸미는 뒷 백그라운드 네모 박스
        answer = findViewById(R.id.answer); // 맞힌 갯수의 TextView
        fish = findViewById(R.id.fish);

        h = new Handler() {                     // 원주 날씨를 URL을 통해 얻어오고 파싱한다
            public void handleMessage(Message msg) {
                HTMLParsing();
            }
        };
        a = new WorkerThread(h);
        a.start();

        m = (SensorManager) getSystemService(SENSOR_SERVICE);   // 방향센서
        sen = m.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        m2 = (SensorManager) getSystemService(SENSOR_SERVICE);  // 가속도 센서
        sen2 = m2.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(sen==null){
            Toast.makeText(this,"방향센서 없음->프로그램 종료",Toast.LENGTH_LONG).show();
            finish();
        }
        if(sen2==null){
            Toast.makeText(this,"가속도센서 없음->프로그램 종료",Toast.LENGTH_LONG).show();
            finish();
        }
        m.registerListener(this,sen,SensorManager.SENSOR_DELAY_UI);
        m2.registerListener(this,sen2,SensorManager.SENSOR_DELAY_UI);

        dbHelper helper = new dbHelper(this);   // DBHelper 클래스를 정의하여 SQLite DB를 만든다

        if(db == null) {        // 5마리 물고기 정보 삽입
            db = helper.getWritableDatabase();
            db.execSQL("INSERT INTO 물고기 VALUES ('" + "가다랑어" + "','" + "고등어과의 어류 가운데 소형 종에 속한다. 몸은 굵고 통통한 방추형임" +
                    " 등은 청흑색, 배는 광택을 띤 은백색이고 그 위에 4~10줄의 검은 세로띠가 있는 것이 특징이다." + "')");
            db.execSQL("INSERT INTO 물고기 VALUES ('" + "갈치" + "','" + "광택이 나는 은백색을 띠며 등지느러미는 연한 황록색을 띤다." +
                    " 눈이 머리에 비해 큰 편이며, 입 또한 크다. " + "칼치 또는 도어 라고도 한다" + "')");
            db.execSQL("INSERT INTO 물고기 VALUES ('" + "감성돔" + "','" + "지느러미가 크게 발달되어 있으며, 갑각류나 패류 등 작은 수생물을 포식한다." +
                    " 참돔에 비하면 성장속도가 느리며, 우리나라 근해, 일본의 북해 등 널리 분포하며 서식한다." + "')");
            db.execSQL("INSERT INTO 물고기 VALUES ('" + "고등어" + "','" + "가을철에 맛이 제일 좋으며, 연해에 분포되어 비교적 많이 잡히는 생선이다." +
                    " 오메가-3 지방산이 많은 생선으로 유명하다." + "')");
            db.execSQL("INSERT INTO 물고기 VALUES ('" + "대구" + "','" + "먹성이 대단한 포식성 어류로서, 입과 머리가 크다 해서 ㅁㅁ로 불리우는 한류성 어종이다." +
                    "뒷지느러미는 두 개로 검고, 등지느러미는 세 개로 넓게 퍼져 있으며 가슴지느러미와 함께 노란색을 띤다." + "')");
        }
        check = rand.nextInt(6)+10; // 10~15초 사이에 물고기가 낚이게 끔 랜덤변수 선언
    }

    public void onInit(int status) {      // TTS를 초기화 함
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
        try {   // TTS를 통해 음성인식을 하면, 녹은된 것이 문자열로 저장되고, 그것과 DB에 있는 물고기 이름과 매칭되는지 비교
            if (requestCode == 0 && resultCode == RESULT_OK) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String str = result.get(0);
                if (str.length() > 0 && str.equals(fishname)) {
                    if (tts.isSpeaking())
                        tts.stop();
                    tts.speak("정답입니다!", TextToSpeech.QUEUE_FLUSH, null);    // 정답이면 200점 추가
                    score += 200;
                    answer.setText("SCORE : "+score);
                    GlideDrawableImageViewTarget f = new GlideDrawableImageViewTarget(fish); // 물고기 이미지 보여짐
                    Glide.with(this).load(R.drawable.godung).into(f);
                    fish.setVisibility(View.VISIBLE);
                    iscorrect = true;  // 정답 쓰레드 실행
                    t2 = new Thread(new t2());

                    t2.start();

                    h3 = new Handler() {
                        public void handleMessage(Message msg) {
                            int bg = msg.arg1;
                            int fc = msg.arg2;
                            if(bg == 1) {    // 정답이면 TTS가 짧아서 1초만에 BGM 실행
                                player.start();
                            }
                            if(fc >= 2) {   // 2초뒤에 물고기 이미지 가림
                                fish.setVisibility(View.INVISIBLE);
                                iscorrect = false;
                            }
                        }
                    };
                }
                else {
                    tts.speak("오답입니다! 정답은 " + fishname+ " 입니다.", TextToSpeech.QUEUE_FLUSH, null);
                    score -= 150;       // 오답이면 150점 까임
                    answer.setText("SCORE : "+score);

                    iswrong = true;     // 오답 쓰레드 실행
                    t2 = new Thread(new t2());

                    t2.start();

                    h3 = new Handler() {
                        public void handleMessage(Message msg) {
                            int bg = msg.arg1;
                            if(bg >= 3) {   // 오답은 TTS가 길어서 3초뒤에 BGM 실행
                                player.start();
                                iswrong = false;
                            }
                        }
                    };

                }
            }
            isfishing = false;  // TTS 수행 후 초기화 되어야 할 변수들 (위치이동 가능, 준비자세 가능, 퀴즈풀기 가능 등..)
            flag = false;
            isroding = false;
            isquizing = false;
        } catch (Exception e) {
        }
        quiz.setVisibility(View.INVISIBLE); //물고기를 맞췄던 못맞췄던 문제,부표를 가림
        bupyo.setVisibility(View.INVISIBLE);
        quiz_back.setVisibility(View.INVISIBLE);
    }

    private void getfish() {    // 10~15초 사이에 물고기가 낚이면 수행되는 함수
        // Toast.makeText(getApplicationContext(), "랜덤시간 = " + count, Toast.LENGTH_SHORT).show();
        quiz.setVisibility(View.VISIBLE);   // 문제 TextView 출력
        quiz_back.setVisibility(View.VISIBLE);  // 문제 TextView 백그라운드 출력
        info = "<문제>\n\n";  // < 문제 > \N\N 이 물고기는 ..~

        c = db.rawQuery("SELECT 정보, 이름 FROM 물고기 ",null); // Cursor를 통해 DB접근
        c.moveToPosition(query);    // 0~4 랜덤변수를 통해 DB의 물고기 5마리 중 하나를 랜덤선택
        info += c.getString(0); // < 문제 > + 물고기정보가 문제로 나옴
        fishname = c.getString(1);  // 물고기 이름도 필요함

        quiz.setText(info); // TextView에 문제 출력

        quiz.setOnClickListener(new View.OnClickListener() { // 문제 TextView 클릭시 수행되는 함수
            @Override
            public void onClick(View v) {
                try {
                    player.pause(); // 문제 클릭시 TTS가 실행되고 BGM 일시중지
                    intent2 = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); // TTS 실행
                    startActivityForResult(intent2, 0);
                } catch (Exception e)  {
                    Toast.makeText(getApplicationContext(), "구글 앱이 설치되지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 낚인 물고기를 맞췄던 못맞췄던 수행되는 초기화들...위치변경가능, 준비자세가능, 10~15초 초기화, 0~4 초기화, Thread Count 초기화, 퀴즈풀기 가능

        count = 0;
        query = rand.nextInt(5);
        check = rand.nextInt(6)+10;
        isquizing = true;
    }
    void HTMLParsing() {    // URL을 통해 얻어온 원주 날씨는 파싱...
        try {
            int start = wether.indexOf("class=\"main\"");
            int end = wether.indexOf("어제보다");
            wether = wether.substring(start + 13, end-2);  // 맑음, 흐림, 눈, 비 등을 뽑아오게 된다.
            GlideDrawableImageViewTarget gif = new GlideDrawableImageViewTarget(s);
            if(wether.equals("비") || wether.equals("눈") || wether.equals("흐림"))  // 날씨에 따른 배경화면을 바꿈 (게임화면으로 진입시에만)
                Glide.with(this).load(R.drawable.raining).into(gif);
            else
                Glide.with(this).load(R.drawable.ocean).into(gif);
            GlideDrawableImageViewTarget png1 = new GlideDrawableImageViewTarget(rod1); // 중앙 낚싯대 출력
            Glide.with(this).load(R.drawable.exrod_right).into(png1);

        } catch (Exception e) { Toast.makeText(getApplicationContext(), "파싱에러",Toast.LENGTH_SHORT).show(); }
    }

    class WorkerThread extends Thread {
        Handler h;
        WorkerThread(Handler h) {
            this.h = h;
        }
        public void run() {
            try {  // 네이버 원주날씨 홈페이지에서 얻어옴
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

    class dbHelper extends SQLiteOpenHelper { // fish.db 생성
        public dbHelper(Context context) {
            super(context, "fish.db", null, 1);
        }

        public void onCreate(SQLiteDatabase db) { // fish.db는 물고기 TABLE에 이름(TEXT), 정보(TEXT)
            db.execSQL("CREATE TABLE 물고기 (이름 TEXT, 정보 TEXT);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS 물고기");
            onCreate(db);
        }
    }
    public void onBackPressed(){    // 핸드폰의 뒤로가기 버튼 클릭시 이전 화면인 Loading 화면으로 전환되며 그때는 노래멈춤
        player.stop();
        intent = new Intent(GameActivity.this, LoadingActivity.class);
        startActivity(intent);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    @Override
    public void onSensorChanged(SensorEvent event) { // 방향센서 값 변경시 수행되는 함수
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION){
            float roll = event.values[2]; // y축인 roll로 좌,우 방향전환을 수행함
            if(roll >= 20 && isroding == false) { // 20보다 크다? 양수이니 왼쪽이다.
                GlideDrawableImageViewTarget png2 = new GlideDrawableImageViewTarget(rod2);
                Glide.with(this).load(R.drawable.exrod_left).into(png2);
                rod2.setVisibility(View.VISIBLE); // 왼쪽낚싯대 보임
                rod1.setVisibility(View.INVISIBLE); // 그 외 중앙, 오른쪽 낚싯대 가림
                rod3.setVisibility(View.INVISIBLE);
                rodcheck[0] = false;
                rodcheck[1] = true; // 왼쪽 낚싯대 체크
                rodcheck[2] = false;
            }else if(roll<-20 && isroding == false){ // -20보다 작다? 오른쪽
                GlideDrawableImageViewTarget png3 = new GlideDrawableImageViewTarget(rod3);
                Glide.with(this).load(R.drawable.exrod_right).into(png3);
                rod3.setVisibility(View.VISIBLE); // 오른쪽 낚싯대 보임
                rod1.setVisibility(View.INVISIBLE); // 그 외 왼쪽, 중앙 낚싯대 가림
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
        else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) { // 가속도센서 값 변경시 수행되는 함수
            float value = (event.values[0]*event.values[0]) + (event.values[1]*event.values[1]) + (event.values[2]*event.values[2]);
            long time = System.currentTimeMillis();
            long gap = (time - old);
            if(gap > 500) { // 0.5초 보다 빨리 센서값 변경시 아무일도 안하게끔... (만보기처럼 한번 흔들때 한번만 변경되는 느낌)
                old = time;
                if (value > 200 && flag == false && isfishing == false) { // 낚싯대를 던질 준비가 되었느냐~
                    bupyo.setVisibility(View.INVISIBLE); // 부표 가림
                    for (int i = 0; i < 3; i++) {
                        if (rodcheck[i] == true && i == 0) { // 어느 낚싯대가 체크되있는지 검사 후 그 낚싯대를 회전시켜 던지는 모션처럼 효과
                            rod1.setRotation(30.0f);
                            break;
                        } else if (rodcheck[i] == true && i == 1) {
                            rod2.setRotation(-30.0f);
                            break;
                        } else if (rodcheck[i] == true && i == 2) {
                            rod3.setRotation(30.0f);
                            break;
                        }
                    }
                    count=0; // Count 초기화
                    flag = true; // 준비자세임을 알림
                    isroding = true; // 준비자세일때는 위치전환 불가능하게
                }
                else if (value > 400 && flag == true) {  // 준비자세에서 던지는 모션시 = 가속도 400 이상의 힘으로
                    GlideDrawableImageViewTarget bupyo1 = new GlideDrawableImageViewTarget(bupyo);
                    Glide.with(this).load(R.drawable.bupyo1).into(bupyo1);
                    bupyo.setVisibility(View.VISIBLE); // 부표 보임
                    rod1.setRotation(0.0f); // 회전값 다시 초기화
                    rod2.setRotation(0.0f);
                    rod3.setRotation(0.0f);
                    isfishing = true; // 던진 상태 표시

                    bgcount = 0;    // bg 시간 초기화
                    fishcount=0;    // 물고기 보여짐 시간 초기화

                    t = new Thread(new t()); // 부표가 떳으니 10~15초 랜덤변수를 1초마다 검사하는 쓰레드 시작
                    t.start();

                    h2 = new Handler() {
                        public void handleMessage(Message msg) {
                            int sec = msg.arg1;
                            if(sec == check) {  // 쓰레드 타임과 랜덤변수와 같은가? (10~15초가 흘렀냐)
                                getfish();  // 물고기가 잡혔으니 잡힘 함수 호출
                            }
                        }
                    };
                }
            }
        }
    }
    class t implements Runnable {
        public void run() {
            while(isfishing && !isquizing) {    // 잡는중이면서 퀴즈 푸는중이 아닐때만 쓰레드가 돌게 하기 위함 (없다면 퀴즈가 자꾸 바뀜)
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
    class t2 implements Runnable {
        public void run() {
            if(iscorrect) {     // 정답인 쓰레드
                while (iscorrect) {
                    try {
                        Thread.sleep(1000);   // 0.01초
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Message msg = new Message();
                    msg.arg1 = ++bgcount;       // bg는 1초면 재시작
                    msg.arg2 = ++fishcount;     // fish는 2초면 가려짐
                    h3.sendMessage(msg);
                }
            }
            else if(iswrong) {  // 오답인 쓰레드
                while (iswrong) {
                    try {
                        Thread.sleep(1000);   // 0.01초
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Message msg = new Message();
                    msg.arg1 = ++bgcount;   // 오답은 bg만 3초뒤에 재시작
                    h3.sendMessage(msg);
                }
            }
        }
    }
}