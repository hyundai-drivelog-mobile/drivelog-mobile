# 다음 세션 인계 — 2026-11-10

## 최신 상태 — 로컬 연결·도장 확인 및 PR 준비

이 절은 아래 최초 인계의 실제 설치 미완료 설명보다 우선한다. 모바일 `feature/car-link`, 차량·서버 `feature/drive-card-redesign`의 변경을 각각 커밋하고 main 대상 PR로 제출한다. PR 병합은 하지 않는다.

- Android PR: https://github.com/hyundai-drivelog-mobile/drivelog-mobile/pull/6 (구현 커밋 `93b98e0`).
- 차량·서버 동반 PR: https://github.com/hyundai-drivelog/drivelog/pull/11 (구현 커밋 `7de764e`).
- 두 PR 생성·푸시 완료 후 이 링크 갱신도 같은 브랜치에 추가 커밋한다. 다음 세션에는 리뷰·CI·병합 상태를 다시 확인한다. 이 PC의 Git 작성자는 미설정이므로 필요하면 현재 GitHub 계정 HongJongMin의 noreply 주소를 커밋 명령에만 적용한다.

- 서버는 PostgreSQL 17.11 Compose + Spring local(8080)로 실행 중이었다. DB 자격 증명은 차량 저장소의 Git 제외 `server/.env`에 있다. 소스·PR에 토큰이나 비밀번호를 넣지 않는다.
- `DRIVELOG_Pleos` / `emulator-5554` / 사용자 10에 두 앱이 설치되어 있고 6자리 코드로 연결했다. 모바일 업데이트 시 기존 앱 데이터·연결 토큰을 보존했다. 실제 휴대폰 연결 검증은 아니다.
- 차량에서 에뮬레이터 GPS·기어 입력으로 광안리 → 해운대 약 5.6km·4분 기록을 만들었다. `베니키아호텔 해운대` 기록 카드가 모바일 최근 드라이브에 나타났고 `내 스탬프`에서 `해운대 해수욕장`, `2026.11.10 첫 방문`, 차량과 같은 선화 도장을 확인했다. 앱 자체 simulated 모드는 사용하지 않았지만 실차 데이터가 아닌 에뮬레이터 검증 기록이다. 사용자 요청 결과이므로 보존한다.
- 홈 `내 스탬프` 보조 문구는 아직 `준비 중`이다. 버튼은 실제 스탬프 화면으로 연결된다. 후속 작업에서 문구를 정리할 수 있다.
- PR 준비 시 `:app:assembleDebug :app:testDebugUnitTest` 성공(기존 Gradle 결과 UP-TO-DATE). 차량 빌드·단위 테스트와 서버 test·bootJar도 성공했다. 새 검증용 주행 데이터나 테스트를 추가 생성하지 않았다.
- 다음 세션: 두 저장소 PR 리뷰·CI 확인 → 실제 Android 폰의 HTTPS 접근 구성 → 사진 업로드·카드 PNG 저장 및 오프라인·WorkManager 검증. 즉시 푸시는 구현하지 않았다. 위치·스탬프 판정 실차 검증도 남아 있다.
- 재개 전에 차량 저장소 `docs/CURRENT_STATE.md` 최신 인계, 이 저장소 README·codex.md·디자인 문서를 읽는다. 기존 DB와 토큰을 유지하며, 실행 중인 서버·에뮬레이터를 중복 시작하지 않는다.
- APK는 `app/build/outputs/apk/debug/app-debug.apk`, 도장 화면 캡처는 `D:/AndroidDev/tmp/drivelog-server/haeundae-stamp.png`(로컬 파일)다. 디버그 서버 기본 주소는 `http://10.0.2.2:8080`이며 실제 휴대폰에는 이 주소를 사용하지 않는다.

차량 스탬프를 서버와 연동해 모바일에 저장하고 차량 그림까지 표시하는 요청을 구현했다. 모바일 실제 경로는 `D:\AndroidDev\drivelog-mobile`, 차량·서버 저장소는 `C:\Users\컴퓨터\StudioProjects\drivelog`다. 상세 기록은 차량 저장소 `docs/CURRENT_STATE.md`와 `docs/verification/STAMP_SYNC_2026-11-10.md`에 있다.

## 구현

- 서버 `GET /api/v1/stamps`를 기존 모바일 Bearer 토큰으로 호출한다. 서버는 차량 공통 카탈로그와 전체 주행 기록으로 계정별 최초 방문을 계산한다. 실제 주행 0.1km 이상·경로 2점 이상·종료 위치 1km 이내의 가장 가까운 명소 하나만 인정한다. 시뮬레이션은 제외하고 기록 삭제는 다음 동기화에 반영한다.
- 홈의 `도전과제` 대신 `내 스탬프`를 연결했다. 소셜 로그인은 필요 없고 기존 6자리 차량 연결을 사용한다. 장소·지역·설명·최초 방문 날짜와 도장 그림을 표시한다.
- `data/StampCache.kt`의 `StampDto`와 토큰 해시별 `stamps.json`으로 오프라인 사본을 보관한다. 조회 실패 시 사본을 유지한다. `DriveLogApi`, `MobileModel`, `RecordSyncWorker`에 조회를 추가했다. 전경 복귀·기록 동기화·화면 진입·새로고침·WorkManager 주기 조회로 갱신한다. 즉시 푸시·스탬프 알림은 없다.
- `feature/journey/StampScreen.kt`, `feature/journey/StampArt.kt` 신규. 차량 `StampArt.kt`의 Canvas 도형을 그대로 이식했고 `StampPen` 코드 일치를 비교 확인했다. 서버 `place.art`·`place.tilt`로 그림과 기울기를 적용한다. 외부 이미지 요청은 없다. 차량 그림 변경 시 모바일 이식본도 확인한다.
- `feature/home/HomeScreen.kt`, `navigation/MobileNavHost.kt`에서 진입점을 연결했다. 기존 사진·카드 흐름과 테마·공통 머리글·페이지 간격을 유지하고 새 라이브러리·Pleos SDK는 추가하지 않았다.

## 검증·적용

- 도장 그림 이식 후 `:app:assembleDebug :app:testDebugUnitTest` 성공.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- 차량 빌드·단위 테스트도 성공했고 서버 `test bootJar`에서 테스트 45개 통과. H2 HTTP·DB 통합 테스트이며 실제 기기 간 검증과 구분한다.
- 앱 설치·실제 화면 확인·오프라인 재실행·WorkManager 실행·서버 배포·실차 GPS 판정은 아직 하지 않았다. 이번 세션 커밋·푸시는 없다.
- 먼저 `/stamps` 포함 서버를 적용하고 차량은 `-PphoneLinkEnabled=true`로 빌드한다. 실제 폰에는 접근 가능한 HTTPS 주소가 필요하며 모바일 release 설정은 `-PreleaseApiUrl=https://호스트`다.
- 차량 연결 전 기록의 전송 보완은 차량 `JourneyStore.setToken`에 있다. 필요하면 새 차량 버전에서 다시 코드 연결하고 기존 모바일 토큰을 유지해 같은 계정으로 연결한다.
- 작업 시 루트 `README.md`, `codex.md`, `docs/MOBILE_DESIGN_SOURCES.md`를 따른다. 사용자의 스탬프 추가 요청이 이전 문서의 방문 도장을 중심 구조로 도입하지 않는다는 규칙보다 우선하며, 보조 화면으로 구현했다.
