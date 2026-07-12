# AI와 Maestro로 안드로이드 앱 자동화 테스트하기 (5) — 테스트가 깨지는 날: 실패를 빠르게 찾고 팀에 알리기

[1편](stage1-blog-post.md)에서 AI가 짠 스크립트로 테스트를 자동화했고, [2편](stage2-blog-post.md)에서 Maestro Studio로 화면을 보며 테스트를 만들었고, [3편](stage3-blog-post.md)에서 커밋마다 CI가 돌려주게 했고, [4편](stage4-blog-post.md)에서 화면이 늘어난 실전 앱까지 테스트했습니다. 그런데 시리즈 내내 리포트는 늘 **초록불**이었습니다. 사실 자동화 테스트가 정말 필요한 순간은 반대입니다. 초록불일 땐 아무 일도 없습니다. **빨간불이 떴을 때**, 그것도 내가 화면을 안 보고 있을 때 대신 잡아주는 것 — 그게 CI의 존재 이유죠. 이번 편은 지금까지 한 번도 안 보여준 그 장면, "테스트가 깨지는 날"을 다룹니다. 실패를 어떻게 읽고, flaky를 어떻게 줄이고, 팀에 어떻게 알리는지까지요.

## 1. 지금까진 초록불만 봤다

4편까지 오는 동안 8개 플로우는 늘 통과했습니다. 그래서 한 가지 중요한 질문을 아직 안 던졌습니다. **"빨간불이 뜨면 나는 무엇을 보게 되나?"** CI의 가치는 초록불을 만드는 게 아니라 **빨강을 빨리, 정확하게 알려주는 것**입니다. 초록불만 보다가 처음 빨간불을 만나면 대부분 이렇게 됩니다.

- Actions 페이지엔 빨간 ❌만 떠 있고,
- 로그는 수백 줄인데,
- "그래서 **어디서, 왜** 깨졌는데?"에 답이 안 나온다.

이번 편에서는 일부러 빨간불을 만들어서, 그 빨간불을 열어보고 원인까지 5분 안에 짚는 과정을 따라가 봅니다.

## 2. 일부러 빨간불을 만든다

가장 현실적인 실패는 "테스트를 잘못 짜서"가 아니라 **"앱 코드에 버그가 들어가서"** 깨지는 경우입니다. 그래서 앱에 흔한 실수 하나를 심어봤습니다. 카운터 화면의 축하 메시지 노출 조건에서 등호를 빠뜨린 **off-by-one** 버그입니다.

```kotlin
// 원래: 10에 도달하면(>=) 메시지 표시
// 버그: 10을 초과해야(>) 표시 → 정확히 10에서는 안 뜬다
if (count > MILESTONE) {   // was: count >= MILESTONE
    Text(text = "🎉 $MILESTONE 달성!", ... )
}
```

이 한 글자 때문에 카운트가 **정확히 10일 때 축하 메시지가 안 뜹니다.** 사람이 손으로 테스트하면 "10에서 11까지 눌러보다가" 놓치기 딱 좋은 종류의 버그죠. 하지만 4편에서 만든 `04_milestone.yaml` 플로우는 정확히 10에서 메시지를 검증합니다.

```yaml
- repeat:
    times: 10
    commands:
      - tapOn:
          id: "btn_increment"

- assertVisible:
    id: "text_counter"
    text: "10"
- assertVisible:
    id: "text_milestone"   # ← 버그 때문에 여기서 깨진다
```

이 버그를 별도 브랜치로 올리면, [3편](stage3-blog-post.md)에서 만든 워크플로우가 PR에서 알아서 에뮬레이터를 띄우고 테스트를 돌리다가 **빨간불**을 띄웁니다. (3편에서 `gradlew` 실행 권한 때문에 첫 CI가 실패했던 것처럼, 이번엔 일부러 만든 실패 실행을 라이브로 남겨둡니다. `main`에는 버그를 병합하지 않아 초록불을 유지합니다.)

> 실패 실행(라이브): <!-- 버그 브랜치 push 후 실제 Actions run 링크로 교체 -->

