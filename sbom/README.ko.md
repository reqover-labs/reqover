[English](README.md) | **한국어**

# 의존성 목록과 보안 검사

`reqover.cdx.json`은 저장소가 실제 선택한 의존성으로 만든 CycloneDX 1.6 목록입니다.
배포된 릴리스의 사본은 해당 GitHub Release에서 받습니다. 빌드·테스트·예제·프로젝트
모듈도 포함하므로 agent JAR 안에 묶여 있는 의존성만 나열한 것은 아닙니다.

저장소 루트에서 갱신하고 대조합니다.

```bash
./gradlew clean build cyclonedxBom --no-daemon --console=plain
cp build/reports/bom/reqover.cdx.json sbom/reqover.cdx.json
python3 scripts/verify-sbom-lock.py build/reports/bom/reqover.cdx.json sbom/reqover.cdx.json
python3 scripts/check-sbom-osv.py sbom/reqover.cdx.json
```

Windows는 `gradlew.bat`과 PowerShell의 `Copy-Item`을 사용합니다. 실행 중인 JVM이
JAR을 잡고 있으면 `clean`이 실패할 수 있으므로 다른 작업의 데모를 강제로 종료하지 않습니다.

Reqover 코드는 Apache-2.0입니다. 외부 라이선스는 실제 Maven 메타데이터와
[THIRD_PARTY_NOTICES](../THIRD_PARTY_NOTICES.md)를 함께 확인합니다. 일부 라이브러리의
복수 라이선스는 모두 적용하는 AND가 아니라 선택 가능한 OR 조건입니다. ASM은
agent에 묶이므로 별도의 라이선스 문구도 보관합니다.

검증기는 구성요소·버전·라이선스·관계를 비교하고 생성 시각·일련번호 같은 가변 출처만
정규화합니다. OSV의 `findings`와 기한 있는 `exceptions`를 구분합니다. 예외 정책으로
검사가 통과해도 라이브러리 자체가 패치됐거나 모든 위험이 없다는 뜻은 아닙니다.

[보안 정책](../SECURITY.md) | [보완 이력](../docs/29_osv_dependency_remediation.ko.md)
