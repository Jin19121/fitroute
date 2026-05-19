// global/enums/PlanItemAction.java
package com.fitroute.global.enums;

public enum PlanItemAction {
    COMPLETE, // 1. 순수 완료 처리
    SKIP, // 2. 건너뛰기 처리
    MODIFY, // 3. 수정 이력만 기록 (상태는 PENDING 유지)
    COMPLETE_WITH_MODIFY, // 4. 수정 이력 기록과 동시에 완료 처리
    RESET // 5. 초기 대기 상태로 리셋
}
