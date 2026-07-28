from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs" / "competition" / "submission"
DOCX_PATH = OUT_DIR / "Reqover_result_report_draft.docx"
SCREENSHOT = ROOT / "docs" / "assets" / "reqover-webflux-report.png"


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), fill)
    tc_pr.append(shd)


def set_cell_text(cell, text, bold=False):
    cell.text = ""
    paragraph = cell.paragraphs[0]
    run = paragraph.add_run(text)
    run.bold = bold
    run.font.name = "Calibri"
    run.font.size = Pt(10)
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER


def add_heading(doc, text, level):
    paragraph = doc.add_heading(text, level=level)
    paragraph.paragraph_format.keep_with_next = True
    return paragraph


def add_bullets(doc, items):
    for item in items:
        paragraph = doc.add_paragraph(style="List Bullet")
        paragraph.add_run(item)


def add_numbered(doc, items):
    for item in items:
        paragraph = doc.add_paragraph(style="List Number")
        paragraph.add_run(item)


def add_label_table(doc, rows):
    table = doc.add_table(rows=1, cols=2)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    table.autofit = False
    table.columns[0].width = Inches(1.65)
    table.columns[1].width = Inches(4.85)
    header = table.rows[0].cells
    set_cell_text(header[0], "항목", bold=True)
    set_cell_text(header[1], "내용", bold=True)
    set_cell_shading(header[0], "F2F4F7")
    set_cell_shading(header[1], "F2F4F7")
    for label, value in rows:
        cells = table.add_row().cells
        set_cell_text(cells[0], label, bold=True)
        set_cell_text(cells[1], value)
    doc.add_paragraph()


def configure_styles(doc):
    section = doc.sections[0]
    section.top_margin = Inches(1)
    section.bottom_margin = Inches(1)
    section.left_margin = Inches(1)
    section.right_margin = Inches(1)
    section.header_distance = Inches(0.492)
    section.footer_distance = Inches(0.492)

    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "Calibri"
    normal.font.size = Pt(11)
    normal.paragraph_format.space_after = Pt(6)
    normal.paragraph_format.line_spacing = 1.10

    for name, size, color, before, after in [
        ("Heading 1", 16, "2E74B5", 16, 8),
        ("Heading 2", 13, "2E74B5", 12, 6),
        ("Heading 3", 12, "1F4D78", 8, 4),
    ]:
        style = styles[name]
        style.font.name = "Calibri"
        style.font.size = Pt(size)
        style.font.color.rgb = RGBColor.from_string(color)
        style.paragraph_format.space_before = Pt(before)
        style.paragraph_format.space_after = Pt(after)
        style.paragraph_format.keep_with_next = True


