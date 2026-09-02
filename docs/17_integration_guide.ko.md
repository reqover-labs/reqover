**한국어** | [English](17_integration_guide.md)

# 17. Reqover를 내 Spring 프로젝트에 붙이기

데모가 아니라 **내 애플리케이션**에서 요청별 실행 기록을 보려면 이 문서를 따라오면 됩니다. 처음이면 20~30분 정도 걸립니다.

아직 데모를 안 돌려보셨다면 [README의 "5분 만에 직접 보기"](../README.ko.md#5분-만에-직접-보기)를 먼저 해보시는 걸 권합니다. 데모가 되는 걸 본 다음에 붙이는 게 문제를 찾기 훨씬 쉽습니다.

> [!IMPORTANT]
> Reqover `0.2.0`은 **Maven Central에 아직 없습니다.** 서명된 배포 파이프라인은 만들어져 있지만(`./gradlew centralBundle`과, 저장소가 켜기 전까지는 동작하지 않는 릴리스 잡) 아직 실행한 적이 없어서 지금 Central에서 받을 수 있는 것은 없습니다. 소스를 직접 빌드해 로컬 Maven 저장소에 넣거나, GitHub Release의 jar를 받아 써야 합니다. 개발·QA·스테이징 환경을 전제로 만들었고, 운영 환경에 상시로 켜두는 용도가 아닙니다.

---

## 전체 흐름

붙이는 작업은 네 단계입니다. **어떤 단계도 내 소스 코드를 수정하지 않습니다.**

| 단계 | 하는 일 | 왜 필요한가 |
| --- | --- | --- |
| 1 | 라이브러리 받기 | Maven Central에 아직 없어서 |
| 2 | 의존성 1개 추가 | 요청과 실행 기록을 연결하는 부분 |
| 3 | 리포트를 어떻게 볼지 정하기 | 보는 방법 둘 다 기본이 꺼짐이라서 |
| 4 | Java agent 붙여서 실행 | 실제로 실행을 기록하는 부분 |

### 미리 정해야 할 것

- **JDK 17 또는 21**
- **Spring Boot 3.x** — 샘플은 3.5.16에서 검증했습니다
- **MVC인지 WebFlux인지** — 스타터는 어댑터 둘 다 들고 있고, 맞는 쪽만 켜집니다

---

## 1. 라이브러리 받기

### 방법 A — 소스에서 빌드 (권장)

`v0.2.0` 태그를 받아서 로컬 Maven 저장소에 설치합니다.

```bash
git clone --branch v0.2.0 --depth 1 https://github.com/reqover-labs/reqover.git
cd reqover
./gradlew clean publishToMavenLocal
```

Windows는 `.\gradlew.bat clean publishToMavenLocal`입니다.

설치되는 것은 `io.reqover` 그룹의 라이브러리 모듈입니다.

| 아티팩트 | 버전 |
| --- | --- |
| `io.reqover:reqover-core` | `0.2.0` |
| `io.reqover:reqover-instrumentation` | `0.2.0` |
| `io.reqover:reqover-report` | `0.2.0` |
| `io.reqover:reqover-spring-mvc` | `0.2.0` |
| `io.reqover:reqover-spring-webflux` | `0.2.0` |
| `io.reqover:reqover-spring-boot-starter` | `0.2.0` |

**`reqover-agent`와 `reqover-cli`는 여기 없습니다.** 둘 다 의존성이 섞이지 않게 따로 묶은(shaded) 실행 파일이고 컴파일할 때 참조하는 라이브러리가 아니라서 배포 대상이 아닙니다. GitHub Release에서 파일로 받습니다 → [4단계](#4-java-agent-붙여서-실행).

설치가 됐는지 확인:

```bash
ls ~/.m2/repository/io/reqover        # macOS / Linux
```

```powershell
Get-ChildItem "$env:USERPROFILE\.m2\repository\io\reqover"   # Windows
```

### 방법 B — GitHub Release 번들

[v0.2.0 릴리스](https://github.com/reqover-labs/reqover/releases/tag/v0.2.0)에는 `reqover-0.2.0.zip`이 있고, 그 안에 같은 라이브러리 jar가 `lib/`에, 소스가 `sources/`에, 그리고 `reqover-agent-0.2.0.jar`와 `reqover-cli-0.2.0.jar`가 최상위에 들어 있습니다. 소스를 빌드할 수 없는 상황이면 이 방법을 쓰되, `lib/`의 jar를 빌드가 찾을 수 있는 곳(flat-dir 저장소나 사내 Nexus/Artifactory)에 직접 올려야 합니다.

어느 쪽이든 같은 릴리스의 `reqover-0.2.0-SHA256SUMS.txt`로 파일이 온전한지 확인할 수 있습니다.

---

## 2. 의존성 추가

### 스타터 (권장)

**의존성 1개**입니다. `reqover-spring-boot-starter`가 `reqover-core`, `reqover-report`, 그리고 어댑터 둘 다를 함께 가져오고, 리포트 엔드포인트와 종료 시 내보내기를 위한 Spring Boot 자동 설정을 추가합니다.

`mavenLocal()`을 저장소 목록에 넣어야 하고, 1단계에서 설치한 것을 먼저 찾도록 `mavenCentral()`보다 앞에 둡니다.

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.reqover:reqover-spring-boot-starter:0.2.0")
}
```

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'io.reqover:reqover-spring-boot-starter:0.2.0'
}
```

`mavenLocal`은 Maven에서 기본으로 쓰이므로 저장소 설정을 따로 추가할 필요가 없습니다.

```xml
<dependency>
  <groupId>io.reqover</groupId>
  <artifactId>reqover-spring-boot-starter</artifactId>
  <version>0.2.0</version>
</dependency>
```

**어댑터 둘을 다 들고 있어도 안전합니다.** 각 어댑터는 웹 애플리케이션 타입뿐 아니라 필요한 클래스의 존재 여부까지 조건으로 걸려 있어서, 서블릿 애플리케이션은 MVC 어댑터만 켜고 WebFlux 쪽은 건드리지 않습니다. 반대도 마찬가지입니다.

### 개별 모듈

어댑터 하나만 쓰고 스타터는 원하지 않는다면 어댑터와 리포트 모듈을 직접 넣으면 됩니다. 대신 리포트 엔드포인트, 종료 시 내보내기, `ReqoverReportService`는 포기하게 됩니다 — 셋 다 스타터에 있습니다. 리포트는 `CoverageStore`에서 직접 만들어야 합니다.

```kotlin
dependencies {
    // Spring MVC 프로젝트라면
    implementation("io.reqover:reqover-spring-mvc:0.2.0")

    // Spring WebFlux 프로젝트라면 (위 대신)
    // implementation("io.reqover:reqover-spring-webflux:0.2.0")

    implementation("io.reqover:reqover-report:0.2.0")
}
```

### 이것만으로 무슨 일이 일어나나

어댑터의 자동 설정이 `CoverageStore` 빈을 만들고, 들어오는 요청마다 기록함을 하나씩 붙입니다. 여기까지는 **기록만 쌓입니다.** 아직 볼 방법이 없고, agent도 안 붙었으니 기록될 내용도 없습니다.

> [!NOTE]
> **기본 상태에서는 아무것도 열리지 않습니다.** 리포트 엔드포인트는 꺼진 채로 배포되고, 종료 시 내보내기도 경로를 지정하기 전까지는 아무것도 쓰지 않습니다. 커버리지 리포트는 내부 구조가 그대로 드러나는 데이터인데 인증 방식과 네트워크 정책은 환경마다 다르므로, 어디에 열지를 라이브러리가 몰래 정하지 않습니다. [3단계](#3-리포트를-어떻게-볼지-정하기)를 보세요.

### WebFlux를 쓴다면 알아야 할 것

WebFlux 어댑터는 요청 정보가 스레드를 넘어 따라가게 하려고 **Reactor의 컨텍스트 자동 전달 기능을 JVM 전체에 켭니다.** Reqover가 JVM 전역에 손대는 유일한 지점입니다. 이게 필요 없거나 곤란하면, 애플리케이션이 시작되기 전에 아래 속성으로 어댑터 전체를 끌 수 있습니다.

```properties
reqover.webflux.enabled=false
```

---

## 설정 속성 레퍼런스

Reqover가 읽는 속성 전부와, 지정하지 않았을 때 적용되는 기본값입니다.

`reqover.mvc.*`는 `reqover-spring-mvc`, `reqover.webflux.*`는 `reqover-spring-webflux`, `reqover.report.*`는 `reqover-spring-boot-starter`에서 옵니다. 스타터는 셋 다 들고 있습니다.

| 속성 | 기본값 | 하는 일 |
| --- | --- | --- |
| `reqover.mvc.enabled` | `true` | 요청 추적을 아예 붙일지 여부. `false`면 MVC 어댑터가 컨텍스트에 올라오지 않습니다 |
| `reqover.mvc.include-path-patterns` | `/**` | 인터셉터가 추적할 Ant 경로 패턴. 기본은 전체입니다 |
| `reqover.mvc.exclude-path-patterns` | `/reqover`, `/reqover/**`, `/error` | 추적에서 제외할 경로. 이 속성을 지정하면 기본 목록을 **대체**합니다 |
| `reqover.mvc.max-snapshots` | `10000` | 기본 인메모리 저장소가 보관 정책을 적용하기 전까지 보관하는 완료 요청 수. 내 `CoverageStore` 빈을 넣으면 무시됩니다 |
| `reqover.mvc.snapshot-eviction` | `oldest-first` | 상한에 닿았을 때의 동작: `oldest-first`는 가장 오래된 스냅샷을 지우고(기본), `reject-when-full`은 기존 창을 그대로 두고 새로 들어오는 것을 버립니다. 내 `CoverageStore` 빈을 넣으면 무시됩니다 |
| `reqover.webflux.enabled` | `true` | WebFlux 어댑터를 붙일지 여부. `false`면 Reactor 컨텍스트 자동 전달을 켜는 것도 건너뜁니다 |
| `reqover.webflux.exclude-path-prefixes` | `/reqover` | 추적에서 제외할 경로. Ant 패턴이 아니라 **앞부분 일치**로 비교합니다. 지정하면 기본 목록을 대체합니다 |
| `reqover.webflux.max-snapshots` | `10000` | `reqover.mvc.max-snapshots`와 같고, 리액티브 애플리케이션용입니다 |
| `reqover.webflux.snapshot-eviction` | `oldest-first` | `reqover.mvc.snapshot-eviction`과 같고, 리액티브 애플리케이션용입니다 |
| `reqover.report.endpoint.enabled` | **`false`** | 내장 HTTP 리포트 엔드포인트를 등록할지 여부. 기본은 꺼짐 — [3단계](#3-리포트를-어떻게-볼지-정하기) 참고 |
| `reqover.report.endpoint.path` | `/reqover/report` | 엔드포인트의 기준 경로. 이 경로로 JSON이, 같은 경로에 `.html`을 붙인 경로로 HTML 리포트가 나갑니다 |
| `reqover.report.export.json-path` | *지정 안 함* | 애플리케이션 컨텍스트가 닫힐 때 JSON 리포트를 쓸 경로. 비워두면 JSON을 내보내지 않습니다 |
| `reqover.report.export.html-path` | *지정 안 함* | HTML 리포트에 대해 같은 역할. 비워두면 HTML을 내보내지 않습니다 |

표에 담기지 않는 두 가지:

- 어댑터 둘이 동시에 켜지는 일은 없습니다. 어느 쪽 속성이 적용되는지는 애플리케이션이 서블릿인지 리액티브인지가 정합니다.
- `reqover.report.*`는 `CoverageStore` 빈이 있을 때만, 즉 어댑터가 켜져 있을 때만 효력이 있습니다. 어댑터를 둘 다 끄면 리포트 생성도 함께 꺼집니다.

---

## 3. 리포트를 어떻게 볼지 정하기

리포트를 꺼내는 방법은 두 가지이고, **둘 다 기본이 꺼짐입니다.** 필요한 쪽을 켜면 됩니다. 애플리케이션이 떠 있는 동안 눈으로 보려면 HTTP 엔드포인트, CI 실행 결과를 파일로 남기려면 종료 시 내보내기입니다. 동시에 켜도 되고, 두 경로가 만드는 문서는 같습니다.

### 3-1. HTTP 리포트 엔드포인트

```properties
reqover.report.endpoint.enabled=true
reqover.report.endpoint.path=/reqover/report
```

이러면 리포트가 아래 경로로 나갑니다.

| URL | Content type |
| --- | --- |
| `/reqover/report` | `application/json` |
| `/reqover/report.html` | `text/html` |

`.html`은 지정한 `path` 뒤에 붙는 것이고, 따로 설정할 수 있는 값이 아닙니다.

엔드포인트는 MVC와 WebFlux 양쪽 모두에 등록되며, 스타터가 웹 애플리케이션 타입에 맞는 구현을 골라 줍니다. 두 데모 모두 이 방식으로 켭니다 — [`examples/mvc-sample/src/main/resources/application.properties`](../examples/mvc-sample/src/main/resources/application.properties)를 보세요.

> [!CAUTION]
> **기본값이 `false`인 것은 의도한 것이고, 켜는 것은 배포에 대한 결정입니다.** 리포트에는 내부 클래스·메서드 이름이 그대로 담기고, **Reqover에는 자체 인증이 없습니다** — 이 엔드포인트에도, 다른 어디에도 없습니다. 켠다면 경로를 지키는 것은 애플리케이션의 몫입니다. 애플리케이션의 인증 설정을 앞에 두고, 개발·QA 네트워크 안에서만 여세요. 샘플 애플리케이션에는 인증이 없고, 그래서 데모 스크립트가 `--server.address=127.0.0.1`로 띄웁니다.

#### 경로는 제외 접두사 안에 두세요

두 어댑터 모두 기본적으로 **`/reqover`를 기록 대상에서 제외**합니다. MVC는 `/reqover`, `/reqover/**`, `/error`를 제외하고, WebFlux는 `/reqover`로 시작하는 경로를 건너뜁니다.

제외하는 이유는 간단합니다. 리포트를 열어보는 행위 자체가 요청이라서, 제외하지 않으면 **리포트를 볼 때마다 리포트에 리포트 조회 기록이 쌓입니다.**

`reqover.report.endpoint.path`로 경로를 옮긴다면 그에 맞는 제외를 직접 추가하세요. 이 속성을 지정하면 기본 목록을 대체하므로, 계속 쓰고 싶은 기본값도 함께 적어야 합니다.

```properties
reqover.report.endpoint.path=/internal/reqover/report
reqover.mvc.exclude-path-patterns=/internal/reqover,/internal/reqover/**,/error
# WebFlux라면
# reqover.webflux.exclude-path-prefixes=/internal/reqover
```

#### 직접 컨트롤러를 만드는 방식도 그대로 됩니다

`0.1.1`의 안내 — 리포트를 직접 노출하기 — 는 지금도 지원되고, 스타터를 안 쓴다면 이 방법뿐입니다. 스타터가 만드는 빈은 전부 `@ConditionalOnMissingBean`이 걸려 있어서, 내가 정의하면 Reqover 쪽이 물러납니다.

| 내가 정의한 빈 | 물러나는 것 |
| --- | --- |
| `ReqoverReportService` | 스타터의 리포트 서비스 |
| `ReqoverMvcReportEndpoint` | 내장 서블릿 엔드포인트 |
| `reqoverReportRoutes`라는 이름의 `RouterFunction<ServerResponse>` | 내장 리액티브 라우트 |
| `ReqoverReportExporter` | 종료 시 내보내기 |

스타터를 쓴다면 `ReqoverReportService`를 주입받는 게 코드가 가장 적습니다.

```java
package com.example.app.internal;

import io.reqover.spring.boot.ReqoverReportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalReqoverReportController {
    private final ReqoverReportService reportService;

    public InternalReqoverReportController(ReqoverReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reqover/report")
    ResponseEntity<String> report() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(reportService.json());
    }

    @GetMapping("/reqover/report.html")
    ResponseEntity<String> htmlReport() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(reportService.html());
    }
}
```

스타터를 안 쓴다면 `CoverageStore`를 주입받아서 `reqover-report`의 `CoverageReportGenerator`, `HtmlCoverageReportRenderer`로 리포트를 직접 만들면 됩니다.

> [!IMPORTANT]
> **`InMemoryCoverageStore`가 아니라 `CoverageStore`를 주입받으세요.** `0.2.0`에서 바뀐 부분입니다 — [저장소 교체하기](#저장소-교체하기) 참고.

동작하는 전체 예시는 [`examples/mvc-sample`](../examples/mvc-sample)과 [`examples/webflux-sample`](../examples/webflux-sample)에 있습니다.

### 3-2. 종료 시 리포트 파일로 내보내기

```properties
reqover.report.export.json-path=build/reqover-report.json
reqover.report.export.html-path=build/reqover-report.html
```

둘 중 하나만 지정해도 되고, 둘 다 비워두면 내보내기 자체가 꺼집니다. 상위 디렉터리가 없으면 만들어 줍니다. 성공하면 파일마다 한 줄씩 표준 출력에 찍힙니다.

```text
[reqover] wrote the JSON report to /abs/path/build/reqover-report.json
```

**CI 작업이 통합 테스트 실행 결과를 리포트 파일로 받아 가는 방법이 바로 이것입니다.** agent를 붙여서 애플리케이션을 띄우고, 테스트를 흘려보내고, 정상 종료시키면 파일이 남습니다.

```bash
java -javaagent:reqover-agent-0.2.0.jar=include=com.example \
  -jar build/libs/your-app.jar \
  --reqover.report.export.json-path=build/reqover-report.json
```

알아둘 것 세 가지:

- **내보내기는 애플리케이션 컨텍스트가 닫힐 때 실행됩니다.** `SIGKILL`로 죽인 프로세스는 아무것도 쓰지 않습니다. CI에서는 `SIGTERM`(그냥 `kill`)으로 멈추고 종료될 때까지 기다리세요.
- **내보내기 실패는 표준 에러에 남기고 삼킵니다.** 측정 도구가 종료를 실패시키는 원인이 되어서는 안 되기 때문입니다. 뒤집어 말하면 파일이 없는 것이 조용한 실패라는 뜻이므로, 분석하기 전에 파일이 실제로 있는지 확인하세요.
- **내보낸 문서는 엔드포인트가 내보냈을 것과 바이트 단위로 같습니다.** 둘 다 같은 `ReqoverReportService`를 거칩니다.

리포트를 내보내고, diff가 어떤 엔드포인트에 영향을 주는지 묻고, 그 답을 pull request에 댓글로 다는 전체 흐름은 [CI에서 영향 분석하기](18_ci_impact_analysis.ko.md)에 있습니다.

---

## 4. Java agent 붙여서 실행

여기가 실제로 실행을 기록하는 부분입니다.

### 4-1. agent JAR 받기

[v0.2.0 릴리스](https://github.com/reqover-labs/reqover/releases/tag/v0.2.0)에서 `reqover-agent-0.2.0.jar`를 받습니다. 같은 릴리스의 `reqover-0.2.0-SHA256SUMS.txt`로 파일이 온전한지 확인할 수 있습니다.

```bash
shasum -a 256 reqover-agent-0.2.0.jar          # macOS
sha256sum reqover-agent-0.2.0.jar              # Linux
```

```powershell
Get-FileHash reqover-agent-0.2.0.jar -Algorithm SHA256   # Windows
```

### 4-2. 실행

**내 애플리케이션 패키지를 `include=`로 반드시 지정해야 합니다.**

```bash
java \
  -javaagent:reqover-agent-0.2.0.jar=include=com.example.orders \
  -jar app.jar
```

### `include` / `exclude` 문법

```text
include=com.example.orders;com.example.payments,exclude=com.example.orders.generated
```

| 규칙 | 내용 |
| --- | --- |
| 옵션 사이 구분자 | 쉼표 `,` |
| 패키지 여러 개 구분자 | 세미콜론 `;` |
| `include` | **필수.** 없으면 아무것도 계측하지 않습니다 |
| 매칭 방식 | 점 표기 클래스 이름의 **앞부분 일치** |
| 우선순위 | 가장 긴 접두사가 이깁니다. **길이가 같으면 `exclude` 승리** |

**기본으로 제외되는 것** — 프레임워크까지 계측해서 리포트가 잠기는 걸 막기 위한 값입니다.

```text
org.springframework.   reactor.   io.micrometer.
```

**항상 제외되고 `include`로도 켤 수 없는 것** — 계측하면 JVM이나 Reqover 자신이 깨집니다.

```text
java.  javax.  jakarta.  jdk.  sun.  com.sun.  org.objectweb.asm.
io.reqover.core.  io.reqover.agent.  io.reqover.instrumentation.
io.reqover.report.  io.reqover.spring.
```

우선순위 규칙이 실제로 어떻게 동작하는지:

```text
include=com.example, exclude=com.example.generated
  → com.example.orders.OrderService   계측됨    (include만 일치)
  → com.example.generated.Dto         계측 안 됨 (exclude가 더 길다)

include=org.springframework.samples
  → org.springframework.samples.Foo   계측됨    (기본 제외보다 더 길어서 이긴다)
  → org.springframework.boot.Bar      계측 안 됨 (기본 제외에 걸린다)
```

> [!TIP]
> 접두사는 **앞부분 문자열 일치**입니다. `include=com.example.order`라고 쓰면 `com.example.orders`도 함께 걸립니다. 의도한 패키지만 잡으려면 `include=com.example.order.`처럼 **끝에 점을 붙이세요.**

`include`는 좁게 쓰는 편이 좋습니다. 넓게 잡으면 리포트가 읽기 힘들어지고 시작 시간도 늘어납니다.

---

## 5. 잘 됐는지 확인하기

1. **개발·QA 네트워크에서만** 애플리케이션을 시작합니다.
2. **시작 로그의 표준 에러(stderr)를 봅니다.** `[reqover]`로 시작하는 줄이 있으면 agent 설정에 문제가 있다는 뜻입니다 ([문제 해결](#문제-해결) 참고). 아무 경고도 없으면 정상입니다.
3. 내가 아는 API를 **하나** 호출합니다.
4. 엔드포인트를 켰다면 `/reqover/report`를 조회하고, 아니면 애플리케이션을 멈춘 뒤 내보낸 파일을 엽니다.
5. 아래를 확인합니다.
   - 방금 호출한 API 경로가 정규화된 형태(`GET /orders/{id}`)로 있는지
   - 그 아래에 내 컨트롤러·서비스 클래스가 있는지
   - 메서드 이름이 읽을 수 있는 형태(`find(long): OrderResponse`)로 나오는지

WebFlux라면 하나 더 — 한 API의 기록 안에 **서로 다른 스레드 이름이 2개 이상** 있는지 봅니다. 그게 스레드를 넘어서 추적이 유지됐다는 증거입니다. (스레드 전환이 실제로 일어나지 않는 단순한 API라면 1개만 나올 수도 있습니다.)

---

## 문제 해결

가장 흔한 실패는 **"리포트가 비어 있다"** 이고, 원인은 대부분 `include`입니다.

| 증상 | 원인 | 조치 |
| --- | --- | --- |
| 리포트에 엔드포인트가 아예 없음 | agent가 안 붙었거나 `include`가 없음 | stderr의 `[reqover]` 경고 확인. `-javaagent:` 경로가 실제 파일을 가리키는지 확인 |
| `[reqover] no include configured` | `include=`를 안 줬음 | `include=내.패키지` 추가. 이 상태에서는 의도적으로 아무것도 계측하지 않습니다 |
| `[reqover] no valid include configured` | `include`를 줬지만 값이 비어 있음 | `include=` 뒤에 값이 있는지, 쉼표/세미콜론을 헷갈리지 않았는지 확인 |
| `[reqover] ignoring malformed agent option` | `key=value` 형태가 아님 | `include=com.example` 처럼 `=`를 넣었는지 확인 |
| `[reqover] ignoring unknown agent option` | `include`/`exclude` 외의 키를 씀 | 오타 확인 (`includes`, `packages` 등은 인식하지 않습니다) |
| 엔드포인트는 나오는데 클래스 목록이 비어 있음 | `include`가 내 클래스와 안 맞음 | 클래스가 아니라 **패키지 접두사**를 주는지, 기본 제외(`org.springframework.` 등)에 걸리지 않는지 확인 |
| `/reqover/report`가 404 | **엔드포인트는 기본이 꺼짐** | `reqover.report.endpoint.enabled=true`를 넣거나 직접 컨트롤러를 만드세요. 아무 설정도 안 한 상태에서는 이게 정상 동작입니다 |
| 엔드포인트를 켰는데도 404 | 어댑터가 안 켜져서 `CoverageStore`도 리포트 서비스도 없음 | 웹 애플리케이션이 맞는지, `reqover.mvc.enabled=false` / `reqover.webflux.enabled=false`로 꺼두지 않았는지 확인 |
| 리포트 조회만 기록에 쌓임 | 엔드포인트 경로가 제외 접두사 밖에 있음 | 경로를 `/reqover` 아래로 두거나 `reqover.mvc.exclude-path-patterns` / `reqover.webflux.exclude-path-prefixes`에 추가 |
| `exclude-path-patterns`를 지정했더니 `/error`가 기록됨 | 이 속성은 기본 목록을 **대체**함 | 계속 쓰고 싶은 기본값을 내 값 안에 함께 적으세요 |
| 종료했는데 파일이 없음 | `SIGKILL`로 죽였거나 쓰기가 실패함 | `SIGTERM`으로 멈추고 종료를 기다리세요. stdout의 `[reqover] wrote the`와 stderr의 `[reqover] could not write` 확인 |
| `0.1.1`에서 올린 뒤 `InMemoryCoverageStore` 주입 실패 | 어댑터가 이제 `CoverageStore` 빈을 만듦 | 주입 지점을 `CoverageStore`로 바꾸세요. [저장소 교체하기](#저장소-교체하기) 참고 |
| `CoverageStore` 빈 주입 실패 | 어댑터 의존성이 없거나 웹 타입이 안 맞음 | 스타터를 쓰거나, MVC 앱에 `reqover-spring-mvc`, WebFlux 앱에 `reqover-spring-webflux`가 들어갔는지 확인 |
| 의존성을 못 찾음 (`Could not find io.reqover:...`) | 1단계를 안 했거나 `mavenLocal()`이 없음 | `publishToMavenLocal` 재실행, `mavenLocal()`이 `mavenCentral()`보다 앞인지 확인 |
| 한참 뒤 오래된 기록이 사라짐 | 보관 개수 상한(기본 10,000)에 도달 | 정상 동작입니다. [보관 개수 조정](#보관-개수-조정) 참고 |

---

## 조정할 수 있는 것

### 보관 개수 조정

기록은 메모리에만 남고 기본 상한이 10,000건입니다. 상한을 넘으면 가장 오래된 스냅샷을 지우거나(`oldest-first`, 기본), 기존 창을 그대로 두고 새로 들어오는 것을 버립니다(`reject-when-full`). 상한과 정책 모두 속성으로 조정합니다 — 빈을 만들 필요가 없습니다.

```properties
reqover.mvc.max-snapshots=50000
reqover.mvc.snapshot-eviction=oldest-first
# 긴 QA 세션에서 처음 N건을 남기고 싶다면:
# reqover.mvc.snapshot-eviction=reject-when-full
# WebFlux라면
# reqover.webflux.max-snapshots=50000
# reqover.webflux.snapshot-eviction=reject-when-full
```

`CoverageStore`를 직접 구현한다면 `reqover-core` 테스트에 있는 추상 JUnit 계약(`CoverageStoreContract`)으로 같은 동작을 보장할 수 있습니다. 상속해서 `newStore()`가 내 저장소를 돌려주게 하고 테스트를 돌리면 됩니다.

### 저장소 교체하기

기록이 어디로 갈지는 `CoverageStore`가 SPI입니다. 이 타입의 빈을 정의하면 어댑터가 물러납니다 — 두 어댑터 모두 자기 저장소 빈에 `@ConditionalOnMissingBean(CoverageStore.class)`를 걸어 두었습니다. 그래서 스냅샷을 디스크나 데이터베이스에 쓰거나, 샘플링 규칙으로 버리는 구현을 넣을 수 있습니다.

```java
@Bean
CoverageStore reqoverCoverageStore() {
    return new SamplingCoverageStore(0.1);   // 직접 만든 구현
}
```

인터페이스는 메서드 세 개입니다.

| 메서드 | 지켜야 할 것 |
| --- | --- |
| `flush(CoverageBucket)` | 끝난 기록함을 받습니다. `bucket.snapshot()`을 즉시 호출하세요 — 호출이 반환된 뒤에도 남은 스레드에서 기록이 더 들어올 수 있습니다 |
| `snapshots()` | 보관 중인 스냅샷을 오래된 것부터. 반환값은 복사본이라 다른 스레드가 flush하는 중에도 순회해도 안전합니다 |
| `clear()` | 보관 중인 스냅샷을 전부 버립니다 |

구현은 동시 호출에 안전해야 하고, `flush`는 작업 단위를 끝낸 스레드(보통 HTTP 워커)에서 호출되므로 오래 블로킹하면 안 됩니다.

내 저장소를 넣으면 **`max-snapshots`는 의미가 없어집니다.** 그 시점부터 보관 정책은 전적으로 내 몫입니다.

> [!IMPORTANT]
> **`0.1.1`에서 바뀐 점(호환성 깨짐).** 어댑터가 만들던 빈이 `InMemoryCoverageStore`에서 `CoverageStore`로 바뀌었습니다. `InMemoryCoverageStore`는 이제 유일한 선택지가 아니라 그 인터페이스의 한 구현일 뿐입니다. 구체 타입으로 주입받던 애플리케이션은 주입 지점을 `CoverageStore`로 바꿔야 합니다.

인메모리 저장소를 그대로 쓰면서 값만 직접 정하고 싶다면 빈 방식도 여전히 됩니다.

```java
@Bean
CoverageStore reqoverCoverageStore() {
    return new InMemoryCoverageStore(50_000);
}
```

### 테스트 사이에 기록 비우기

`clear()`로 비웁니다. 테스트마다 깨끗한 상태에서 시작하고 싶을 때 씁니다.

```java
@AfterEach
void resetCoverage() {
    coverageStore.clear();
}
```

### HTTP 요청이 아닌 작업 기록하기

어댑터는 HTTP 요청을 자동으로 추적합니다. 그 외의 것 — 스케줄 작업, 메시지 리스너, 개별 테스트 — 을 똑같이 다루는 방법이 `UnitScope`입니다. try-with-resources 블록 안에서 실행된 것이 전부 그 작업 단위로 기록되고, 블록이 끝날 때 저장소로 flush됩니다.

```java
import io.reqover.core.CoverageStore;
import io.reqover.core.UnitInfo;
import io.reqover.core.UnitScope;

@Component
class NightlySettlementJob {
    private final CoverageStore coverageStore;
    private final SettlementService settlement;

    NightlySettlementJob(CoverageStore coverageStore, SettlementService settlement) {
        this.coverageStore = coverageStore;
        this.settlement = settlement;
    }

    @Scheduled(cron = "0 0 3 * * *")
    void run() {
        String runId = UUID.randomUUID().toString();
        try (UnitScope scope = UnitScope.open(coverageStore, UnitInfo.scheduledJob(runId, "nightly-settlement"))) {
            settlement.settleYesterday();
        }
    }
}
```

리포트는 작업 단위의 이름으로 묶으므로 `nightly-settlement`가 엔드포인트들 옆에 나란히 나옵니다. `UnitInfo`에는 흔한 경우를 위한 팩토리가 있습니다 — `scheduledJob(runId, jobName)`, `message(messageId, destination)`, `test(runId, testName)` — 그 외에는 `of(unitId, unitType, name)`을 쓰면 됩니다. 이름은 개별 실행이 아니라 그 작업·토픽·테스트를 가리키는 값으로 잡으세요. 리포트가 묶는 기준이 그 이름입니다.

스코프를 닫으면 기록함이 마감되고 정확히 한 번 flush됩니다. 그래서 블록을 중간에 빠져나가거나 예외로 나가도 안전하고, 두 번 닫아도 아무 일도 일어나지 않습니다. 상태 코드를 직접 남기고 싶다면 블록이 끝나기 전에 `scope.bucket().finish(status)`를 호출하세요.

> [!IMPORTANT]
> **추적은 스코프를 연 스레드를 따라갑니다.** 블록 안에서 다른 스레드로 넘긴 작업은 이 작업 단위로 기록되지 *않습니다.* 그 스레드가 자기 작업을 `UnitScope.join(scope.bucket())`으로 감싸서 직접 합류해야 합니다. `join` 스코프를 닫으면 이전 컨텍스트만 복원되고 마감이나 flush는 하지 않습니다 — 그건 기록함을 만든 `open` 스코프의 몫입니다.

---

## 붙이기 전에 알아둘 한계

- **메서드 단위입니다.** 몇 번째 줄까지 실행했는지는 알 수 없습니다. 줄·분기 정밀도가 필요하면 JaCoCo를 함께 쓰세요.
- **기록은 메모리에만 있습니다.** 애플리케이션을 재시작하면 사라지고, 인스턴스가 여러 대면 각자 자기 기록만 갖습니다. 다른 곳에 저장하는 확장점이 `CoverageStore`이지만 Reqover가 제공하는 영속 구현은 없습니다 — 대신 리포트를 파일로 내보내세요.
- **MVC의 비동기 처리 구간은 자동으로 이어지지 않습니다.** 별도 스레드로 넘어간 부분은 기록되지 않고, 요청 처리가 다시 돌아오는 시점부터 이어집니다. `UnitScope` 안에서도 다른 스레드가 `join` 스코프를 열지 않으면 마찬가지입니다.
- **컴파일러가 자동 생성한 메서드는 기록하지 않습니다.**
- **종료 시 내보내기는 정상 종료에서만 동작합니다.** `SIGKILL`은 아무것도 쓰지 않고, 쓰기 실패는 예외로 올리지 않고 로그만 남깁니다.
- **리포트 엔드포인트의 인증은 애플리케이션 책임입니다.** Reqover가 제공하는 인증은 없습니다.
- **리포트는 실제로 관측된 것만 보여줍니다.** 리포트에 없다는 것이 그 관계가 없다는 증거는 아닙니다 — 그 API를 아직 호출하지 않은 것일 수도 있습니다. 같은 이유로 역방향 조회는 "여기부터 보라"는 힌트이고, 완전한 변경 영향 분석이 아닙니다.

성능 영향의 측정 범위와 한계는 [측정된 agent 오버헤드](15_performance_results.ko.md)에 있습니다. 계측 메서드 진입당 약 24 ns이며, 교대 라운드로 측정했고, 다루지 않은 것도 함께 적어 두었습니다.

---

## 막혔다면

[이슈로 남겨주세요](https://github.com/reqover-labs/reqover/issues/new/choose). 아래를 함께 적어주시면 훨씬 빨리 좁혀집니다.

- OS와 `java -version` 결과
- Spring Boot 버전, MVC인지 WebFlux인지
- 실행한 `-javaagent:` 옵션 전체
- 설정한 `reqover.*` 속성
- stderr에 나온 `[reqover]` 줄
- 기대한 결과와 실제로 나온 결과

**어디서 막혔는지가 지금 이 프로젝트에 가장 필요한 정보입니다.** 문서가 부실해서 막힌 것도 버그로 취급합니다.

관련 문서: [시스템 아키텍처](02_architecture.ko.md) · [CI에서 영향 분석하기](18_ci_impact_analysis.ko.md) · [Agent E2E Demo](09_agent_e2e_demo.md) · [데모 스크립트](10_demo_script.md)
