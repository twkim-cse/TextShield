# TextShield

온디바이스 AI(Gemma-4-E2B-it)로 문자 메시지의 스미싱(피싱) 여부를 실시간으로 판별하는 안드로이드 앱입니다. 문자 내용이 기기 밖으로 나가지 않고, 인터넷 연결 없이도 판별이 가능합니다.

## 주요 기능

- **실시간 자동 감지**: SMS 수신 시(`SmsReceiver`) 및 카카오톡/RCS(채팅+) 등 메시지 알림 수신 시(`MessageNotificationListener`) 자동으로 AI가 분석
- **피싱 알림**: 피싱으로 판단된 문자만 알림으로 경고 (정상 문자는 알림 없이 조용히 기록)
- **수동 판별**: 홈 화면에서 문자를 직접 붙여넣어 즉시 판별 가능
- **검사 기록**: 지금까지 판별한 모든 문자 내역과 판정 근거를 확인·삭제 가능
- **통계**: 이번 달 피싱 탐지/정상 문자 건수 요약
- **스미싱 사례 카드**: 자주 발생하는 스미싱 수법을 스와이프로 둘러보고, 관련 네이버 뉴스로 바로 연결
- **신고 연결**: KISA 118(불법스팸대응센터)로 원터치 연결
- **다크 모드** 지원

## 기술 스택

- **UI**: Jetpack Compose, Material 3
- **온디바이스 AI 추론**: [LiteRT-LM](https://github.com/google-ai-edge/litertlm) (`com.google.ai.edge.litertlm`)
- **모델**: [Gemma-4-E2B-it](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) (`.litertlm`, 약 2.6GB, 앱 최초 실행 시 자동 다운로드)
- **비동기 처리**: Kotlin Coroutines
- **저장소**: SharedPreferences (검사 기록/통계/설정)
- **백그라운드 처리**: Foreground Service (`SmsAnalysisService`) — 모델 로드/추론이 방송 수신자의 처리 시간 제한을 넘길 수 있어, 무거운 작업은 포그라운드 서비스로 위임

## 프로젝트 구조

```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt                   # 전체 UI (홈/검사기록/설정 화면, 드로어, 다이얼로그)
├── PhishingDetector.kt               # LiteRT-LM 엔진 초기화 및 추론
├── ModelDownloader.kt                # 모델 파일 다운로드 (DownloadManager)
├── SmsReceiver.kt                    # SMS_RECEIVED 브로드캐스트 수신
├── MessageNotificationListener.kt    # 메시지 알림 감지 (RCS/카카오톡 등)
├── SmsAnalysisService.kt             # 포그라운드 서비스에서 실제 판별 수행
├── PhishingNotifier.kt               # 피싱 경고 알림 표시
├── HistoryStore.kt                   # 검사 기록 저장/조회/삭제
├── ClassificationStats.kt            # 월별 통계 저장/조회
├── ThemePrefs.kt                     # 다크모드 설정 저장
├── SmishingCases.kt                  # 스미싱 유형 예시 데이터
└── ui/theme/                         # Compose 테마 (색상, 타이포그래피)
```

## 빌드 및 실행

1. Android Studio에서 프로젝트 열기
2. Gradle Sync
3. 실제 기기에서 Run (에뮬레이터는 모델 다운로드/추론 성능상 권장하지 않음)
4. 앱 최초 실행 시 SMS 수신·알림 권한을 요청하며, 설정 화면에서 알림 접근 권한(카카오톡/RCS 감지용)을 별도로 허용해야 합니다.

**요구 사항**: minSdk 26, 인터넷 연결(최초 모델 다운로드 시에만 필요, 이후 완전 오프라인 동작)

## 참고 사항

- `AndroidManifest.xml`에 정의된 패키지명은 `com.example.myapplication`입니다.
- 설정 화면의 "모델 정확도 정보"(정확도/오탐률/미탐률)는 자체 테스트 데이터셋(100건) 기준 측정값입니다.
- RCS(채팅+) 메시지는 안드로이드 표준 `SMS_RECEIVED` 브로드캐스트를 거치지 않아, `MessageNotificationListener`(알림 접근 권한)를 통해 보완적으로 감지합니다.
