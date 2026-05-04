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
        Long userId = event.userId();
        log.info("[PlanGeneration] Start - userId={}", userId);

        // 1. 초기 상태(GENERATING)로 플랜 생성 및 저장
        Plan plan = Plan.createGenerating(userId);
        planRepository.save(plan);

        try {
            UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException(
                            "UserProfile not found for userId=" + userId));

            // 2. AI 지시사항(System Instruction)과 사용자 데이터(User Prompt) 분리
            // 팁: promptBuilder에서 이 두 가지를 각각 생성하도록 설계하는 것이 좋습니다.
            String systemInstruction = promptBuilder.getPlanSystemInstruction();
            String userPrompt = promptBuilder.buildInitialPlanPrompt(profile);

            // 3. Gemini 호출 (인자 2개 전달로 컴파일 에러 해결)
            String rawResponse = geminiClient.call(systemInstruction, userPrompt);

            // 4. 응답 파싱
            String planJson = responseParser.extractText(rawResponse);
            int dailyCalories = responseParser.parseDailyCalories(planJson);
            List<PlanItem> items = responseParser.parsePlanItems(planJson, plan);

            // 5. 결과 저장 및 상태 완료 변경
            planItemRepository.saveAll(items);
            plan.complete(dailyCalories, planJson);

            log.info("[PlanGeneration] Success - userId={}, planId={}, dailyCalories={}",
                    userId, plan.getId(), dailyCalories);

        } catch (Exception e) {
            String errorMessage = extractMeaningfulMessage(e);
            log.error("[PlanGeneration] Failed - userId={}, planId={}, error={}",
                    userId, plan.getId(), errorMessage, e);
            plan.fail(errorMessage);
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