## 3. 빨간불을 열어본다

빨간불을 클릭하면 로그에 실패 지점이 이렇게 찍혀 있습니다.

```
Assert that "10", id: text_counter is visible... COMPLETED
Assert that id: text_milestone is visible... FAILED

Assertion is false: id: text_milestone is visible

Assertion 'id: text_milestone is visible' failed. Check the UI hierarchy
in debug artifacts to verify the element state and properties.
```

로그만 봐도 "카운트 10까지는 맞는데(직전 스텝 COMPLETED), `text_milestone`이 안 보인다"까지는 압니다. 그런데 **왜** 안 보이는지는 로그만으론 애매합니다. 요소 id가 틀린 걸까, 로딩이 덜 된 걸까, 진짜 버그일까? Maestro는 이 판단을 위해 **실패한 순간의 증거**를 남깁니다.

3편에서 워크플로우에 이미 넣어둔 `--debug-output` 덕분입니다. 여기에 이번 편에서 옵션 하나를 더했습니다.

```yaml
# .github/workflows/maestro-test.yml
maestro test .maestro/ --format HTML-DETAILED \
  --output reports/report.html \
  --debug-output reports/debug --flatten-debug-output
```

`--flatten-debug-output`은 실행마다 생기던 타임스탬프 하위 폴더를 없애고 **모든 산출물을 한 폴더에 평평하게** 모아줍니다. CI 아티팩트에서 실패 스크린샷을 클릭 몇 번 없이 바로 찾으라고 있는 옵션입니다(Maestro `--help`에도 "Useful for CI"라고 적혀 있습니다). 실패한 실행의 `reports/debug/`에는 이렇게 남습니다.

```
reports/debug/
├─ screenshot-❌-<타임스탬프>-(04_milestone.yaml).png   ← 실패 순간 화면
├─ commands-(04_milestone.yaml).json                    ← 스텝별 실행 기록
└─ maestro.log
```

핵심은 첫 번째 파일, **실패 순간의 스크린샷**입니다. 통과할 때는 안 생기고, **깨질 때만** Maestro가 자동으로 그 화면을 찍어둡니다. 열어보면 답이 한눈에 나옵니다.

![실패 순간 스크린샷 — 카운트는 10인데 축하 메시지가 없다](../screenshots/stage5_3_debug_shot.png)
*카운트는 분명히 `10`인데 `🎉 10 달성!`이 없다. "기대(메시지 있음) vs 실제(없음)"가 그림 한 장으로 드러난다 — id가 틀린 게 아니라 앱이 10에서 메시지를 안 그린 것, 즉 진짜 버그다.*

로그가 "무엇이 실패했나"를 알려준다면, 이 스크린샷은 "실제로 화면이 어땠나"를 보여줍니다. 둘을 겹치면 원인 추적이 추측이 아니라 확인이 됩니다.

## 4. 원하는 순간을 증거로 남기기 — `takeScreenshot`

Maestro는 실패할 때만 화면을 찍지만, **통과하든 실패하든 남기고 싶은 순간**이 있습니다. 그럴 땐 플로우 안에 직접 스크린샷 명령을 넣습니다. 사실 `04_milestone.yaml`에는 처음부터 이게 있었습니다.

```yaml
- assertVisible:
    id: "text_milestone"

- takeScreenshot: reports/milestone_reached   # 달성 화면을 증거로 남긴다
```

그런데 여기 함정이 하나 있었습니다. `takeScreenshot: milestone_reached`처럼 이름만 주면 파일이 **실행한 위치(저장소 루트)** 에 떨어집니다. 로컬에선 보이지만, **CI에선 이 파일이 아티팩트(`reports/`)에 안 담겨서 사라집니다.** 애써 찍은 증거가 CI에선 정작 없는 셈이죠. 그래서 이번 편에서 경로를 `reports/` 아래로 바꿨습니다.

```yaml
# 이름만: 저장소 루트(CWD)에 저장 → CI 아티팩트에 안 담김
- takeScreenshot: milestone_reached
# 경로 지정: reports/ 아래 → CI가 아티팩트로 함께 보존
- takeScreenshot: reports/milestone_reached
```

