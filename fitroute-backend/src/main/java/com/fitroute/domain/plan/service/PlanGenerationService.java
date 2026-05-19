// domain/plan/service/PlanGenerationService.java
package com.fitroute.domain.plan.service;

import com.fitroute.domain.log.service.LogService;
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
    private final LogService logService;

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

    // ─────────────────────────────────────────────────────────────────────
    // 핵심 생성 로직 (버전 관리 포함)
    // ─────────────────────────────────────────────────────────────────────

    private void doGenerate(Long userId, LocalDate date) {
        log.info("[PlanGeneration] Start - userId={}, date={}", userId, date);

        // 1. 기존 플랜 조회 → versioning 계산
        List<DailyPlan> existingPlans = dailyPlanRepository
                .findByUserIdAndPlanDateOrderByVersionDesc(userId, date);

        int newVersion = 1;
        Long rootPlanId = null;

        if (!existingPlans.isEmpty()) {
            DailyPlan latest = existingPlans.get(0);
            newVersion = latest.getVersion() + 1;
            rootPlanId = latest.getRootPlanId();

            // ACTIVE 플랜 SUPERSEDE
            existingPlans.stream()
                    .filter(p -> p.getStatus() == DailyPlan.PlanStatus.ACTIVE)
                    .forEach(DailyPlan::supersede);

            log.info("[PlanGeneration] Superseding v{} → creating v{}",
                    latest.getVersion(), newVersion);
        }

        // 2. 새 GENERATING 플랜 저장
        DailyPlan dailyPlan = DailyPlan.builder()
                .userId(userId)
                .planDate(date)
                .version(newVersion)
                .rootPlanId(rootPlanId)
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

            int dailyCalories = responseParser.parseDailyCalories(planJson);
            Map<String, Object> mealPlan = responseParser.parseMealPlan(planJson);
            Map<String, Object> workoutPlan = responseParser.parseWorkoutPlan(planJson);
            Map<String, Object> aiMeta = Map.of(
                    "rawResponse", planJson,
                    "generatedAt", LocalDateTime.now().toString(),
                    "version", newVersion);

            List<PlanItem> items = responseParser.parsePlanItems(planJson, dailyPlan);
            planItemRepository.saveAll(items);

            dailyPlan.complete(dailyCalories, mealPlan, workoutPlan, aiMeta);

            // ★ PHASE 3: Log 초기화 (재생성 시 리셋, 최초 생성 시 신규)
            if (newVersion > 1) {
                logService.resetForNewPlan(dailyPlan);
            } else {
                logService.createForPlan(dailyPlan);
            }

            log.info("[PlanGeneration] Success v{} - userId={}, planId={}, dailyCalories={}",
                    newVersion, userId, dailyPlan.getId(), dailyCalories);

        } catch (Exception e) {
            dailyPlan.fail();
            log.error("[PlanGeneration] Failed v{} - userId={}, planId={}, error={}",
                    newVersion, userId, dailyPlan.getId(), extractMeaningfulMessage(e), e);
        }
    }

    private String extractMeaningfulMessage(Exception e) {
        if (e instanceof HttpClientErrorException httpEx) {
            String body = httpEx.getResponseBodyAsString();
            return "HTTP " + httpEx.getStatusCode() + ": "
                    + (body.length() > 500 ? body.substring(0, 500) + "...[truncated]" : body);
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}