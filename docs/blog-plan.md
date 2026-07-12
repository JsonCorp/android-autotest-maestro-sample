# 블로그 시리즈 계획 — "AI와 Maestro로 안드로이드 앱 자동화 테스트하기"

## 목표

안드로이드 개발을 처음 접하는 독자도 따라 할 수 있도록, Maestro를 이용한 UI 자동화 테스트를 "아주 쉽게" 설명하는 시리즈(현재 5편).
샘플 저장소: `android-autotest-maestro-sample` (카운터 앱 2개 화면 + Maestro 플로우 8개 + GitHub Actions CI)

## 1편 — AI가 테스트 스크립트를 작성하고, 실행하고, 보고서까지 만든다 (CLI 자동화)

**핵심 메시지**: "이미 만들어진 앱이 있다면, AI에게 화면만 보여줘도 테스트 코드를 짜준다. 그다음은 명령어 한 줄로 끝."

**목차 초안**
1. 왜 모바일 앱도 자동화 테스트가 필요한가 (수동 테스트의 한계)
2. Maestro 소개 — 설치 없이 YAML 몇 줄로 UI를 조작하는 도구
3. 샘플 앱 소개 — 카운터 앱 (+1 / -1 / 초기화 / 10 달성 메시지)
4. AI에게 "이 화면을 테스트하는 Maestro 스크립트 짜줘"라고 요청하는 과정
5. 실제로 만들어진 4개 플로우 살펴보기 (tapOn, assertVisible, repeat 등 핵심 커맨드)
6. 단말 연결 확인 → 빌드 → 설치 → 테스트 실행 → HTML 리포트 생성까지, 스크립트 한 번으로 끝내기
7. 리포트 해석하기 (PASS/FAIL, 소요 시간, 스텝별 로그)
8. 마무리 및 2편 예고

**준비물**: 연결된 Android 단말(또는 에뮬레이터), adb, Maestro CLI, 이미 작성된 테스트 대상 앱

**상태**: 오늘 구현 완료. 코드/리포트/스크린샷은 저장소 루트 참고.

## 2편 — Maestro Studio로 코드 없이 시각적으로 테스트 만들기

**핵심 메시지**: "YAML을 몰라도 된다. 화면을 보면서 자동완성만으로 테스트를 만든다."

**목차**
1. Maestro Studio란 무엇인가 — CLI에서 분리된 별도 데스크톱 앱(`studio.maestro.dev`)
2. 앱 실행 시 연결된 단말이 자동 인식되고 실시간 미러링되는 화면
3. `id:` 자리에서 뜨는 자동완성 — 화면에 있는 모든 요소 id가 실시간으로 나열되고, 미러링 화면에 하이라이트까지 표시
4. 그 자리에서 `Run Test`로 바로 실행/검증
5. 1편에서 AI가 짠 스크립트와 비교 — 언제 AI에게 맡기고 언제 Studio로 직접 만들지, 실전에서는 함께 쓰는 방식 제안

**준비물**: 1편과 동일 + Maestro Studio 데스크톱 앱

**상태**: 작성 완료. `docs/stage2-blog-post.md`, `screenshots/studio_*.png`, `.maestro/05_studio_demo.yaml` 참고.

**진행 중 발견한 이슈**:
- `maestro studio` CLI 명령은 더 이상 지원되지 않고 데스크톱 앱 설치가 필요함 (설치는 사용자가 직접 진행)
- CLI(2.6.1)와 Studio 데스크톱 앱이 단말에 서로 다른 버전의 드라이버 앱(`dev.mobile.maestro`)을 설치하면서 `maestro test` 실행 시 "was not installed" 오류가 발생 → `--reinstall-driver` 옵션으로 해결

## 3편 — GitHub Actions로 커밋마다 자동 실행하기

**핵심 메시지**: "실행 버튼조차 누르지 않는다. 푸시하면 알아서 돌아간다."

**목차**
1. 왜 CI가 필요한가 (사람이 실행하는 한 "내 컴퓨터에선 됐는데"가 반복된다)
2. 워크플로우 YAML 하나로 끝 — `reactivecircus/android-emulator-runner`로 실제 단말 없이 에뮬레이터 구동
3. 처음 실행은 실패했다 — `gradlew` 실행 비트 누락(exit code 126) 트러블슈팅
4. 수정 후 5분 만에 5개 플로우 전체 통과, 리포트를 Artifact로 보존
5. 로컬(Windows)과 CI(Linux)의 서로 다른 함정 비교 (인코딩 vs 실행 권한)

**준비물**: 1편과 동일한 저장소 + GitHub 저장소(공개), 별도 단말 불필요 (에뮬레이터 사용)

