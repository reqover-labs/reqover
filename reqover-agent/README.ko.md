[English](README.md) | **한국어** | [프로젝트](../README.ko.md)

# Reqover Java Agent

포함 대상으로 지정한 애플리케이션 클래스를 로드할 때 기록 코드를 넣습니다.
Spring starter 또는 직접 관리한 `UnitScope`와 함께 사용합니다. agent만 붙인다고
HTTP 요청 귀속이나 리포트 엔드포인트가 생기는 것은 아닙니다.

[v0.4.2 릴리스](https://github.com/reqover-labs/reqover/releases/tag/v0.4.2)에서
`reqover-agent-0.4.2.jar`와 체크섬 파일을 받습니다. Maven Central 라이브러리가 아닌
의존성을 묶은 실행 JAR이며 JDK 17 이상이 필요합니다.

```bash
java -javaagent:reqover-agent-0.4.2.jar=include=com.example -jar app.jar
```

| 옵션 | 기본값 | 의미 |
| --- | --- | --- |
| `include` | 비어 있음 | 클래스·패키지 접두어. 없으면 계측하지 않음 |
| `exclude` | 프레임워크 접두어 | 가장 긴 접두어가 우선하며 같은 길이면 제외 |
| `accessors` | `skip` | `record`로 단순 getter·setter·builder도 영향 분석에 포함 |
| `references` | `skip` | `record`로 포함 대상 인터페이스 호출·정적 필드 참조도 관측 |

옵션은 쉼표로, 여러 접두어는 세미콜론으로 구분합니다. 세미콜론이 있으면 인자 전체를
따옴표로 감쌉니다. 내부 런타임과 생성 프록시의 강제 제외는 해제할 수 없습니다.
참조 관측은 구현 내부가 실행됐다는 뜻이 아니며 호출 순서·줄/분기·메서드 시간을 측정하지 않습니다.

소스 빌드는 저장소 루트에서 `./gradlew :reqover-agent:shadowJar`를 실행합니다.
JAR은 `reqover-agent/build/libs/`에 생성됩니다.

[연동과 옵션 설명](../docs/17_integration_guide.ko.md) |
[계측 모듈](../reqover-instrumentation/README.ko.md)
