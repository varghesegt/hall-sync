package com.exam.parser.config;

import com.exam.parser.PdfParserPipeline;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the PdfParserPipeline as a Spring-managed bean.
 * The pipeline itself is stateless — all state flows through method parameters.
 */
@Configuration
public class ParserBeanConfig {

    @Bean
    public PdfParserPipeline pdfParserPipeline(ParserConfig config, MeterRegistry meterRegistry) {
        return new PdfParserPipeline(config, meterRegistry);
    }
}
