"""
parse_local_foods.py
단일 정형화 시트 고속 파싱 및 FitRoute AI 데이터셋 빌드 스크립트
"""

import pandas as pd
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
TARGET_FILE_NAME = "food_data.xlsx"  # 식품코드가 정제/결합된 단일 시트 파일
RAW_DATA_FILE = os.path.join(BASE_DIR, "..", "data", TARGET_FILE_NAME)
OUTPUT_FILE = os.path.join(BASE_DIR, "..", "foods.csv")

# 타깃 매칭 명세 (공백 제거 버전)
TARGET_FOODS = {
    "바나나": {"category": "BREAKFAST", "tags": "탄수화물,과일", "type": "Natural", "keyword": "바나나,생것"},
    "사과": {"category": "BREAKFAST", "tags": "과일,저칼로리", "type": "Natural", "keyword": "사과,후지,생것"},
    "우유": {"category": "BREAKFAST", "tags": "단백질,칼슘", "type": "Natural", "keyword": "우유,저지방우유"},
    "귀리": {"category": "BREAKFAST", "tags": "고단백,다이어트", "type": "Natural", "keyword": "귀리,겉귀리,도정,생것"},
    "고구마": {"category": "BREAKFAST", "tags": "탄수화물,구황작물", "type": "Natural", "keyword": "고구마,찐것"},
    "닭가슴살": {"category": "LUNCH", "tags": "고단백,저지방", "type": "Natural", "keyword": "닭고기,가슴살,생것"},
    "달걀": {"category": "BREAKFAST", "tags": "고단백,저지방", "type": "Natural", "keyword": "달걀,전란,생것"},
    "연어": {"category": "DINNER", "tags": "고단백,오메가3", "type": "Natural", "keyword": "연어,대서양,생것"},
    "흰쌀밥": {"category": "LUNCH", "tags": "탄수화물,한식", "type": "Cooked", "keyword": "멥쌀,백미,밥"},
    "잡곡밥": {"category": "LUNCH", "tags": "탄수화물,한식", "type": "Cooked", "keyword": "잡곡밥"},
    "식빵": {"category": "BREAKFAST", "tags": "탄수화물,베이커리", "type": "Cooked", "keyword": "식빵"}
}

def clean_value(val):
    if pd.isna(val):
        return 0.0
    s_val = str(val).strip().lower()
    if s_val in ["-", "tr", "na", "n/a", ""]:
        return 0.0
    try:
        return round(float(s_val), 1)
    except ValueError:
        return 0.0

def main():
    if not os.path.exists(RAW_DATA_FILE):
        print(f"❌ 원본 파일 트레이싱 실패: {RAW_DATA_FILE}")
        return

    print("🔍 [FitRoute AI Engine] 단일 마스터 덤프 고속 파싱 파이프라인 기동...")
    
    try:
        # 시트 매칭 로직을 완전히 배제하고 첫 번째 시트를 직접 로드
        df = pd.read_excel(RAW_DATA_FILE, header=3)
    except Exception as e:
        print(f"❌ 엑셀 로드 중 에러 발생: {e}")
        return

    # 정형화된 위치 기반 절대 인덱스 매퍼 적용
    col_code = df.columns[0]   # 결합된 식품코드 (A열)
    col_name = df.columns[3]   # 식품명
    col_cal  = df.columns[5]   # 에너지(kcal)
    col_prot = df.columns[7]   # 단백질(g)
    col_fat  = df.columns[8]   # 지방(g)
    col_carb = df.columns[10]  # 탄수화물(g)

    final_results = []
    df[col_name] = df[col_name].astype(str).str.replace(r'\s+', '', regex=True)

    for name, meta in TARGET_FOODS.items():
        keyword = meta["keyword"]
        valid_df = df[df[col_name].notna()]
        
        if "," in keyword:
            parts = [p.strip() for p in keyword.split(",")]
            condition = valid_df[col_name].str.contains(parts[0], na=False)
            for part in parts[1:]:
                condition &= valid_df[col_name].str.contains(part, na=False)
            candidates = valid_df[condition]
        else:
            candidates = valid_df[valid_df[col_name].str.contains(keyword, na=False)]
            
        if candidates.empty:
            candidates = valid_df[valid_df[col_name].str.contains(name, na=False)]
            if candidates.empty:
                print(f"  ⚠️ [{name}] 매칭 데이터 누락 (스킵)")
                continue

        selected_row = candidates.iloc[0]
        
        final_results.append({
            "food_code": str(selected_row.get(col_code, "N/A")).strip(),
            "name": name,
            "type": meta["type"],
            "category": meta["category"],
            "serving_size": "100g",
            "calories": int(clean_value(selected_row.get(col_cal, 0))),
            "protein": clean_value(selected_row.get(col_prot, 0)),
            "fat": clean_value(selected_row.get(col_fat, 0)),
            "carbs": clean_value(selected_row.get(col_carb, 0)),
            "tags": meta["tags"],
            "data_source": "RDA_DB_10.4"
        })
        print(f"  ✅ [매핑 성공] {name} -> 로드 완료")

    result_df = pd.DataFrame(final_results)
    result_df.to_csv(OUTPUT_FILE, index=False, encoding="utf-8-sig")
    print(f"\n🎉 [마이그레이션 완수] {OUTPUT_FILE} 단일 인프라 마스터셋 빌드 완료!")

if __name__ == "__main__":
    main()