작은 차이지만, "로컬에선 됐는데 CI에선 증거가 없더라"를 막아주는 실전 습관입니다.

## 5. flaky를 줄인다 — 고정 sleep 대신 조건 대기

빨간불이 다 진짜 버그면 차라리 낫습니다. 더 골치 아픈 건 **어쩌다 한 번씩 깨지는(flaky)** 테스트입니다. 통과했다 깨졌다 하면 팀은 곧 빨간불을 무시하기 시작하고, 그 순간 CI는 죽은 것과 같습니다. flaky의 최대 원인은 **타이밍**입니다. 화면 전환 애니메이션, 비동기 로딩이 끝나기 전에 다음 요소를 확인하러 가면 간헐적으로 실패합니다.

가장 나쁜 대응이 "그럼 좀 기다리자"며 **고정 sleep**을 넣는 것입니다. 로딩이 빠르면 시간을 낭비하고, 어쩌다 느리면 그대로 깨지니까요. Maestro는 아예 고정 sleep 명령을 기본기로 밀지 않습니다. 대신 **조건이 충족될 때까지만** 기다리는 도구를 씁니다.

- `extendedWaitUntil` (4편에서 사용) — 특정 요소가 나타날 때까지 대기, 상한(timeout)만 지정
- `waitForAnimationToEnd` — 화면 전환/스크롤 애니메이션이 끝날 때까지 대기

이번 편에서는 `08_async_wait.yaml`의 뒤로가기 직후에 `waitForAnimationToEnd`를 더했습니다. 화면 전환 애니메이션 도중에 카운터 화면을 검증하면 간헐적으로 깨질 수 있기 때문입니다.

```yaml
# 시스템 뒤로가기로 카운터 화면 복귀 (백스택 검증)
- back

# 화면 전환 애니메이션이 끝나기 전에 검증하면 간헐적으로 깨진다(flaky).
# 고정 sleep 대신 애니메이션 종료를 조건으로 기다린다.
- waitForAnimationToEnd

- assertVisible:
    id: "text_counter"
```

원칙은 하나입니다. **"몇 초 기다려"가 아니라 "무엇이 될 때까지 기다려".** 시간을 세지 말고 상태를 기다리면, 빠른 기기에선 빨라지고 느린 기기에선 안 깨집니다.

## 6. 팀이 알게 한다 — 배지와 자동 코멘트

빨간불을 잘 읽을 줄 알아도, **아무도 안 보면** 소용이 없습니다. CI의 마지막 한 걸음은 "실패를 사람에게 밀어 넣는 것"입니다. 외부 서비스나 시크릿 없이, GitHub만으로 두 가지를 붙였습니다.

**(1) 상태 배지** — README 맨 위에 현재 `main`이 초록인지 빨강인지 한눈에 보이는 배지를 답니다. 저장소를 여는 사람 누구나 즉시 현재 상태를 압니다.

```markdown
[![Maestro E2E Tests](https://github.com/<owner>/<repo>/actions/workflows/maestro-test.yml/badge.svg)](https://github.com/<owner>/<repo>/actions/workflows/maestro-test.yml)
```

**(2) 실패 시 PR 자동 코멘트** — PR에서 테스트가 깨지면, 워크플로우가 실행 링크와 아티팩트 위치를 PR에 코멘트로 남깁니다. 리뷰어가 Actions 탭을 뒤질 필요 없이, PR 화면에서 바로 "무엇을 보면 되는지" 알게 됩니다. `actions/github-script`로 `GITHUB_TOKEN`만 써서(별도 시크릿 불필요) 구현합니다.

```yaml
- name: Comment on PR when tests fail
  if: failure() && github.event_name == 'pull_request'
  uses: actions/github-script@v7
  with:
    script: |
      const runUrl = `${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId}`;
      const body = [
        '❌ **Maestro E2E 테스트가 실패했습니다.**',
        '',
        `- 실행 로그: ${runUrl}`,
        '- 실패한 스텝의 스크린샷은 위 실행의 Artifacts → `maestro-report`에서 확인할 수 있습니다.',
      ].join('\n');
      await github.rest.issues.createComment({
        owner: context.repo.owner, repo: context.repo.repo,
        issue_number: context.issue.number, body,
      });
```

