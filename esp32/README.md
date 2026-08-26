# ESP32 물품보관함 잠금장치 컨트롤러

백엔드의 `GET/PATCH /api/lockers/{lockerId}/lock-status` API를 1초 주기로 폴링해서,
1번/2번 물품보관함의 잠금 상태(`LOCKED`/`UNLOCKED`)에 맞춰 서보모터 각도를 바꾸는 데모용 펌웨어.

## 준비물

- ESP32 개발보드
- SG90 등 서보모터 2개 (1번 보관함용, 2번 보관함용)
- ESP32와 서보모터가 백엔드 서버와 같은 Wi-Fi 네트워크에 있어야 함

## Arduino IDE 설정

1. **보드 매니저**: Arduino IDE > 환경설정 > 추가 보드 매니저 URL에 아래 추가 후, 보드 매니저에서 `esp32` (Espressif Systems) 설치
   ```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
   ```
2. **라이브러리 매니저**에서 아래 라이브러리 설치
   - `ArduinoJson` (7.x) — Benoit Blanchon
   - `ESP32Servo` — Kevin Harrington / madhephaestus
3. **보드 선택**: 도구 > 보드 > ESP32 Arduino > 사용 중인 보드(예: ESP32 Dev Module)

## 설정값 채우기 — `config.h`

업로드 전에 아래 값을 실제 환경에 맞게 수정한다.

| 항목 | 설명 |
|---|---|
| `WIFI_SSID`, `WIFI_PASSWORD` | ESP32가 접속할 Wi-Fi |
| `SERVER_HOST` | 백엔드가 떠 있는 PC의 LAN IP (예: `192.168.0.100`). macOS는 `ipconfig getifaddr en0`으로 확인 |
| `SERVER_PORT` | 백엔드 포트 (기본 8080, 로컬 환경에 따라 다를 수 있음) |
| `SERVO_PINS` | 1번/2번 보관함 서보모터가 연결된 GPIO 핀 (기본 13, 14) |
| `ANGLE_LOCKED` / `ANGLE_UNLOCKED` | 잠김/열림에 대응하는 서보 각도 (기본 0도/90도) |

## 배선

| 서보모터 핀 | ESP32 |
|---|---|
| 신호(주황/노랑) | `SERVO_PINS`에 지정한 GPIO |
| VCC(빨강) | 5V (외부 전원 권장 — ESP32 3.3V 핀으로 서보 여러 개 구동 시 전압 강하 발생 가능) |
| GND(갈색/검정) | GND (ESP32와 서보 전원의 GND는 반드시 공통으로 연결) |

## 동작 방식

- 1초마다 1번, 2번 보관함 상태를 순서대로 GET 조회
- 직전에 반영한 상태와 다를 때만 서보 각도를 다시 씀 (불필요한 서보 떨림 방지)
- Wi-Fi 연결이 끊기면 루프에서 자동 재연결 시도
- 조회 실패(네트워크 오류, 4xx/5xx 등)는 로그만 남기고 다음 폴링에서 재시도
