package com.hnu.legal_cases.service.impl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaseOriginalTranslateServiceImplTest {

    @Test
    void segmentTextKeepsBlankLineParagraphsSeparate() {
        String text = "First paragraph sentence one. Second sentence of the first paragraph. "
                + "The court considered the parties' submissions and the applicable law carefully. "
                + "This third sentence adds enough length to remain a standalone translation segment. "
                + "A final sentence of the first paragraph closes the factual background.\n\n"
                + "Second paragraph starts here. The panel reviewed the record de novo and concluded "
                + "that the district court applied the correct legal standard. Additional reasoning follows "
                + "in support of the panel's conclusion, making this paragraph clearly long enough to keep "
                + "as an independent translation unit within the rendered document.\n\n"
                + "Third paragraph is intentionally short.";

        List<String> segments = CaseOriginalTranslateServiceImpl.segmentTextForTranslation(text, 1600);

        assertThat(segments).hasSize(3);
        assertThat(segments.get(0))
                .startsWith("First paragraph sentence one.")
                .contains("Second sentence of the first paragraph.");
        assertThat(segments.get(1)).startsWith("Second paragraph starts here.");
        assertThat(segments.get(2)).isEqualTo("Third paragraph is intentionally short.");
    }

    @Test
    void segmentTextSplitsLongParagraphAtSentenceBoundary() {
        String longSentence = "This is a fairly long sentence that should still be kept inside one segment. ";
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            text.append("Sentence number ").append(i).append(" contains meaningful legal wording and details. ");
        }

        List<String> segments = CaseOriginalTranslateServiceImpl.segmentTextForTranslation(text.toString(), 1600);

        assertThat(segments.size()).isGreaterThan(1);
        assertThat(String.join(" ", segments).replaceAll("\\s+", " ").trim())
                .isEqualTo(text.toString().trim());
        for (String segment : segments) {
            assertThat(segment.length()).isLessThanOrEqualTo(1700);
        }
    }

    @Test
    void mergesVeryShortNeighbourParagraphsToAvoidTinyModelCalls() {
        String text = "Short heading.\n\nBody paragraph with enough words to make a meaningful translation unit "
                + "for the model, so merging happens only when the neighbour is really short.";

        List<String> segments = CaseOriginalTranslateServiceImpl.segmentTextForTranslation(text, 1600);

        // 第一段过短会与第二段合并
        assertThat(segments.size()).isEqualTo(1);
        assertThat(segments.get(0)).startsWith("Short heading.");
    }

    @Test
    void detectsChineseAndJapaneseText() {
        String chinese = "本判决书由中华人民共和国最高人民法院作出，双方当事人在合同履行过程中发生争议。";
        String japanese = "本件は、東京都における知的財産権に関する訴訟であり、裁判所は次のとおり判決する。";
        String english = "The United States Court of Appeals affirmed the district court's judgment in full.";

        assertThat(CaseOriginalTranslateServiceImpl.detectSourceLanguage(chinese, "US")).isEqualTo("zh");
        assertThat(CaseOriginalTranslateServiceImpl.detectSourceLanguage(japanese, "JPN")).isEqualTo("ja");
        assertThat(CaseOriginalTranslateServiceImpl.detectSourceLanguage(english, "US")).isEqualTo("en");
    }

    @Test
    void euFallbackReturnsAutoWhenTextLooksLatin() {
        assertThat(CaseOriginalTranslateServiceImpl.detectSourceLanguage(
                "Court opinions published on official databases remain the authoritative record.", "EU"))
                .isEqualTo("auto");
    }

    @Test
    void usFallbackReturnsEnglish() {
        assertThat(CaseOriginalTranslateServiceImpl.detectSourceLanguage(
                "Court opinions published on official databases remain the authoritative record.", "US"))
                .isEqualTo("en");
    }
}