이걸 쓰려면 워크플로우(또는 잡)에 `permissions: pull-requests: write` 한 줄이 필요합니다. 더 나아가고 싶다면 Slack 웹훅으로 채널에 알림을 보낼 수도 있지만, 그건 웹훅 URL을 저장소 시크릿에 넣어야 하므로 여기서는 확장 아이디어로만 남겨둡니다.

## 7. 버그를 고치고, 다시 초록불

원인을 확인했으니 고치는 건 한 글자입니다. `>`를 `>=`로 되돌리면 됩니다. 그리고 다시 돌리면 8개가 전부 통과합니다.

```
[Passed] 증가 — +1 버튼을 3번 누르면 3이 표시된다 (9s)
[Passed] 경계값 — 0에서 -1을 눌러도 음수로 내려가지 않는다 (6s)
[Passed] 초기화 — +1을 5번 누른 뒤 초기화하면 0으로 돌아간다 (14s)
[Passed] 달성 메시지 — 10에 도달하면 축하 메시지가 뜨고, 초기화하면 사라진다 (25s)
[Passed] Studio 데모 — Maestro Studio에서 클릭/자동완성만으로 조립한 플로우 (4s)
[Passed] 기록 저장 — 카운트 7에서 이름을 입력해 저장하면 목록 끝에 Maestro - 7이 남는다 (32s)
[Passed] 목록 스크롤 — 화면 밖에 있는 마지막 예시 기록까지 스크롤해서 찾는다 (8s)
[Passed] 비동기 로딩 — 스피너가 사라질 때까지 기다렸다가 목록을 확인하고 뒤로 돌아온다 (10s)

8/8 Flows Passed in 1m 48s
```

방금 우리가 겪은 게 자동화 테스트의 진짜 값어치입니다. **10에서만 안 뜨는 축하 메시지** 같은 버그는 수동 테스트로는 놓치기 쉽지만, 커밋을 올리는 순간 CI가 대신 눌러보고, 스크린샷까지 찍어서, PR에 코멘트로 알려줬습니다. 사람이 한 일은 `>`를 `>=`로 고친 것뿐입니다.

## 8. 정리

| 실패 상황 | 대응 도구 |
|---|---|
| 빨간불이 떴는데 원인을 모름 | `--debug-output`의 실패 스크린샷 + `commands-*.json` |
| CI 아티팩트에서 스크린샷 찾기 번거로움 | `--flatten-debug-output` |
| 통과/실패와 무관하게 증거를 남기고 싶음 | `takeScreenshot: reports/<name>` (경로를 `reports/` 아래로) |
| 간헐적 실패(flaky) | 고정 sleep 금지, `extendedWaitUntil` · `waitForAnimationToEnd` |
| 현재 상태를 모두에게 | README 상태 배지 |
| 실패를 사람에게 밀어넣기 | 실패 시 PR 자동 코멘트(`actions/github-script`) |

1편(AI가 스크립트 작성) → 2편(Studio로 시각적으로) → 3편(CI로 자동 실행) → 4편(실전 앱 확장) → 5편(실패를 빠르게 찾고 알리기)까지, 테스트를 **만들고 → 자동으로 돌리고 → 깨졌을 때 대처하는** 한 사이클을 완성했습니다. 초록불은 결과일 뿐이고, 좋은 자동화의 진짜 실력은 빨간불이 떴을 때 드러납니다. 이 시리즈의 저장소는 공개되어 있으니, 직접 버그를 하나 심어보고 CI가 어떻게 잡아내는지 확인해 보시기 바랍니다.

---

**태그**: Android, Maestro, 안드로이드, UI자동화테스트, 모바일테스트자동화, CI, GitHubActions, 테스트자동화, QA, flaky테스트, 디버깅, takeScreenshot, waitForAnimationToEnd
