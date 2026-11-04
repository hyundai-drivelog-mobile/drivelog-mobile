# DRIVELOG Mobile

차량에서 서버로 전송한 주행 기록을 조회하고, 사진 한 장을 골라 주행 카드를 만드는 독립 Android 앱입니다. 모바일 앱에는 Pleos SDK가 없습니다.

## 구성

- `app/`: Kotlin + Jetpack Compose 모바일 앱
- `docs/MOBILE_DESIGN_SOURCES.md`: 디자인 근거와 적용 범위
- 차량 앱·서버는 별도 `drivelog` 프로젝트에 있습니다.

## 실행

표준 Pixel Android Emulator에서 서버 주소는 기본 `http://10.0.2.2:8080`입니다. 서버는 차량 프로젝트 `server/`에서 `demo` 프로필로 실행할 수 있습니다.

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

실제 기기는 `-PdebugApiUrl=...`으로 접근 가능한 개발 서버 주소를 지정합니다. 릴리스 빌드는 `-PreleaseApiUrl=https://...`으로 실제 HTTPS 주소를 지정해야 합니다. 개발 빌드의 HTTP 허용 대상은 `10.0.2.2`뿐입니다.

차량 앱은 별도 프로젝트에서 `-PphoneLinkEnabled=true`로 빌드하고 Pleos 에뮬레이터에 설치합니다. 차량 홈의 `휴대폰 연결`에서 받은 6자리 코드를 모바일 앱에 입력합니다. 앱은 연결된 계정의 기록을 조회하며, 사용자가 시스템 사진 선택기에서 고른 사진과 카드 설정을 서버에 저장합니다. PNG는 사용자가 선택한 위치에 저장합니다.

새 기록 안내는 앱의 전경 동기화와 WorkManager 주기 조회를 사용합니다. 주기 조회는 즉시 도착을 보장하지 않으며, 실시간 푸시는 서버의 FCM 연동이 추가로 필요합니다.

테스트: `.\gradlew.bat :app:testDebugUnitTest`.