def build_doc():
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    doc = Document()
    configure_styles(doc)

    title = doc.add_paragraph()
    title.alignment = WD_ALIGN_PARAGRAPH.LEFT
    title.paragraph_format.space_after = Pt(3)
    run = title.add_run("Reqover 결과보고서 초안")
    run.font.name = "Calibri"
    run.font.size = Pt(24)
    run.font.bold = True
    run.font.color.rgb = RGBColor(24, 33, 47)

    subtitle = doc.add_paragraph()
    subtitle.add_run("요청 단위 코드 커버리지 귀속 도구").italic = True

    add_label_table(
        doc,
        [
            ("프로젝트명", "Reqover"),
            ("저장소", "https://github.com/reqover-labs/reqover"),
            ("팀명", "<팀명 입력>"),
            ("접수번호", "<접수번호 입력>"),
            ("시연영상", "<YouTube URL 입력>"),
        ],
    )

    add_heading(doc, "1. 프로젝트 개요", 1)
    doc.add_paragraph(
        "Reqover는 Java/Spring 애플리케이션에서 발생한 coverage hit을 개별 HTTP 요청과 endpoint 단위로 귀속하는 개발자 도구이다. "
        "기존 coverage 도구가 코드 실행 여부를 전역 기준으로 보여준다면, Reqover는 어떤 API 요청이 어떤 코드 경로를 실행했는지를 보여준다."
    )
    doc.add_paragraph(
        "MVP는 Spring MVC와 Spring WebFlux를 대상으로 하며, Java agent 기반 method-entry instrumentation, 요청별 coverage bucket, "
        "JSON/HTML 리포트, code-to-endpoint 역조회 기능을 포함한다."
    )

    add_heading(doc, "2. 개발 배경과 문제 정의", 1)
    doc.add_paragraph(
        "백엔드 서비스는 여러 endpoint가 service, validator, mapper, utility 코드를 공유한다. 특정 메서드를 수정했을 때 어떤 API를 우선 재테스트해야 하는지 판단하기 어렵다."
    )
    doc.add_paragraph(
        "Reqover는 이 문제를 정적 추측이 아니라 실제 실행 관측 데이터로 좁히는 것을 목표로 한다."
    )

    add_heading(doc, "3. 주요 기능", 1)
    add_bullets(
        doc,
        [
            "HTTP 요청마다 coverage bucket을 생성하고 probe hit을 해당 bucket에 기록",
            "Spring MVC interceptor로 endpoint pattern과 request lifecycle 관리",
            "Spring WebFlux Reactor Context와 Micrometer Context Propagation을 이용한 thread-hop 귀속 유지",
            "ASM 기반 Java agent로 애플리케이션 클래스 method entry에 probe 삽입",
            "endpoint-to-code 리포트와 code-to-endpoint 영향 범위 역조회 제공",
            "CycloneDX 1.6 JSON SBOM 생성 task 제공",
        ],
    )

    add_heading(doc, "4. 아키텍처", 1)
    add_numbered(
        doc,
        [
            "HTTP 요청이 Spring MVC interceptor 또는 WebFlux filter에 진입한다.",
            "Reqover가 request id와 endpoint 정보를 가진 coverage bucket을 생성한다.",
            "Java agent가 삽입한 ReqoverProbe.hit(classId, probeId)가 메서드 진입 시 호출된다.",
            "ReqoverProbe는 현재 요청 context를 찾아 해당 bucket에 hit을 기록한다.",
            "요청 종료 시 bucket snapshot이 in-memory store에 저장된다.",
            "report module이 endpoint 기준으로 coverage와 reverse index를 집계한다.",
        ],
    )

    add_heading(doc, "5. 검증 결과", 1)
    add_bullets(
        doc,
        [
            "./gradlew test 통과",
            "./gradlew build --quiet 통과",
            "./gradlew cyclonedxBom 통과",
            "MVC agent E2E: 수동 probe 없는 endpoint가 자동 계측되어 report에 표시됨",
            "WebFlux agent E2E: boundedElastic, parallel, reactor-http-nio thread hop 이후에도 같은 endpoint bucket에 귀속됨",
        ],
    )
    doc.add_paragraph("로컬 WebFlux 순차 요청 성능 참고값:")
    perf = doc.add_table(rows=1, cols=5)
    perf.alignment = WD_TABLE_ALIGNMENT.CENTER
    perf.style = "Table Grid"
    headers = ["모드", "Average ms", "p50 ms", "p95 ms", "p99 ms"]
    for idx, text in enumerate(headers):
        set_cell_text(perf.rows[0].cells[idx], text, bold=True)
        set_cell_shading(perf.rows[0].cells[idx], "F2F4F7")
    for row in [
        ["Baseline, no agent", "18.58", "17.88", "26.77", "31.44"],
        ["Reqover agent enabled", "21.57", "18.15", "38.19", "61.65"],
    ]:
        cells = perf.add_row().cells
        for idx, text in enumerate(row):
            set_cell_text(cells[idx], text, bold=(idx == 0))
    doc.add_paragraph("위 수치는 로컬 데모 sanity check이며 production benchmark가 아니다.")

    if SCREENSHOT.exists():
        add_heading(doc, "6. 리포트 화면", 1)
        doc.add_paragraph("WebFlux 자동 계측 데모 리포트 예시:")
        doc.add_picture(str(SCREENSHOT), width=Inches(6.3))

    add_heading(doc, "7. 오픈소스 준비 상태", 1)
    add_bullets(
        doc,
        [
            "Apache License 2.0 적용",
            "THIRD_PARTY_NOTICES.md 작성",
            "CONTRIBUTING.md, SECURITY.md, CODE_OF_CONDUCT.md 작성",
            "GitHub issue template 3종 작성",
            "GitHub Actions build workflow 구성",
            "SBOM 생성 파일: build/reports/bom/reqover-sbom.json",
        ],
    )

    add_heading(doc, "8. 한계와 향후 계획", 1)
    add_bullets(
        doc,
        [
            "현재 coverage 정밀도는 method-entry 수준이며 JaCoCo 수준의 branch coverage는 제공하지 않음",
            "저장소는 MVP 단계에서 in-memory 방식",
            "report endpoint 인증, 보존 정책, 운영 배포 기능은 후속 과제",
            "향후 JaCoCo XML/report 연동, Gradle/Maven plugin, persistent storage, richer report UI를 검토",
        ],
    )

    add_heading(doc, "9. AI 모델 활용 정보", 1)
    doc.add_paragraph(
        "Reqover 제출 소프트웨어에는 AI 모델이 탑재되어 있지 않다. 개발 과정에서 AI 도구를 보조적으로 활용한 경우에도, "
        "런타임 산출물에는 모델 가중치나 외부 inference API 호출이 포함되지 않는다."
    )

    footer = doc.sections[0].footer.paragraphs[0]
    footer.text = "Reqover result report draft"
    footer.alignment = WD_ALIGN_PARAGRAPH.RIGHT

    doc.save(DOCX_PATH)
    print(DOCX_PATH)


if __name__ == "__main__":
    build_doc()
