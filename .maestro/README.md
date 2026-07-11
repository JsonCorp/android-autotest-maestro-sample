# Maestro 테스트 플로우

`com.example.maestrosample` (카운터 샘플 앱)을 검증하는 플로우 4개.

| 파일 | 검증 내용 |
|---|---|
| `01_increment.yaml` | `+1` 버튼 3회 → `3` 표시 확인 |
| `02_boundary.yaml` | `0`에서 `-1` 눌러도 음수로 안 내려감 (경계값) |
| `03_reset.yaml` | `+1` 5회 → `초기화` → `0` 복귀 확인 |
| `04_milestone.yaml` | `+1` 10회 → 축하 메시지 노출 → `초기화` 후 메시지 사라짐 |

## 사전 준비

1. `adb devices`로 단말이 연결되어 있는지 확인
2. 앱이 단말에 설치되어 있어야 함 (`./gradlew installDebug` 또는 `scripts/run-tests.ps1` 사용)

## 실행

```
maestro test .maestro/                      # 전체 플로우 실행
maestro test .maestro/01_increment.yaml     # 플로우 1개만 실행
maestro studio                              # 시각적으로 플로우 만들기 (2편에서 다룰 예정)
```

전체 실행 + HTML 리포트까지 자동으로 만들려면 저장소 루트의 `scripts/run-tests.ps1`을 사용한다.
