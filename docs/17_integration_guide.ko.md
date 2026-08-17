**한국어** | [English](17_integration_guide.md)

# 17. Reqover를 내 Spring 프로젝트에 붙이기

데모가 아니라 **내 애플리케이션**에서 요청별 실행 기록을 보려면 이 문서를 따라오면 됩니다. 처음이면 20~30분 정도 걸립니다.

아직 데모를 안 돌려보셨다면 [README의 "5분 만에 직접 보기"](../README.ko.md#5분-만에-직접-보기)를 먼저 해보시는 걸 권합니다. 데모가 되는 걸 본 다음에 붙이는 게 문제를 찾기 훨씬 쉽습니다.

> [!IMPORTANT]
> Reqover `0.1.1`은 **Maven Central에 아직 없습니다.** 그래서 1단계에서 라이브러리를 내 컴퓨터의 로컬 Maven 저장소에 직접 설치해야 합니다. 개발·QA·스테이징 환경을 전제로 만들었고, 운영 환경에 상시로 켜두는 용도가 아닙니다.

---

## 전체 흐름

붙이는 작업은 네 단계입니다. **어떤 단계도 내 소스 코드를 수정하지 않습니다** — 3단계에서 리포트를 볼 컨트롤러 하나만 추가합니다.

| 단계 | 하는 일 | 왜 필요한가 |
| --- | --- | --- |
| 1 | 라이브러리를 로컬 Maven에 설치 | Maven Central에 없어서 |
| 2 | 의존성 2개 추가 | 요청과 실행 기록을 연결하는 부분 |
| 3 | 리포트 엔드포인트 만들기 | Reqover가 안 만들어 줍니다 (아래 이유 설명) |
| 4 | Java agent 붙여서 실행 | 실제로 실행을 기록하는 부분 |

### 미리 정해야 할 것

- **JDK 17 또는 21**
- **Spring Boot 3.x** — 샘플은 3.5.16에서 검증했습니다
- **MVC인지 WebFlux인지** — 어댑터는 **둘 중 하나만** 넣습니다

---

## 1. 라이브러리를 로컬 Maven 저장소에 설치

`v0.1.1` 태그를 받아서 설치합니다.

```bash
git clone --branch v0.1.1 --depth 1 https://github.com/reqover-labs/reqover.git
cd reqover
./gradlew clean publishToMavenLocal
```

Windows는 `.\gradlew.bat clean publishToMavenLocal`입니다.

설치되는 것은 `io.reqover` 그룹의 라이브러리 모듈입니다.

| 아티팩트 | 버전 |
| --- | --- |
| `io.reqover:reqover-core` | `0.1.1` |
| `io.reqover:reqover-instrumentation` | `0.1.1` |
| `io.reqover:reqover-report` | `0.1.1` |
| `io.reqover:reqover-spring-mvc` | `0.1.1` |
| `io.reqover:reqover-spring-webflux` | `0.1.1` |

**`reqover-agent`는 여기 없습니다.** agent는 의존성이 섞이지 않게 따로 묶은(shaded) JAR이라 로컬 설치 대상이 아니고, GitHub Release에서 파일로 받습니다 → [4단계](#4-java-agent-붙여서-실행).

설치가 됐는지 확인:

```bash
ls ~/.m2/repository/io/reqover        # macOS / Linux
```

```powershell
Get-ChildItem "$env:USERPROFILE\.m2\repository\io\reqover"   # Windows
```

---

## 2. 의존성 추가

**어댑터 1개 + 리포트 모듈 1개**입니다. `mavenLocal()`을 저장소 목록에 넣어야 하고, 1단계에서 설치한 것을 먼저 찾도록 `mavenCentral()`보다 앞에 둡니다.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Spring MVC 프로젝트라면
    implementation("io.reqover:reqover-spring-mvc:0.1.1")

    // Spring WebFlux 프로젝트라면 (위 대신)
    // implementation("io.reqover:reqover-spring-webflux:0.1.1")

    implementation("io.reqover:reqover-report:0.1.1")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'io.reqover:reqover-spring-mvc:0.1.1'
    implementation 'io.reqover:reqover-report:0.1.1'
}
```

### Maven

`mavenLocal`은 Maven에서 기본으로 쓰이므로 저장소 설정을 따로 추가할 필요가 없습니다.

```xml
<dependencies>
  <!-- Spring MVC 프로젝트라면 -->
  <dependency>
    <groupId>io.reqover</groupId>
    <artifactId>reqover-spring-mvc</artifactId>
    <version>0.1.1</version>
  </dependency>

  <!-- Spring WebFlux 프로젝트라면 artifactId를 reqover-spring-webflux로 -->

  <dependency>
    <groupId>io.reqover</groupId>
    <artifactId>reqover-report</artifactId>
    <version>0.1.1</version>
  </dependency>
</dependencies>
```

### 이것만으로 무슨 일이 일어나나

어댑터의 자동 설정이 `InMemoryCoverageStore` 빈을 만들고, 들어오는 요청마다 기록함을 하나씩 붙입니다. 여기까지는 **기록만 쌓입니다.** 아직 볼 방법이 없고, agent도 안 붙었으니 기록될 내용도 없습니다.

> [!NOTE]
> **Reqover는 리포트 엔드포인트를 만들어 주지 않습니다.** 커버리지 리포트는 내부 구조가 그대로 드러나는 데이터인데, 인증 방식과 네트워크 정책은 환경마다 다릅니다. 그래서 "어디에 열고 어떻게 보호할지"를 라이브러리가 몰래 정하지 않고 애플리케이션의 명시적인 결정으로 남겨두었습니다.

### WebFlux를 쓴다면 알아야 할 것

WebFlux 어댑터는 요청 정보가 스레드를 넘어 따라가게 하려고 **Reactor의 컨텍스트 자동 전달 기능을 JVM 전체에 켭니다.** 이게 필요 없거나 곤란하면, 애플리케이션이 시작되기 전에 아래 속성으로 어댑터 전체를 끌 수 있습니다.

```properties
reqover.webflux.enabled=false
```

---

## 3. 리포트 엔드포인트 만들기

아래 컨트롤러를 추가합니다. `import`까지 그대로 쓸 수 있습니다.

```java
package com.example.app.internal;

import io.reqover.core.InMemoryCoverageStore;
import io.reqover.report.CoverageReport;
import io.reqover.report.CoverageReportGenerator;
import io.reqover.report.HtmlCoverageReportRenderer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalReqoverReportController {
    private final InMemoryCoverageStore coverageStore;
    private final CoverageReportGenerator reportGenerator = new CoverageReportGenerator();
    private final HtmlCoverageReportRenderer htmlRenderer = new HtmlCoverageReportRenderer();

    public InternalReqoverReportController(InMemoryCoverageStore coverageStore) {
        this.coverageStore = coverageStore;
    }

    @GetMapping("/reqover/report")
    CoverageReport report() {
        return reportGenerator.generate(coverageStore.snapshots());
    }

    @GetMapping("/reqover/report.html")
    ResponseEntity<String> htmlReport() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlRenderer.render(reportGenerator.generate(coverageStore.snapshots())));
    }
}
```

`InMemoryCoverageStore`는 2단계의 자동 설정이 만들어 둔 빈이라 그냥 주입받으면 됩니다.

동작하는 전체 예시는 [`examples/mvc-sample`](../examples/mvc-sample)과 [`examples/webflux-sample`](../examples/webflux-sample)에 있습니다.

### 경로는 `/reqover/**`를 유지하세요

두 어댑터가 이 접두사를 **기록 대상에서 제외**하고 있습니다. MVC는 인터셉터에서 `/reqover`, `/reqover/**`, `/error`를 제외하고, WebFlux는 필터에서 같은 경로를 건너뜁니다.

제외하는 이유는 간단합니다. 리포트를 열어보는 행위 자체가 요청이라서, 제외하지 않으면 **리포트를 볼 때마다 리포트에 리포트 조회 기록이 쌓입니다.** 다른 경로를 쓰고 싶다면 엔드포인트를 열기 전에 그 경로에 대한 제외를 직접 추가하세요.

> [!CAUTION]
> **이 엔드포인트를 인터넷에 노출하지 마세요.** 리포트에는 내부 패키지·클래스·메서드 구조가 그대로 담깁니다. 애플리케이션의 인증과 네트워크 접근 제어를 반드시 적용하고, 개발·QA 네트워크 안에서만 여세요. 샘플의 엔드포인트에는 인증이 없습니다.

---

## 4. Java agent 붙여서 실행

여기가 실제로 실행을 기록하는 부분입니다.

### 4-1. agent JAR 받기

[v0.1.1 릴리스](https://github.com/reqover-labs/reqover/releases/tag/v0.1.1)에서 `reqover-agent-0.1.1.jar`를 받습니다. 같은 릴리스의 `reqover-0.1.1-SHA256SUMS.txt`로 파일이 온전한지 확인할 수 있습니다.

```bash
shasum -a 256 reqover-agent-0.1.1.jar          # macOS
sha256sum reqover-agent-0.1.1.jar              # Linux
```

```powershell
Get-FileHash reqover-agent-0.1.1.jar -Algorithm SHA256   # Windows
```

### 4-2. 실행

**내 애플리케이션 패키지를 `include=`로 반드시 지정해야 합니다.**

```bash
java \
  -javaagent:reqover-agent-0.1.1.jar=include=com.example.orders \
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
4. `/reqover/report`를 조회합니다.
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
| 리포트 조회만 기록에 쌓임 | 리포트를 `/reqover/**` 밖에 열었음 | 경로를 `/reqover/**`로 옮기거나 그 경로에 제외를 직접 추가 |
| `/reqover/report`가 404 | 3단계 컨트롤러를 안 만들었음 | Reqover는 엔드포인트를 만들어 주지 않습니다. 3단계 진행 |
| `InMemoryCoverageStore` 빈 주입 실패 | 어댑터 의존성이 없거나 웹 타입이 안 맞음 | MVC 앱에 `reqover-spring-mvc`, WebFlux 앱에 `reqover-spring-webflux`가 들어갔는지 확인 |
| 의존성을 못 찾음 (`Could not find io.reqover:...`) | 1단계를 안 했거나 `mavenLocal()`이 없음 | `publishToMavenLocal` 재실행, `mavenLocal()`이 `mavenCentral()`보다 앞인지 확인 |
| 한참 뒤 오래된 기록이 사라짐 | 보관 개수 상한(기본 10,000)에 도달 | 정상 동작입니다. [보관 개수 조정](#보관-개수-조정) 참고 |

---

## 조정할 수 있는 것

### 보관 개수 조정

기록은 메모리에만 남고 기본 상한이 10,000건입니다. 넘으면 오래된 것부터 지워집니다. 어댑터의 빈은 "애플리케이션이 직접 정의하면 물러나도록" 되어 있으니, 내 빈을 정의하면 그 값이 쓰입니다.

```java
@Bean
InMemoryCoverageStore reqoverCoverageStore() {
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

---

## 붙이기 전에 알아둘 한계

- **메서드 단위입니다.** 몇 번째 줄까지 실행했는지는 알 수 없습니다. 줄·분기 정밀도가 필요하면 JaCoCo를 함께 쓰세요.
- **기록은 메모리에만 있습니다.** 애플리케이션을 재시작하면 사라지고, 인스턴스가 여러 대면 각자 자기 기록만 갖습니다.
- **MVC의 비동기 처리 구간은 자동으로 이어지지 않습니다.** 별도 스레드로 넘어간 부분은 기록되지 않고, 요청 처리가 다시 돌아오는 시점부터 이어집니다.
- **컴파일러가 자동 생성한 메서드는 기록하지 않습니다.**
- **리포트는 실제로 관측된 것만 보여줍니다.** 리포트에 없다는 것이 그 관계가 없다는 증거는 아닙니다 — 그 API를 아직 호출하지 않은 것일 수도 있습니다. 같은 이유로 역방향 조회는 "여기부터 보라"는 힌트이고, 완전한 변경 영향 분석이 아닙니다.

성능 영향의 측정 범위와 한계는 [로컬 성능 결과](15_performance_results.md)에 있습니다. 정식 벤치마크가 아닙니다.

---

## 막혔다면

[이슈로 남겨주세요](https://github.com/reqover-labs/reqover/issues/new/choose). 아래를 함께 적어주시면 훨씬 빨리 좁혀집니다.

- OS와 `java -version` 결과
- Spring Boot 버전, MVC인지 WebFlux인지
- 실행한 `-javaagent:` 옵션 전체
- stderr에 나온 `[reqover]` 줄
- 기대한 결과와 실제로 나온 결과

**어디서 막혔는지가 지금 이 프로젝트에 가장 필요한 정보입니다.** 문서가 부실해서 막힌 것도 버그로 취급합니다.

관련 문서: [시스템 아키텍처](02_architecture.md) · [Agent E2E Demo](09_agent_e2e_demo.md) · [데모 스크립트](10_demo_script.md)
