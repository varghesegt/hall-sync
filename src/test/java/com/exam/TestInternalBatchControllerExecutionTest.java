package com.exam;

import com.exam.config.tenant.TenantContext;
import com.exam.controller.internal.InternalBatchController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://ep-small-bread-b4kpy8u6-pooler.c-6.us-east-2.aws.neon.tech/tenant_krce?sslmode=require",
    "spring.datasource.username=neondb_owner",
    "spring.datasource.password=npg_SsVa9Wo7mlzw",
    "spring.flyway.enabled=false"
})
public class TestInternalBatchControllerExecutionTest {

    @Autowired
    private InternalBatchController internalBatchController;

    @Test
    public void testDownloadPdfDirectExecution() {
        TenantContext.setCurrentTenant("krce");
        UUID batchId = UUID.fromString("73d7076a-ee7d-4f7f-b867-ce86d444b386");

        System.out.println("Calling internalBatchController.downloadPdf...");
        ResponseEntity<?> response = internalBatchController.downloadPdf(batchId);
        System.out.println("Response Status: " + response.getStatusCode());
        System.out.println("Response Headers: " + response.getHeaders());

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof byte[]);
        byte[] pdfBytes = (byte[]) response.getBody();
        System.out.println("Generated PDF bytes: " + pdfBytes.length);
        assertTrue(pdfBytes.length > 1000);
        // Verify PDF Magic Bytes %PDF
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    public void testDownloadExcelDirectExecution() {
        TenantContext.setCurrentTenant("krce");
        UUID batchId = UUID.fromString("73d7076a-ee7d-4f7f-b867-ce86d444b386");

        System.out.println("Calling internalBatchController.downloadExcel...");
        ResponseEntity<?> response = internalBatchController.downloadExcel(batchId);
        System.out.println("Response Status: " + response.getStatusCode());
        System.out.println("Response Headers: " + response.getHeaders());

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof byte[]);
        byte[] excelBytes = (byte[]) response.getBody();
        System.out.println("Generated Excel bytes: " + excelBytes.length);
        assertTrue(excelBytes.length > 1000);
        // Verify ZIP / XLSX Magic Bytes PK
        assertEquals('P', (char) excelBytes[0]);
        assertEquals('K', (char) excelBytes[1]);
    }

    @Test
    public void testDownloadSummaryExcelDirectExecution() {
        TenantContext.setCurrentTenant("krce");
        UUID batchId = UUID.fromString("73d7076a-ee7d-4f7f-b867-ce86d444b386");

        System.out.println("Calling internalBatchController.downloadSummaryExcel...");
        ResponseEntity<?> response = internalBatchController.downloadSummaryExcel(batchId);
        System.out.println("Response Status: " + response.getStatusCode());
        System.out.println("Response Headers: " + response.getHeaders());

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof byte[]);
        byte[] summaryBytes = (byte[]) response.getBody();
        System.out.println("Generated Summary Excel bytes: " + summaryBytes.length);
        assertTrue(summaryBytes.length > 500);
        // Verify ZIP / XLSX Magic Bytes PK
        assertEquals('P', (char) summaryBytes[0]);
        assertEquals('K', (char) summaryBytes[1]);
    }
}
