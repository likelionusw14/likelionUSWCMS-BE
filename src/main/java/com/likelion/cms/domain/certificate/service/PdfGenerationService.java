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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    private static final String CLUB_NAME = "멋쟁이사자처럼 수원대학교";
    private static final String REPRESENTATIVE_NAME = "최재령";

    private final TemplateEngine templateEngine;

    public PdfGenerationService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateCertificatePdf(String name, String studentId, String department,
                                         String cohortName, String activityType, String part,
                                         LocalDate activityStartDate, LocalDate activityEndDate,
                                         LocalDateTime issuedAt) {
        String formattedDate = issuedAt.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"));

        DateTimeFormatter periodFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        String activityPeriod = activityStartDate.format(periodFormatter)
                + " – " + activityEndDate.format(periodFormatter);   // 3. ~ → –

        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("studentId", studentId);
        context.setVariable("department", department);
        context.setVariable("clubName", CLUB_NAME);
        context.setVariable("cohortName", cohortName);
        context.setVariable("activityType", activityType);
        context.setVariable("part", part);
        context.setVariable("activityPeriod", activityPeriod);
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

                float watermarkSize = 200f;

                PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                graphicsState.setNonStrokingAlphaConstant(0.55f);  // 5. 0.4f → 0.55f, 조금 더 진하게

                for (PDPage page : document.getPages()) {
                    PDRectangle pageSize = page.getMediaBox();

                    float x = (pageSize.getWidth() - watermarkSize) / 2f;
                    float y = (pageSize.getHeight() - watermarkSize) / 2f;

                    try (PDPageContentStream contentStream = new PDPageContentStream(
                            document, page,
                            PDPageContentStream.AppendMode.APPEND, true, true)) {

                        contentStream.setGraphicsStateParameters(graphicsState);
                        contentStream.drawImage(watermarkImage, x, y, watermarkSize, watermarkSize);
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