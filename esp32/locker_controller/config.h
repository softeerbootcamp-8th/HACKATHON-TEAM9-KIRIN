#ifndef LOCKER_CONTROLLER_CONFIG_H
#define LOCKER_CONTROLLER_CONFIG_H

// Wi-Fi
#define WIFI_SSID "YOUR_WIFI_SSID"
#define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"

// 백엔드 서버 (ESP32와 같은 네트워크에 있는 PC의 LAN IP)
#define SERVER_HOST "192.168.0.100"
#define SERVER_PORT 8081

// 폴링 주기 (ms)
#define POLL_INTERVAL_MS 1000

// 관리할 보관함 개수와 매핑
#define LOCKER_COUNT 2

static const int LOCKER_IDS[LOCKER_COUNT] = {1, 2};
static const int SERVO_PINS[LOCKER_COUNT] = {13, 14};

// 서보모터 각도
#define ANGLE_LOCKED 0
#define ANGLE_UNLOCKED 90

#endif
