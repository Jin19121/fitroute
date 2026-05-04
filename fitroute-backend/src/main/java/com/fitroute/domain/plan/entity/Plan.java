// domain/plan/entity/Plan.java
package com.fitroute.domain.plan.entity;

// import com.fitroute.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
// import org.springframework.data.annotation.CreatedDate;
// import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.time.LocalDate;

// domain/plan/entity/Plan.java
@Entity
@Table(name = "plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanStatus status;

    @Column(columnDefinition = "TEXT") // ✅ VARCHAR → TEXT
    private String errorMessage;

    @Column(columnDefinition = "LONGTEXT") // AI JSON은 클 수 있음
    private String rawJson;

    private Integer totalCalories;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PlanSource source;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static Plan createGenerating(Long userId) {
        Plan plan = new Plan();
        plan.userId = userId;
        plan.status = PlanStatus.GENERATING;
        plan.source = PlanSource.AI;
        return plan;
    }

    public void complete(int dailyCalories, String rawJson) {
        this.status = PlanStatus.ACTIVE;
        this.totalCalories = dailyCalories;
        this.rawJson = rawJson;
        this.startDate = LocalDate.now();
    }

    public void fail(String errorMessage) {
        this.status = PlanStatus.FAILED;
        // ✅ DB 컬럼 크기와 무관하게 방어적 truncate
        this.errorMessage = truncateErrorMessage(errorMessage, 5000);
    }

    private String truncateErrorMessage(String message, int maxLength) {
        if (message == null)
            return "Unknown error";
        return message.length() > maxLength
                ? message.substring(0, maxLength) + "... [truncated]"
                : message;
    }
}