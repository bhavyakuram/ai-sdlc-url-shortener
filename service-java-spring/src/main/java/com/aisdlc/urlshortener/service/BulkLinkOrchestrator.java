package com.aisdlc.urlshortener.service;

import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.service.exception.BatchTooLargeException;
import com.aisdlc.urlshortener.service.exception.CodeSpaceExhaustedException;
import com.aisdlc.urlshortener.service.exception.CustomCodeTakenException;
import com.aisdlc.urlshortener.service.exception.EmptyBatchException;
import com.aisdlc.urlshortener.service.exception.InvalidCustomCodeShapeException;
import com.aisdlc.urlshortener.service.exception.InvalidUrlException;
import com.aisdlc.urlshortener.service.exception.ReservedCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The R-BULK-2 self-invocation fix (technical-design.md Section 2): a separate {@code
 * service}-layer bean that holds {@link LinkService} as a constructor-injected dependency
 * and drives the per-item loop for {@code POST /api/v1/links/batch} entirely through the
 * injected proxy -- {@code linkService.createLink(...)}, never {@code this.createLink(...)}.
 * {@link LinkService} itself receives zero edits; this class is the entire mitigation.
 *
 * <p>Whole-request validation ({@link EmptyBatchException}/{@link BatchTooLargeException})
 * runs here, before the per-item loop starts (feature-spec.md Section 4, R-BULK-5) -- zero
 * items are attempted for a request that fails either check.
 *
 * <p>All five reachable exception types from {@link LinkService#createLink} (covering all
 * seven {@code code} values -- technical-design.md Section 2's "5 vs 6 vs 7" note) are caught
 * per item and converted into a {@link BulkItemOutcome#failure}; none may propagate to {@code
 * ApiExceptionHandler} (R-BULK-4) or the partial-success model collapses to all-or-nothing on
 * the first failing item. One item's failure has zero effect on any other item's persistence
 * because each {@code createLink} call keeps its own {@code @Transactional} boundary,
 * guaranteed only when invoked through the proxy (R-BULK-2).
 */
@Service
public class BulkLinkOrchestrator {

    private static final int MAX_BATCH_SIZE = 100;

    private final Logger log = LoggerFactory.getLogger(BulkLinkOrchestrator.class);
    private final LinkService linkService;

    public BulkLinkOrchestrator(LinkService linkService) {
        this.linkService = linkService;
    }

    public List<BulkItemOutcome> processBatch(List<BulkLinkItem> items) {
        if (items == null || items.isEmpty()) {
            throw new EmptyBatchException("items must be a non-empty array");
        }
        if (items.size() > MAX_BATCH_SIZE) {
            throw new BatchTooLargeException("items.length must not exceed " + MAX_BATCH_SIZE);
        }

        List<BulkItemOutcome> outcomes = new ArrayList<>(items.size());
        for (BulkLinkItem item : items) {
            try {
                // Through the injected proxy -- never this.createLink(...). This is the
                // entire R-BULK-2 mitigation: LinkService.java is never touched, so there is
                // no self-invocation call site to introduce in the first place.
                ShortLinkEntity link = linkService.createLink(item.url(), item.customCode());
                outcomes.add(BulkItemOutcome.success(link));
            } catch (InvalidUrlException ex) {
                outcomes.add(BulkItemOutcome.failure(ex.getErrorCode(), ex.getMessage()));
            } catch (InvalidCustomCodeShapeException ex) {
                outcomes.add(BulkItemOutcome.failure("INVALID_CUSTOM_CODE_SHAPE", ex.getMessage()));
            } catch (ReservedCodeException ex) {
                outcomes.add(BulkItemOutcome.failure("RESERVED_CODE", ex.getMessage()));
            } catch (CustomCodeTakenException ex) {
                outcomes.add(BulkItemOutcome.failure("CUSTOM_CODE_TAKEN", ex.getMessage()));
            } catch (CodeSpaceExhaustedException ex) {
                // Pathological case -- worth operator visibility even though caught in-loop,
                // mirroring ApiExceptionHandler's own log.error for this exception
                // (rules/coding-standards.md Logging). Never rethrown: R-BULK-4 requires this
                // exception stay inside the loop like every other per-item failure.
                log.error("CODE_SPACE_EXHAUSTED for one item in a batch request", ex);
                outcomes.add(BulkItemOutcome.failure("CODE_SPACE_EXHAUSTED", ex.getMessage()));
            }
            // Deliberately NOT catching LinkUnavailableException or bare Exception here --
            // an exception outside this named set is a real bug, not a per-item outcome, and
            // must still propagate to ApiExceptionHandler.handleUnexpected -> 500
            // INTERNAL_ERROR (whole-request), rather than being silently absorbed into a fake
            // per-item result.
        }
        return outcomes;
    }
}
