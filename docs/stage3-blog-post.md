# AI와 Maestro로 안드로이드 앱 자동화 테스트하기 (3) — GitHub Actions로 커밋마다 자동 실행하기

[1편](stage1-blog-post.md)에서는 AI가 짠 스크립트로 내 PC에서 빌드→테스트→리포트를 자동화했고, [2편](stage2-blog-post.md)에서는 Maestro Studio로 화면을 보면서 테스트를 만들었습니다. 그런데 두 방법 다 "내가 직접 실행 버튼을 눌러야" 한다는 공통점이 있습니다. 이번 편에서는 그 버튼조차 누르지 않도록, **커밋을 푸시할 때마다 GitHub Actions가 알아서 에뮬레이터를 띄우고 테스트를 돌려주는** 파이프라인을 만들어봅니다.

## 1. 왜 CI가 필요한가

로컬에서 테스트를 잘 돌렸어도, 다른 사람이 받은 코드에서는 안 돌아갈 수 있습니다. "내 컴퓨터에선 됐는데"를 막는 가장 확실한 방법은 사람 손을 아예 거치지 않는 것입니다. 커밋이 올라올 때마다 깨끗한 가상 환경에서 처음부터 다시 빌드하고 테스트해서, 결과를 누구나 볼 수 있게 남겨두는 것이 CI(Continuous Integration)입니다.

## 2. 워크플로우 파일 하나로 끝

GitHub Actions는 저장소에 YAML 파일 하나(`.github/workflows/maestro-test.yml`)만 추가하면 바로 동작합니다.

```yaml
name: Maestro E2E Tests

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  workflow_dispatch:

jobs:
  maestro-test:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Enable KVM group perms (emulator hardware acceleration)
        run: |
          echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' | sudo tee /etc/udev/rules.d/99-kvm4all.rules
          sudo udevadm control --reload-rules
          sudo udevadm trigger --name-match=kvm

      - name: Install Maestro CLI
        run: |
          curl -Ls "https://get.maestro.mobile.dev" | bash
          echo "$HOME/.maestro/bin" >> "$GITHUB_PATH"

      - name: Build debug APK
        run: ./gradlew assembleDebug

      - name: Run Maestro flows on emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 30
          target: google_apis
          arch: x86_64
          profile: pixel_6
          disable-animations: true
          script: |
            adb install -r app/build/outputs/apk/debug/app-debug.apk
            mkdir -p reports
            maestro test .maestro/ --format HTML-DETAILED --output reports/report.html --debug-output reports/debug

      - name: Upload Maestro report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: maestro-report
          path: reports/
```

핵심은 `reactivecircus/android-emulator-runner` 액션입니다. GitHub이 제공하는 우분투 러너 위에 진짜 Android 에뮬레이터(API 30, Pixel 6 프로필)를 띄워주기 때문에, 실제 단말 없이도 1~2편에서 쓰던 것과 완전히 똑같은 `maestro test` 명령이 그대로 돌아갑니다. 실행이 끝나면 `--format HTML-DETAILED`로 만든 리포트를 `actions/upload-artifact`로 첨부해서, 실행 결과 화면을 다운로드해서 볼 수 있게 남겨둡니다.

## 3. 처음 실행했을 때 바로 실패했다 — exit code 126

워크플로우를 커밋하고 푸시하자마자 실행됐는데, "Build debug APK" 단계에서 14초 만에 실패했습니다.

```
Process completed with exit code 126.
```

exit code 126은 "실행 권한이 없다"는 뜻입니다. 원인은 `gradlew` 파일이었습니다. Windows에서 다른 프로젝트 파일을 복사해서 이 저장소를 처음 만들었는데, 그 과정에서 실행 비트가 빠진 채로 커밋되어 있었습니다.

```
$ git ls-files -s gradlew
100644 faf93008b77e7b52e18c44e4eef257fc2f8fd76d 0   gradlew
```

`100644`는 일반 파일 권한이고, 실행 가능한 스크립트라면 `100755`여야 합니다. Windows에는 유닉스 실행 비트 개념이 없다 보니, macOS/Linux에서 만든 `gradlew`를 Windows에서 그대로 다루면 이 비트가 깨지기 쉽습니다. 로컬(Windows)에서는 `gradlew.bat`을 쓰기 때문에 전혀 문제가 없었는데, 리눅스 러너에서 `./gradlew`를 직접 실행하려니 바로 걸린 겁니다.

```
git update-index --chmod=+x gradlew
git commit -m "Fix gradlew missing executable bit"
git push
```

이렇게 실행 비트만 고쳐서 다시 푸시하니, 두 번째 실행은 **5분 13초 만에 전부 통과**했습니다.

```
maestro-test  succeeded in 5m 11s
  ✓ Set up job                                          1s
  ✓ Run actions/checkout@v4                              0s
  ✓ Set up JDK 17                                        1s
  ✓ Enable KVM group perms (emulator hardware acceleration)  0s
  ✓ Install Maestro CLI                                  5s
  ✓ Build debug APK                                   2m 17s
  ✓ Run Maestro flows on emulator                     2m 43s
  ✓ Upload Maestro report                                1s
```

실행 결과는 저장소가 공개되어 있어 누구나 직접 볼 수 있습니다: [Actions 실행 #2](https://github.com/JsonCorp/android-autotest-maestro-sample/actions/runs/29155582282)

## 4. 로컬(Windows)과 CI(리눅스)의 차이

1편에서 Windows 콘솔의 한글 인코딩(cp949) 문제 때문에 `JAVA_TOOL_OPTIONS`에 UTF-8을 강제로 지정해야 했던 것, 기억하시나요? 리눅스 러너에서는 기본 로케일이 UTF-8이라 이 문제 자체가 없습니다. 반대로 이번처럼 "실행 권한 비트"는 Windows에서는 신경 쓸 일이 없다가 리눅스로 넘어가는 순간 튀어나오는 문제입니다. 플랫폼이 바뀌면 서로 다른 종류의 함정이 있다는 걸 실제로 겪은 셈입니다.

## 5. 이제 무엇이 달라졌나

- 코드를 `main`에 푸시하거나 PR을 올리면, 사람이 아무것도 하지 않아도 5개 플로우가 전부 자동으로 실행됩니다.
- 실행 결과(HTML 리포트)가 Artifacts로 남아서, 나중에라도 그 시점의 테스트 결과를 다시 열어볼 수 있습니다.
- 실행 여부와 관계없이 "지금 main 브랜치가 정상 동작하는가"를 커밋 옆의 초록/빨강 체크 표시로 바로 확인할 수 있습니다.

1편(AI가 스크립트 작성) → 2편(Studio로 시각적으로 다듬기) → 3편(커밋마다 자동 실행)까지 오면서, 사람이 손대야 하는 부분이 "테스트 작성"만 남고 나머지는 전부 자동화되었습니다.

지금까지는 화면이 하나뿐인 앱이라 테스트도 단순했습니다. 다음 편에서는 앱에 화면을 하나 더 추가하면서(화면 전환, 텍스트 입력, 스크롤 목록, 비동기 로딩), 실제 앱을 테스트할 때 반드시 마주치는 문제들을 Maestro로 어떻게 푸는지 다뤄보겠습니다.

---

**태그**: Android, Maestro, GitHubActions, CI/CD, 안드로이드, UI자동화테스트, 모바일테스트자동화, Kotlin, JetpackCompose, 테스트자동화, QA, DevOps
