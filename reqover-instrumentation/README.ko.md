[English](README.md) | **한국어** | [프로젝트](../README.ko.md)

# 바이트코드 계측

Maven Central 라이브러리 `io.github.reqover-labs:reqover-instrumentation:0.4.1`입니다.
대부분의 앱은 이 모듈을 직접 사용하기보다 [agent](../reqover-agent/README.ko.md)를
붙입니다. Java 패키지는 `io.reqover.instrumentation` 그대로입니다.

ASM으로 메서드 진입 probe를 넣고 클래스·메서드 정보를 등록합니다. 합성 메서드는
제외하고 단순 접근자는 기본적으로 건너뜁니다. 옵션으로 접근자를 포함하거나,
포함 대상 호출 지점에서 인터페이스 호출·정적 필드 참조 probe를 넣을 수 있습니다.
참조 관측은 대상 사용을 나타내며 인터페이스·enum 내부 메서드의 실행을 뜻하지 않습니다.

JaCoCo를 확장한 구현이 아닌 자체 메서드 진입 계측입니다. probe 집합에 호출 순서,
반복 호출 횟수, 종료 span, CPU 시간이나 줄/분기 비율은 없습니다. 확인 가능한 첫
소스 줄은 메타데이터이지 모든 줄이 실행됐다는 증거가 아닙니다.

포함/제외·클래스 로딩 정책은 agent가 적용하고, 묶어 배포하는 ASM은 패키지를
옮겨 앱의 ASM과 충돌을 피합니다. 원본 소스는 수정하지 않으며 변환에 실패한
클래스는 계측하지 않은 채 유지합니다.

[변환 코드](src/main/java/io/reqover/instrumentation/ReqoverClassInstrumenter.java) |
[agent 옵션](../reqover-agent/README.ko.md) |
[JaCoCo 연동 판단](../docs/14_jacoco_interop_decision.md)
