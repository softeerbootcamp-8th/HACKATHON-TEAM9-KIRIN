#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <ESP32Servo.h>

#include "config.h"

Servo servos[LOCKER_COUNT];
String lastKnownStatus[LOCKER_COUNT];
unsigned long lastPollAt = 0;

void connectWiFi()
{
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("WiFi 연결 중");

  unsigned long startedAt = millis();
  while (WiFi.status() != WL_CONNECTED)
  {
    if (millis() - startedAt > WIFI_CONNECT_TIMEOUT_MS)
    {
      Serial.println();
      Serial.println("WiFi 재연결 실패 - 재부팅합니다");
      delay(200);
      ESP.restart();
    }
    delay(300);
    Serial.print(".");
  }
  Serial.println();
  Serial.print("WiFi 연결됨 - IP=");
  Serial.println(WiFi.localIP());

  // WiFi 모뎀 슬립을 끈다 - 슬립 진입/이탈 시 APB 클럭이 흔들리면서
  // ESP32Servo(LEDC 하드웨어 PWM)의 펄스가 순간적으로 깨져 서보가 제멋대로 움직이는 문제 방지
  WiFi.setSleep(false);
}

bool fetchLockStatus(int lockerId, String &outStatus)
{
  HTTPClient http;
  String url = String("https://") + SERVER_HOST +
               "/api/lockers/" + lockerId + "/lock-status";

  http.begin(url);
  http.setTimeout(HTTP_TIMEOUT_MS);
  int httpCode = http.GET();

  if (httpCode != HTTP_CODE_OK)
  {
    Serial.printf("보관함 상태 조회 실패 - lockerId=%d, httpCode=%d\n", lockerId, httpCode);
    http.end();
    return false;
  }

  JsonDocument doc;
  DeserializationError err = deserializeJson(doc, http.getStream());
  http.end();

  if (err)
  {
    Serial.printf("응답 파싱 실패 - lockerId=%d, error=%s\n", lockerId, err.c_str());
    return false;
  }

  if (!doc["lockStatus"].is<String>())
  {
    Serial.printf("lockStatus 필드 없음/형식 이상 - lockerId=%d\n", lockerId);
    return false; // 이전 상태를 유지, 잘못된 값으로 덮어쓰지 않음
  }
  outStatus = doc["lockStatus"].as<String>();
  return true;
}

void applyLockStatus(int index, const String &status)
{
  int angle = (status == "UNLOCKED") ? ANGLE_UNLOCKED : ANGLE_LOCKED;
  servos[index].write(angle);
  Serial.printf("보관함 잠금 상태 반영 - lockerId=%d, lockStatus=%s, angle=%d\n",
                LOCKER_IDS[index], status.c_str(), angle);
}

void setup()
{
  Serial.begin(115200);
  connectWiFi();

  for (int i = 0; i < LOCKER_COUNT; i++)
  {
    servos[i].attach(SERVO_PINS[i]);
    servos[i].write(ANGLE_LOCKED);
    lastKnownStatus[i] = "";
  }
}

void loop()
{
  if (WiFi.status() != WL_CONNECTED)
  {
    connectWiFi();
  }

  unsigned long now = millis();
  if (now - lastPollAt < POLL_INTERVAL_MS)
  {
    return;
  }
  lastPollAt = now;

  for (int i = 0; i < LOCKER_COUNT; i++)
  {
    String status;
    if (!fetchLockStatus(LOCKER_IDS[i], status))
    {
      continue;
    }
    if (status != lastKnownStatus[i])
    {
      applyLockStatus(i, status);
      lastKnownStatus[i] = status;
    }
  }
}
