// domain/plan/service/PlanGenerationService.java
package com.fitroute.domain.plan.service;

import com.fitroute.domain.plan.entity.Plan;
import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.domain.plan.repository.PlanItemRepository;
import com.fitroute.domain.plan.repository.PlanRepository;
import com.fitroute.domain.user.entity.UserProfile;
import com.fitroute.domain.user.event.UserSignedUpEvent;
import com.fitroute.domain.user.repository.UserProfileRepository;
import com.fitroute.global.ai.GeminiClient;
import com.fitroute.global.ai.GeminiPromptBuilder;
import com.fitroute.global.ai.GeminiResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanGenerationService {

    private final PlanRepository planRepository;
    private final PlanItemRepository planItemRepository;
    private final UserProfileRepository userProfileRepository;
    private final GeminiClient geminiClient;
    private final GeminiPromptBuilder promptBuilder;
    private final GeminiResponseParser responseParser;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserSignedUp(UserSignedUpEvent event) {
        doGenerate(event.userId());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateForUser(Long userId) {
        doGenerate(userId);
    }

    private void doGenerate(Long userId) {
        log.info("[PlanGeneration] Start - userId={}", userId);
        Plan plan = Plan.createGenerating(userId);
        planRepository.save(plan);

        try {
            UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException(
                            "UserProfile not found for userId=" + userId));

            String systemInstruction = promptBuilder.getPlanSystemInstruction();
            String userPrompt = promptBuilder.buildInitialPlanPrompt(profile);
            String rawResponse = geminiClient.call(systemInstruction, userPrompt);

            String planJson = responseParser.extractText(rawResponse);
            int dailyCalories = responseParser.parseDailyCalories(planJson);
            List<PlanItem> items = responseParser.parsePlanItems(planJson, plan);

            planItemRepository.saveAll(items);
            plan.complete(dailyCalories, planJson);

            log.info("[PlanGeneration] Success - userId={}, planId={}, dailyCalories={}",
                    userId, plan.getId(), dailyCalories);

        } catch (Exception e) {
            String msg = extractMeaningfulMessage(e);
            log.error("[PlanGeneration] Failed - userId={}, planId={}, error={}",
                    userId, plan.getId(), msg, e);
            plan.fail(msg);
        }
    }

    private String extractMeaningfulMessage(Exception e) {
        if (e instanceof HttpClientErrorException httpEx) {
            String body = httpEx.getResponseBodyAsString();
            String truncatedBody = body.length() > 500
                    ? body.substring(0, 500) + "... [truncated]"
                    : body;
            return "HTTP " + httpEx.getStatusCode() + ": " + truncatedBody;
        }
        if (e instanceof IllegalStateException) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}