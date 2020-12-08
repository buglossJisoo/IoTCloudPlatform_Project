# IoTCloudPlatform_Project

## AWS를 이용한 IoT 클라우드 플랫폼 프로젝트

### 주제 : 지붕 붕괴 예방

1. Arduino(MKR WIFI 1010)에 연결된 weight sensor을 이용해 지붕의 무게를 감지해 DynamoDB에 값을 Upload

2. Uploada된 값과 기상청 API, 건축물API를 통해 위험도를 예측(추후, 데이터가 쌓여서 위험도 예측 모델을 만들 수 있음.) 

3. 붕괴 피해가 예상될 경우, APP을 통해 LED와 BUZZER를 ON으로 변화시켜 주민들의 대피를 유도함.(상황 종료시, OFF)


![1](https://user-images.githubusercontent.com/70942492/101362675-c643dd00-38e3-11eb-8a3c-9a826b64cc67.png)

## 1. Arduino MKR WIFI 1010 관련 Library 설치

* WIFININA
* ArduinoBearSSL
* ArduinoECCX08
* ArduinoMqttClient
* Arduino Cloud Provider Examples

## 2. ECCX08SCR예제를 통해 인증서 만들기

1. Arduino 파일 -> 예제 -> ArduinoECCX08 -> Tools -> ECCX08CSR Click!

2. Serial Monitor를 연 후, Common Name: 부분에 SnowProject 입력(나머지 질문들은 입력 없이 전송 누르기) Would you like to generate? 에는 Y 입력!

3. 생성된 CSR을 csr.txt 파일로 만들어 Save!

## 3. AWS IoT Core에서 사물 등록하기

1. 관리 -> 사물 -> 단일 사물 생성 -> 사물 이름은 SnowProject 입력 -> CSR을 통한 생성을 Click -> 2번에서 저장한 csr.txt를 Upload -> 사물 등록

* region은 아시아 태평양(서울) ap-northeast-2로 해줌./ 사물의 정책 AllowEverything(작업 필드 : iot.* 관련) 생성 후 연결해줌.

2. 보안 -> 관리에서 생성된 인증서도 정책(AllowEverything)을 연결 해줌.

3. 생성된 인증서의 …를 Click한 후, 다운로드 선택

4. 다운로드 된 인증서 확인

## 4. Arduino_SnowProject/arduino_secrets.h 

1. #define SECRET_SSID ""에 자신의 Wifi 이름을 적고, #define SECRET_PASS ""에 Wifi의 비밀번호를 적는다.

2. #define SECRET_BROKER "xxxxxxxxxxxxxx.iot.xx-xxxx-x.amazonaws.com"에는 설정에서 확인한 자신의 엔드포인트를 붙여넣기 한다.

3. const char SECRET_CERTIFICATE[] 부분에는, 3에서 다운 받은 인증서 긴 영어들을 복사 붙여넣기 해준다.

* 올바르게 작성 후, 업로드를 하면 Serial Monitor에는 network와 MQTT broker에 connect된 문구가 뜰것이다.

## 5. AWS DynamoDB 테이블 만들기 / Lambda함수 정의 / 규칙 정의

 1. 테이블 만들기 -> 테이블 이름 : SnowData / 파티션 키: deviceId(데이터 유형 : 문자열) 
 
 2. 정렬 키 추카 선택 -> time 입력(데이터 유형 : 번호 선택)
 
 3. Lambda함수 Eclipse용 AWS Toolkit 이용해 생성 & Upload 
(https://kwanulee.github.io/IoTPlatform/dynamodb.html)

> Project name : RecordingDeviceDataJavaProject2

> Group ID: com.example.lambda

> Artifact ID: recording

> Class Name: RecordingDeviceInfoHandler2

> Input Type : Custom

>> 
```javascript
mport java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class RecordingDeviceInfoHandler2 implements RequestHandler<Document, String> {
    private DynamoDB dynamoDb;
    private String DYNAMODB_TABLE_NAME = "SnowData";

    @Override
    public String handleRequest(Document input, Context context) {
        this.initDynamoDbClient();
        context.getLogger().log("Input: " + input);

        //return null;
        return persistData(input);
    }

    private String persistData(Document document) throws ConditionalCheckFailedException {

        // Epoch Conversion Code: https://www.epochconverter.com/
        SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        String timeString = sdf.format(new java.util.Date (document.timestamp*1000));

        return this.dynamoDb.getTable(DYNAMODB_TABLE_NAME)
                .putItem(new PutItemSpec().withItem(new Item().withPrimaryKey("deviceId", document.device)
                        .withLong("time", document.timestamp)
                        .withString("weight1", document.current.state.reported.weight1)
                        .withString("weight2", document.current.state.reported.weight2)
                        .withString("LED", document.current.state.reported.LED)
                        .withString("BUZZER", document.current.state.reported.BUZZER)
                        .withString("timestamp",timeString)))
                .toString();
    }

    private void initDynamoDbClient() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion("ap-northeast-2").build();

        this.dynamoDb = new DynamoDB(client);
    }

}

class Document {
    public Thing previous;       
    public Thing current;
    public long timestamp;
    public String device;       // AWS IoT에 등록된 사물 이름 
}

class Thing {
    public State state = new State();
    public long timestamp;
    public String clientToken;

    public class State {
        public Tag reported = new Tag();
        public Tag desired = new Tag();

        public class Tag {
            public String weight1;
            public String weight2;
            public String LED;
            public String BUZZER;
        }
    }
}
```

>> 다음과 같이 수정 후, [Upload function to AWS Lambda] Click! -> 함수 이름 : SnowDataFunction

 
 4. AWS IoT Core -> 동작 -> 규칙 -> 이름 : SnowRule인 규칙 생성 -> 규칙 쿼리 설명문 : SELECT *, 'SnowProject' as device FROM '$aws/things/SnowProject/shadow/update/documents' -> 작업 추가-> 메시지 데이터를 전달하는 Lambda 함수 호출 선택 -> 5-3에서 upload한 SnowDataFunction Lambda함수 선택 -> 작업 추가 -> 규칙 생성 Click!
 
## 6. API Gateway를 이용한 RestAPI 생성

0. CORS 활성화 및 API Gateway 콘솔에서 RESTAPI 배포 (공통)

1. 디바이스 상태 조회 REST API 구축 

2. 디바이스 상태 변경 REST API 구축 

3. 디바이스 로그 조회 REST API 구축

> (https://kwanulee.github.io/IoTPlatform/api-gateway.html)

* API Gateway -> 스테이지 -> prod -> URL호출 부분의 주소를 이용!


## File 설명

### 1. DFDFRobot_HX711-master / HX711_library

* Weight Sensor관련 library

> Weight Sensor 1 관련 library : DFDFRobot_HX711-master

> Weight Sensor 2 관련 library : HX711_library

### 2. Arduino_SnowProject(Arduino Code)

* arduino_secrets.h는 위에서 설명했기때문에 생략

#### 2-1. Arduino_SnowProject.ino

1. Weight sensor 관련 library, Led, Buzzer, mkr, wifi 관련 파일들 정의

2. 

- Arduino에 연결된 Weight Sensor, Led, buzzer의 현재 상태를 topic에 update 

```javascript
    if(MyScale.readWeight() < 0.0 ){
    float t1 = MyScale.readWeight();
    float t2 = scale.get_units(); 
    t1 *= -1.0;
    
    if(scale.get_units()<0.0){
      t2 *= -1.0;
    }
    // make payload for the device update topic ($aws/things/SnowProject/shadow/update)
    sprintf(payload,"{\"state\":{\"reported\":{\"weight1\":\"%0.2f\",\"weight2\":\"%0.2f\",\"LED\":\"%s\",\"BUZZER\":\"%s\"}}}",t1,t2,led,buzzer);
  
  else{
    float t1 = MyScale.readWeight();
    float t2 = scale.get_units();
    if(scale.get_units()<0.0){
       t2 *= -1.0;
    }
    // make payload for the device update topic ($aws/things/SnowProject/shadow/update)
     sprintf(payload,"{\"state\":{\"reported\":{\"weight1\":\"%0.2f\",\"weight2\":\"%0.2f\",\"LED\":\"%s\",\"BUZZER\":\"%s\"}}}",t1,t2,led,buzzer);
  }
```

- Led가 ON값으로 변경되면, Led와 Buzzer ON <-> Led가 OFF값으로 변경되면, Led와 Buzzer OFF

```javascript
/*
   * LED와 BUZEER를 하나의 제어로 하게 했음.
   * LED가 ON -> BUZZER도 ON
   * LED가 OFF -> BUZZER도 OFF
   */
 if (strcmp(led,"ON")==0 /*&& strcmp(buzzer,"ON")==0*/) {
    led1.on();
    buzzer1.on();
    sprintf(payload,"{\"state\":{\"reported\":{\"LED\":\"%s\",\"BUZZER\":\"%s\"}}}","ON","ON");
    sendMessage(payload);
    
  } else if (strcmp(led,"OFF")==0 /*&& strcmp(buzzer,"OFF")==0*/) {
    led1.off();
    buzzer1.off();
    sprintf(payload,"{\"state\":{\"reported\":{\"LED\":\"%s\",\"BUZZER\":\"%s\"}}}","OFF","OFF");
    sendMessage(payload);
  }
```

#### 2-2. Buzzer.h, Buzzer.cpp / Led.h, Led.cpp

Buzzer.h, Buzzer.cpp : Buzzer관련 입출력 PIN 설정, ON/OFF기능 구현, Buzzer의 ON/OFF 상태 관련

> (BUZZER가 ON이면 사이렌 소리가 울리도록 코드 작성)

Led.h, Led.cpp : Led관련 입출력 PIN 설정, ON/OFF기능 구현, Led의 ON/OFF 상태 관련

> (LED가 ON이면 led가 켜지도록 코드 작성)

### 3. Android_SnowProject/SnowProject(Android App Code)



