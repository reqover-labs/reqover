[English](README.md) | **한국어** | [프로젝트](../README.ko.md)

# Reqover CLI

이미 저장한 coverage 리포트를 읽는 오프라인 명령입니다. agent 연결, 앱 기동,
HTTP 요청 전송이나 테스트 실행을 대신하지 않습니다.

[v0.4.1 릴리스](https://github.com/reqover-labs/reqover/releases/tag/v0.4.1)에서
`reqover-cli-0.4.1.jar`를 받습니다. Maven Central 라이브러리가 아니며 JDK 17 이상이
필요합니다. 아래는 JAR이 현재 폴더에 있고 입력 파일을 미리 준비했다고 가정합니다.

```bash
java -jar reqover-cli-0.4.1.jar version
java -jar reqover-cli-0.4.1.jar help
java -jar reqover-cli-0.4.1.jar render --report report.json --out report.html
java -jar reqover-cli-0.4.1.jar diff --baseline before.json --current after.json --format markdown
java -jar reqover-cli-0.4.1.jar impact --report report.json --changed src/main/java/com/example/OrderService.java --format markdown
```

`render`는 `--report`가 필수입니다. `--out`이 없으면 파일 대신 화면에 HTML을
출력합니다. `impact`는 `--report`와 함께 `--changed` 또는 `--changed-files` 중
하나만 지정합니다. 파이프로 경로를 받으려면 `--changed-files -`를 사용합니다.

| 명령 | 결과 |
| --- | --- |
| `render` | 입력에 남은 상세를 이용해 서버 없이 열리는 HTML 생성 |
| `diff` | endpoint·코드 차이. 시간·상태 변화 비교는 아님 |
| `impact` | 관측된 재테스트 대상과 매칭되지 않은 파일 |

종료 코드는 성공 `0`, 명시적으로 켠 `--fail-on-impact`·`--fail-on-change` 게이트에
걸리면 `1`, 사용법·입력 오류는 `2`입니다. 매칭이 없다고 안전하다는 뜻은 아닙니다.
테스트 초안이나 대시보드 요약 JSON이 아닌 coverage report JSON을 사용합니다.

소스 빌드는 `./gradlew :reqover-cli:shadowJar`입니다. 내려받은 이름 대신
`reqover-cli/build/libs/reqover-cli-0.4.1.jar` 경로를 사용합니다.

[CI 사용 안내](../docs/18_ci_impact_analysis.ko.md) |
[시간·상태 비교](../docs/28_recording_comparison.ko.md)
