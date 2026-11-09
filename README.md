# 라이드캄 (RideCalm) – 동승자 케어 앱

운전자가 앞을 보는 동안 차가 대신 돌봄이 필요한 동승자(아이·발달장애인·어르신)를 지켜보고, 문·창문·안전벨트 신호로 위험한 순간을 바로 음성으로 알려 주는 Pleos Connect 차량용 앱입니다. 멀미에 민감한 동승자를 위한 흔들림 미리 알림은 확장 기능으로 들어 있습니다.

> 현대자동차그룹 차량용 앱 개발 공모전 출품작 · 1차 접수용 시제품(v0.8)

## APK

| 파일 | 설명 |
|---|---|
| [`apk/오미희_라이드캄_release.apk`](apk/) | 실행 가능한 release 빌드 (versionName 0.8) |

## 실행 방법

1. Android Studio에서 **Pleos Connect Emulator v2.1.0** AVD를 만들고 실행합니다 ([Pleos Playground 개발 문서](https://document.pleos.ai) › Getting Started 참고).
2. APK를 설치합니다.
   ```
   adb install -r -g apk/오미희_라이드캄_release.apk
   ```
3. 에뮬레이터 앱 목록에서 **라이드캄**을 실행합니다.
4. 왼쪽 위 **[멀미 | 돌봄 | 둘 다]**에서 케어 종류를 고르고, 화면 아래 **시연 콘솔**의 **▶ 자동 시나리오**를 누르면 주행 상황이 차례로 재현됩니다.
   - 돌봄: 출발 전 점검(문·창문·안전벨트·착석) → 창문 열림 · 안전벨트 풀림(가까운 휴게소 안내) · 자리 이동 · 신호 대기 중 문 열림 알림 → 도착 요약
   - 멀미: 회전 · 과속방지턱 · 터널 미리 알림 → 증상 대응 · 휴게소 경유지 추가 → 편안 리포트
   - 콘솔의 개별 버튼(출발, 창문 열림, 문 열림, 벨트, 자리 이동, 에어백 등)으로 한 장면씩 시험할 수도 있습니다.

## 테스트 환경 및 필요한 설정

| 항목 | 내용 |
|---|---|
| 에뮬레이터 | Pleos Connect Emulator v2.1.0 (API 34 시스템 이미지, 2560×1440) |
| 최소 / 대상 SDK | minSdk 28 / targetSdk 34 |
| 빌드 | Android Studio, Kotlin, Jetpack Compose, Gradle 8 이상 |
| 필요한 설정 | 없음. 설치 시 `-g` 옵션으로 퍼미션을 허용합니다. 안내 음성은 에뮬레이터의 기본 TTS를 사용합니다. |

## 현재 시제품의 범위

- 차량 신호(문·창문·안전벨트·착석·회전·과속방지턱 등)는 앱 안의 **시연 콘솔**로 재현합니다.
- 2차 PT까지 Pleos **Vehicle SDK**(Door · Window · Seat · Safety)와 **NaviHelper SDK** 호출로 바꾸고, Pleos Connect **Virtual Console**에서 신호를 넣어 시연할 계획입니다.
- 안전 원칙: 돌봄 기능에서 앱은 문·창문을 스스로 잠그거나 열거나 닫지 않습니다. 알리고, 조작은 운전자가 합니다.

## 소스 구조

```
app/src/main/java/com/ridecalm/app/
├─ MainActivity.kt    앱 시작, 음성(TTS), 차량 속도·기어 연결
├─ RideEngine.kt      상태와 판단 로직(돌봄 점검·위험도별 알림·멈출 곳 안내, 멀미 미리 알림)
├─ Ui.kt              Compose 화면(케어 선택, 돌봄·멀미 화면, 알림, 리포트, 시연 콘솔)
└─ VehicleBridge.kt   차량 정보 연결
```

## 직접 빌드하기

```
./gradlew assembleDebug        # 디버그 APK: app/build/outputs/apk/debug/
```

release 빌드는 저장소에 포함하지 않은 서명 키가 필요합니다.
