package com.hnu.legal_cases.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dedicated AI runner for long-form case summaries.
 *
 * <p>The default {@link AiCallRunner} is intentionally conservative for interactive tasks such as
 * keyword extraction and short QA. Case summaries produce much longer output and should use a
 * separate, larger timeout instead of sharing the same 15-second budget.</p>
 */
@Component
public class SummaryAiCallRunner extends AiCallRunner {

    public SummaryAiCallRunner(
            @Value("${app.ai.summary-call-timeout-ms:120000}") long timeoutMs) {
        super(timeoutMs);
    }
}
