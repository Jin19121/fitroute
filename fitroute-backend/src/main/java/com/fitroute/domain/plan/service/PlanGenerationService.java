// domain/plan/service/PlanGenerationService.java
package com.fitroute.domain.plan.service;

import com.fitroute.domain.plan.entity.DailyPlan;
import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.domain.plan.repository.DailyPlanRepository;
import com.fitroute.domain.plan.repository.PlanItemRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanGenerationService {

    private final DailyPlanRepository dailyPlanRepository;
    private final PlanItemRepository planItemRepository;
    private final UserProfileRepository userProfileRepository;
    private final GeminiClient geminiClient;
    private final GeminiPromptBuilder promptBuilder;
    private final GeminiResponseParser responseParser;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserSignedUp(UserSignedUpEvent event) {
        doGenerate(event.userId(), LocalDate.now());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateForUser(Long userId) {
        doGenerate(userId, LocalDate.now());
    }

    private void doGenerate(Long userId, LocalDate date) {
        log.info("[PlanGeneration] Start - userId={}, date={}", userId, date);

        // GENERATING 상태로 먼저 저장 (실패 추적 가능)
        DailyPlan dailyPlan = DailyPlan.builder()
                .userId(userId)
                .planDate(date)
                .build();
        dailyPlanRepository.save(dailyPlan);

        try {
            UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException(
                            "UserProfile not found for userId=" + userId));

            String systemInstruction = promptBuilder.getPlanSystemInstruction();
            String userPrompt = promptBuilder.buildInitialPlanPrompt(profile);
            String rawResponse = geminiClient.call(systemInstruction, userPrompt);
            String planJson = responseParser.extractText(rawResponse);

            // 정제된 JSON 구성
            int dailyCalories = responseParser.parseDailyCalories(planJson);
            Map<String, Object> mealPlan = responseParser.parseMealPlan(planJson);
            Map<String, Object> workoutPlan = responseParser.parseWorkoutPlan(planJson);
            Map<String, Object> aiMeta = Map.of(
                    "rawResponse", planJson,
                    "generatedAt", LocalDateTime.now().toString());

            // PlanItem 저장
            List<PlanItem> items = responseParser.parsePlanItems(planJson, dailyPlan);
            planItemRepository.saveAll(items);

            // DailyPlan 완료 처리
            dailyPlan.complete(dailyCalories, mealPlan, workoutPlan, aiMeta);

            log.info("[PlanGeneration] Success - userId={}, planId={}, dailyCalories={}",
                    userId, dailyPlan.getId(), dailyCalories);

        } catch (Exception e) {
            dailyPlan.fail();
            String msg = extractMeaningfulMessage(e);
            log.error("[PlanGeneration] Failed - userId={}, planId={}, error={}",
                    userId, dailyPlan.getId(), msg, e);
        }
    }

    private String extractMeaningfulMessage(Exception e) {
        if (e instanceof HttpClientErrorException httpEx) {
            String body = httpEx.getResponseBodyAsString();
            String truncated = body.length() > 500 ? body.substring(0, 500) + "... [truncated]" : body;
            return "HTTP " + httpEx.getStatusCode() + ": " + truncated;
        }
        if (e instanceof IllegalStateException) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}