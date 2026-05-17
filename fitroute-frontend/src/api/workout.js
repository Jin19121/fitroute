// src/api/weight.js
import apiClient from "./axios";

// 운동 항목 추가
export const addWorkoutItem = (data) =>
    apiClient.post("/api/plans/items", {
        type: "WORKOUT",
        category: data.category,
        name: data.name,
        calories: data.calories ?? 0,
        sets: data.sets ?? null,
        reps: data.reps ?? null,
        weightKg: data.weightKg ?? null,
        durationMin: data.durationMin ?? null,
    }).then((r) => r.data);

// 주간 운동 계획 조회
export const getWeeklyWorkoutPlan = (startDate) =>
    apiClient.get("/api/plans/workout/weekly", {
        params: startDate ? { startDate } : {},
    }).then((r) => r.data);