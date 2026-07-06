package com.hnu.legal_cases.service;

public interface CaseSummaryAsyncService {
    void generateSummaryAsync(String caseId, Long userId, String language);
}
