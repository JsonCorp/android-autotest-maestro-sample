# android-autotest-maestro-sample

Maestro로 안드로이드 앱을 자동화 테스트하는 방법을 아주 쉽게 설명하기 위한 블로그용 샘플 저장소입니다.

- 테스트 대상: 단일 화면 카운터 앱 (`+1` / `-1` / `초기화` / 10 달성 메시지)
- 테스트 도구: [Maestro](https://maestro.mobile.dev/)
- 블로그 시리즈 계획: [`docs/blog-plan.md`](docs/blog-plan.md)
- 1편 원고 (AI로 스크립트 작성 → CLI 자동화): [`docs/stage1-blog-post.md`](docs/stage1-blog-post.md)
- 2편 원고 (Maestro Studio로 시각적으로 테스트 만들기): [`docs/stage2-blog-post.md`](docs/stage2-blog-post.md)

## 빠른 시작

1. Android 단말을 USB로 연결하고 `adb devices`로 인식되는지 확인합니다.
2. [Maestro CLI](https://maestro.mobile.dev/getting-started/installing-maestro)를 설치합니다.
3. 저장소 루트에서 아래 스크립트를 실행하면 빌드 → 설치 → 테스트 → HTML 리포트 생성까지 한 번에 끝납니다.

   ```
   powershell -ExecutionPolicy Bypass -File scripts\run-tests.ps1
   ```

4. 테스트가 끝나면 `reports/report.html`이 자동으로 브라우저에 열립니다.

## 구조

```
app/                    카운터 샘플 앱 (Kotlin + Jetpack Compose)
.maestro/               Maestro 테스트 플로우 5개
scripts/run-tests.ps1   빌드→설치→테스트→리포트 원커맨드 스크립트
docs/                   블로그 시리즈 계획 및 원고
screenshots/            블로그용 캡처 이미지
```

## 앱 개요

- 화면 중앙에 카운트를 표시하고 `-1` / `초기화` / `+1` 버튼으로 조작합니다.
- 카운트는 0 미만으로 내려가지 않습니다.
- 카운트가 10 이상이면 축하 메시지가 나타납니다.
- `MainActivity`에서 `FLAG_KEEP_SCREEN_ON`을 설정해, 테스트 도중 화면이 꺼져 Maestro가 빈 화면을 보는 문제를 방지합니다.

## Maestro 플로우

| 파일 | 검증 내용 |
|---|---|
| `.maestro/01_increment.yaml` | `+1` 버튼 3회 → `3` 표시 확인 |
| `.maestro/02_boundary.yaml` | `0`에서 `-1` 눌러도 음수로 안 내려감 (경계값) |
| `.maestro/03_reset.yaml` | `+1` 5회 → `초기화` → `0` 복귀 확인 |
| `.maestro/04_milestone.yaml` | `+1` 10회 → 축하 메시지 노출 → `초기화` 후 메시지 사라짐 |
| `.maestro/05_studio_demo.yaml` | Maestro Studio에서 클릭/자동완성만으로 조립한 플로우 (2편 참고) |

개별 실행:

```
maestro test .maestro/01_increment.yaml
```

## Maestro Studio (2편)

YAML을 직접 외워 쓰지 않고 **Maestro Studio** 데스크톱 앱으로 화면을 보면서 시각적으로 테스트를 만드는 방법은 [`docs/stage2-blog-post.md`](docs/stage2-blog-post.md)에서 다룹니다. `.maestro/05_studio_demo.yaml`이 Studio로 만든 결과물입니다.
