package com.likelion.cms.domain.certificate.service;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    private static final String CLUB_NAME = "멋쟁이사자처럼 수원대학교";
    private static final String REPRESENTATIVE_NAME = "최재령";

    // ==========================================================================
    // 워터마크 정렬 기준: certificate.html 의 info-table 텍스트 블록
    // 좌표계 = 페이지 좌상단(0,0) 기준, CSS px 단위
    //
    //  [가로]
    //   왼쪽  : body padding-left(60px) + table margin-left(160px) = 220px
    //           -> HTML CSS 를 바꾸면 이 값도 같이 바꿀 것
    //   오른쪽: 값 텍스트 중 가장 긴 "소속동아리"(CLUB_NAME) 행의 끝 = 571px (렌더링 PDF 실측)
    //           ※ table 의 width:480px 는 실제 글자보다 넓어서(오른쪽 약 130px 공백)
    //             표 박스 중심을 쓰면 워터마크가 오른쪽으로 밀린다. 그래서 글자 기준으로 잡음.
    //
    //  [세로]
    //   위    : 첫 행(학번) 글자 윗선   = 302px (실측)
    //   아래  : 마지막 행(기수/역할) 글자 아랫선 = 636px (실측)
    // ==========================================================================
    private static final float INFO_LEFT_PX = 220f;
    private static final float INFO_RIGHT_PX = 571f;
    private static final float INFO_TOP_PX = 302f;
    private static final float INFO_BOTTOM_PX = 636f;

    // px -> pt 변환 (openhtmltopdf 기본 96dpi 기준: 96px = 72pt)
    private static final float PX_TO_PT = 0.75f;

    private static final float WATERMARK_SIZE_PT = 280f;
    private static final float WATERMARK_ALPHA = 0.2f;

    private final TemplateEngine templateEngine;

    public PdfGenerationService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateCertificatePdf(String name, String studentId, String department,
                                         String cohortName, String activityType, String part,
                                         LocalDateTime issuedAt) {
        String formattedDate = issuedAt.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"));

        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("studentId", studentId);
        context.setVariable("department", department);
        context.setVariable("clubName", CLUB_NAME);
        context.setVariable("cohortName", cohortName);
        context.setVariable("activityType", activityType);
        context.setVariable("part", part);
        context.setVariable("issuedDate", formattedDate);
        context.setVariable("representativeName", REPRESENTATIVE_NAME);

        String html = templateEngine.process("certificate", context);

        try {
            ByteArrayOutputStream contentOut = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();

            FSSupplier<InputStream> regularFontSupplier = () ->
                    getClass().getClassLoader().getResourceAsStream("fonts/NotoSansKR-Regular.ttf");
            FSSupplier<InputStream> boldFontSupplier = () ->
                    getClass().getClassLoader().getResourceAsStream("fonts/NotoSansKR-Bold.ttf");

            builder.useFont(regularFontSupplier, "NotoSansKR", 400, FontStyle.NORMAL, false);
            builder.useFont(boldFontSupplier, "NotoSansKR", 700, FontStyle.NORMAL, false);

            builder.withHtmlContent(html, null);
            builder.toStream(contentOut);
            builder.run();

            try (PDDocument document = PDDocument.load(contentOut.toByteArray())) {

                InputStream watermarkStream = getClass().getClassLoader()
                        .getResourceAsStream("images/watermark.png");
                PDImageXObject watermarkImage = PDImageXObject.createFromByteArray(
                        document, watermarkStream.readAllBytes(), "watermark"
                );

                // 정보 표 텍스트 블록의 시각적 중심 (CSS px)
                float infoCenterXPx = (INFO_LEFT_PX + INFO_RIGHT_PX) / 2f;   // ≈ 395.5px
                float infoCenterYPx = (INFO_TOP_PX + INFO_BOTTOM_PX) / 2f;   // ≈ 469.0px

                PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                graphicsState.setNonStrokingAlphaConstant(WATERMARK_ALPHA);

                for (PDPage page : document.getPages()) {
                    PDRectangle pageSize = page.getMediaBox();

                    // CSS px(좌상단 기준) -> PDF pt(좌하단 기준) 변환
                    float centerXPt = infoCenterXPx * PX_TO_PT;
                    float centerYPt = pageSize.getHeight() - (infoCenterYPx * PX_TO_PT);

                    // drawImage 는 좌하단 좌표를 받으므로 중심에서 절반만큼 빼준다
                    float x = centerXPt - WATERMARK_SIZE_PT / 2f;
                    float y = centerYPt - WATERMARK_SIZE_PT / 2f;

                    try (PDPageContentStream contentStream = new PDPageContentStream(
                            document, page,
                            PDPageContentStream.AppendMode.APPEND, true, true)) {

                        contentStream.setGraphicsStateParameters(graphicsState);
                        contentStream.drawImage(watermarkImage, x, y,
                                WATERMARK_SIZE_PT, WATERMARK_SIZE_PT);
                    }
                }

                ByteArrayOutputStream finalOut = new ByteArrayOutputStream();
                document.save(finalOut);
                return finalOut.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }
}