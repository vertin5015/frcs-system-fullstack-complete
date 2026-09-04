package com.hnu.legal_cases.dto.crawler;

import com.hnu.legal_cases.enums.CountryEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A parallel search batch plus the ordered source list used to create it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrawlerSearchBatch {

    private List<CompletableFuture<CrawlerSingleQueryResult>> futures;

    private List<CountryEnum> targetCountries;
}