**상태**: 작성 완료. `docs/stage3-blog-post.md`, `.github/workflows/maestro-test.yml` 참고. 실제 Actions 실행: [#1(실패)](https://github.com/JsonCorp/android-autotest-maestro-sample/actions/runs/29155524080), [#2(성공)](https://github.com/JsonCorp/android-autotest-maestro-sample/actions/runs/29155582282)

**진행 중 발견한 이슈**:
- `gradlew`가 `100644`(실행 불가) 권한으로 커밋되어 있어 리눅스 러너에서 `exit code 126`으로 즉시 실패 → `git update-index --chmod=+x gradlew`로 해결
- Chrome 자동화 도구로 캡처한 스크린샷은 이 환경에서 로컬 파일로 저장할 수 없어, 3편은 스크린샷 대신 실제 API로 가져온 단계별 로그와 공개 저장소의 라이브 Actions 링크로 대체

## 4편 — 화면이 늘어나면 테스트는 어떻게 달라지나 (실전 확장)

**핵심 메시지**: "실제 앱은 화면이 하나가 아니다. 화면 전환·입력·리스트·비동기 로딩이 생기는 순간 필요한 Maestro 기법과, 테스트하기 좋은 앱을 만드는 개발 습관을 함께 다룬다."

**목차**
1. 카운터 앱에 '기록 화면' 추가 — 내비게이션 + 텍스트 입력 + 스크롤 목록 + 비동기 로딩(2초 스피너)
2. 개발자 관점: 테스트 가능한 앱은 개발 단계에서 만들어진다 (testTag 컨벤션, `rememberSaveable` 상태 호이스팅)
3. 새 Maestro 커맨드 실전 적용 — `inputText`/`hideKeyboard`(입력), `scrollUntilVisible`(스크롤 목록), `extendedWaitUntil`(비동기 대기), `back`(백스택 검증)
4. 플로우가 늘어나면 중복이 생긴다 — `runFlow` 서브플로우로 공통화 (`common/` 하위 폴더는 디렉터리 실행에서 자동 제외)
5. CI는 손도 안 댔다 — 3편에서 만든 파이프라인이 새 플로우까지 알아서 실행 (3편의 payoff)

**준비물**: 1편과 동일 (연결된 Android 단말 또는 에뮬레이터, adb, Maestro CLI)

**상태**: 작성 완료. `docs/stage4-blog-post.md`, `screenshots/stage4_*.png`, `.maestro/06~08` + `common/` 참고. 로컬 8/8 통과(1m 48s), CI도 워크플로우 수정 없이 8/8 통과(6m 12s): [Actions 실행](https://github.com/JsonCorp/android-autotest-maestro-sample/actions/runs/29188019490)

**진행 중 발견한 이슈**:
- 플로우 `name:`에 큰따옴표(`"`)를 넣었더니, Maestro가 `--debug-output` 저장 시 플로우 이름을 파일명에 그대로 사용하면서 Windows 파일명 금지 문자에 걸려 `FileNotFoundException`으로 러너 전체가 중단됨 (뒤 플로우들 실행 안 됨) → 플로우 이름에서 따옴표 제거로 해결. 1편(cp949 인코딩), 3편(실행 비트)에 이은 Windows 특유의 함정

## 5편 — 테스트가 깨지는 날: 실패를 빠르게 찾고 팀에 알리기

**핵심 메시지**: "초록불은 결과일 뿐이다. 자동화의 진짜 실력은 빨간불이 떴을 때 드러난다 — 실패를 빠르게 읽고, flaky를 줄이고, 팀에 알린다."

**목차**
1. 지금까진 초록불만 봤다 — CI의 목적은 초록이 아니라 '빨강을 빨리 알려주는 것'
2. 일부러 빨간불을 만든다 — 앱에 off-by-one 버그(`count >= 10` → `> 10`)를 심어 별도 브랜치로 push → `04_milestone` 실패 → CI 빨간불 (main은 초록 유지)
3. 빨간불 열어보기 — 실패 로그의 실패 스텝 + `--debug-output`(+`--flatten-debug-output`)이 실패할 때만 남기는 스크린샷으로 '기대 vs 실제' 확인
4. 원하는 순간을 증거로 — `takeScreenshot`을 `reports/` 아래로 보내 CI 아티팩트에 보존(이름만 주면 CWD에 떨어져 CI에서 사라지는 함정)
5. flaky 줄이기 — 고정 sleep 금지, `extendedWaitUntil`(4편 복습) + `waitForAnimationToEnd`
6. 팀이 알게 하기 — README 상태 배지 + 실패 시 PR 자동 코멘트(`actions/github-script`, GITHUB_TOKEN만, 시크릿 불필요). Slack 웹훅은 확장 아이디어로만 언급
7. 버그 고치고 다시 초록불 → 정리 표(실패 상황 → 대응 도구)

**준비물**: 1편과 동일 (연결된 Android 단말 또는 에뮬레이터, adb, Maestro CLI) + 공개 GitHub 저장소

**상태**: 작성 완료. `docs/stage5-blog-post.md`, `screenshots/stage5_3_debug_shot.png`, `.maestro/04·08`·`.github/workflows/maestro-test.yml`·`README.md` 참고. 로컬 8/8 통과(1m 48s, Maestro 2.6.1). 실패 브랜치 CI 라이브 링크는 push 후 원고·본 문서에 반영 예정.

**진행 중 발견한 이슈**:
- Maestro 2.6.1의 `maestro test`에는 CLI 레벨 재시도 플래그가 없음 → flaky 대응은 커맨드 레벨 조건 대기(`extendedWaitUntil`, `waitForAnimationToEnd`)로 정리
- `--debug-output`은 **통과 시에는 스크린샷을 안 남기고 실패 시에만** 실패 지점 스크린샷을 남김(`screenshot-❌-…png`). 실패 재현 없이는 이 산출물을 볼 수 없음
- `takeScreenshot`에 경로를 안 주면 저장소 루트(CWD)에 저장되어 CI 아티팩트(`reports/`)에 안 담김 → `reports/` 하위 경로로 지정해 해결
