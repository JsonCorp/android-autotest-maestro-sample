# Maestro 테스트 플로우

`com.example.maestrosample` (카운터 샘플 앱)을 검증하는 플로우 8개 + 공통 서브플로우.

| 파일 | 검증 내용 |
|---|---|
| `01_increment.yaml` | `+1` 버튼 3회 → `3` 표시 확인 |
| `02_boundary.yaml` | `0`에서 `-1` 눌러도 음수로 안 내려감 (경계값) |
| `03_reset.yaml` | `+1` 5회 → `초기화` → `0` 복귀 확인 |
| `04_milestone.yaml` | `+1` 10회 → 축하 메시지 노출 → `초기화` 후 메시지 사라짐 |
| `05_studio_demo.yaml` | Maestro Studio에서 클릭/자동완성만으로 조립한 플로우 (2편 참고) |
| `06_add_record.yaml` | 기록 화면에서 이름 입력 → 저장 → 목록 끝까지 스크롤해 확인 (4편) |
| `07_scroll_history.yaml` | 화면 밖의 마지막 예시 기록까지 `scrollUntilVisible` (4편) |
| `08_async_wait.yaml` | 로딩 스피너 → `extendedWaitUntil` → `back` 복귀 (4편) |
| `common/launch_clean.yaml` | 앱 초기화+재실행 공통 서브플로우 — `runFlow`로만 참조 |

`maestro test .maestro/`는 하위 폴더로 내려가지 않으므로 `common/`의 서브플로우는 단독 실행되지 않고, 각 플로우의 `runFlow`를 통해서만 실행된다.

## 사전 준비

1. `adb devices`로 단말이 연결되어 있는지 확인
2. 앱이 단말에 설치되어 있어야 함 (`./gradlew installDebug` 또는 `scripts/run-tests.ps1` 사용)

## 실행

```
maestro test .maestro/                      # 전체 플로우 실행
maestro test .maestro/01_increment.yaml     # 플로우 1개만 실행
```

시각적으로 플로우를 만들고 싶다면 [Maestro Studio 데스크톱 앱](https://studio.maestro.dev/)을 사용한다 (2편 참고, `05_studio_demo.yaml`이 이걸로 만든 결과물).

전체 실행 + HTML 리포트까지 자동으로 만들려면 저장소 루트의 `scripts/run-tests.ps1`을 사용한다.
