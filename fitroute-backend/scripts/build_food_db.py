"""
build_food_db.py
국가표준식품성분 DB 10.4 정제 스크립트

입력: food2.xlsx
출력:
  - foods_clean.csv  : 정제된 전체 데이터
  - data.sql         : Spring Boot 자동 적재용 INSERT 문

사용법:
  python3 build_food_db.py --input food2.xlsx
"""

import argparse
import pandas as pd

# ─── 식품군 → category 매핑 ─────────────────────────────────────────────
FOOD_GROUP_MAP = {
    "곡류 및 그 제품":      "BREAKFAST",
    "감자류 및 전분류":     "BREAKFAST",
    "난류":                 "BREAKFAST",
    "우유 및 그 제품":      "BREAKFAST",
    "두류":                 "LUNCH",
    "채소류":               "LUNCH",
    "버섯류":               "LUNCH",
    "조리가공식품류":       "LUNCH",
    "육류 및 그 제품":      "DINNER",
    "어패류 및 그 제품":    "DINNER",
    "해조류":               "DINNER",
    "과일류":               "SNACK",
    "견과류 및 종실류":     "SNACK",
    # 아래는 단독 식품으로 부적합 → 제외
    "당류":                 None,
    "음료류":               None,
    "유지류":               None,
    "조미료류":             None,
    "주류":                 None,
    "차류":                 None,
    "기타":                 None,
}

# ─── 식품명 제외 키워드 ──────────────────────────────────────────────────
EXCLUDE_KEYWORDS = [
    "껌", "과당", "설탕", "포도당", "물엿", "시럽",
    "전분", "밀가루", "쌀가루",
    "오일", "버터", "마가린", "쇼트닝",
    "소금", "간장", "식초", "젓갈", "액젓",
        # 추가 — 생것/원재료
    ", 생것",        # "닭고기, 살코기, 생것" 패턴
    ", 말린것",
    ", 날것",
    "살코기",        # 단독 살코기
    "내장",
    "부산물",
    "뼈",
    ", 껍질",
    "원액",
    "분말",
    "엑기스",
]


def build_tags(row: dict) -> str:
    tags = []
    if row["protein"] >= 15:
        tags.append("고단백")
    if row["fat"] <= 5:
        tags.append("저지방")
    if row["calories"] <= 100:
        tags.append("저칼로리")
    fg = row["food_group"]
    if fg in ("채소류", "버섯류", "해조류"):
        tags.append("채소")
    if fg == "과일류":
        tags.append("과일")
    if fg in ("육류 및 그 제품", "어패류 및 그 제품"):
        tags.append("근육")
    if fg in ("곡류 및 그 제품", "감자류 및 전분류"):
        tags.append("탄수화물")
    return ",".join(tags) if tags else "일반"


def should_exclude(name: str) -> bool:
    if not isinstance(name, str):
        return True
    return any(kw in name for kw in EXCLUDE_KEYWORDS)


def main(input_file: str):
    print(f"파일 로드 중: {input_file}")

    # ── 1. 10.4 시트 로드 ─────────────────────────────────────────────
    df_raw = pd.read_excel(
        input_file,
        sheet_name="국가표준식품성분 Database 10.4",
        header=None,
        skiprows=3,
        dtype={1: "Int64"}
    )
    df = df_raw[[1, 3, 4, 6, 8, 9, 11]].copy()
    df.columns = ["db_index", "food_group", "name", "calories", "protein", "fat", "carbs"]
    print(f"  원본 행 수: {len(df)}")

    # ── 2. 부록2 JOIN → 식품코드 ─────────────────────────────────────
    df_meta = pd.read_excel(
        input_file,
        sheet_name="부록2)식품코드,국문명,영문명,학명 정보 ",
        header=0,
        dtype={"DB색인": "Int64"}
    )[["DB색인", "식품코드"]].rename(columns={"DB색인": "db_index", "식품코드": "food_code"})
    df = df.merge(df_meta, on="db_index", how="left")

    # ── 3. 정제 ───────────────────────────────────────────────────────
    df = df[df["name"].notna()]

    for col in ["calories", "protein", "fat", "carbs"]:
        df[col] = pd.to_numeric(df[col], errors="coerce").fillna(0)

    df = df[df["calories"] > 0]

    df["calories"] = df["calories"].round().astype(int)
    df["protein"]  = df["protein"].round().astype(int)
    df["fat"]      = df["fat"].round().astype(int)
    df["carbs"]    = df["carbs"].round().astype(int)

    # 식품군 필터
    df["category"] = df["food_group"].map(FOOD_GROUP_MAP)
    df = df[df["category"].notna()]

    # 키워드 필터
    df = df[~df["name"].apply(should_exclude)]

    print(f"  정제 후 행 수: {len(df)}")

    # ── 4. 태그 / 출처 / 용량 ─────────────────────────────────────────
    df["tags"]        = df.apply(lambda r: build_tags(r.to_dict()), axis=1)
    df["data_source"] = "MFDS_10.4"
    df["serving_size"]= "100g"

    # food_code null 방어
    null_mask = df["food_code"].isna()
    df.loc[null_mask, "food_code"] = [f"TEMP_{i:05d}" for i in range(null_mask.sum())]

    # ── 5. 최종 컬럼 정리 ────────────────────────────────────────────
    df_out = df[[
        "food_code", "name", "category", "serving_size",
        "calories", "protein", "fat", "carbs",
        "tags", "data_source"
    ]].reset_index(drop=True)

    # ── 6. CSV 저장 ───────────────────────────────────────────────────
    df_out.to_csv("foods_clean.csv", index=False, encoding="utf-8")
    print(f"\n✅ CSV 저장: foods_clean.csv ({len(df_out)}개)")

    print("\n카테고리별 분포:")
    for cat, count in df_out["category"].value_counts().items():
        print(f"  {cat}: {count}개")

    print("\n샘플 (카테고리별 2개):")
    for cat in ["BREAKFAST", "LUNCH", "DINNER", "SNACK"]:
        print(f"  [{cat}]")
        for _, row in df_out[df_out["category"] == cat].head(2).iterrows():
            print(f"    {row['name']} | {row['calories']}kcal | P:{row['protein']}g F:{row['fat']}g C:{row['carbs']}g | {row['tags']}")

    # ── 7. data.sql 생성 ──────────────────────────────────────────────
    with open("data.sql", "w", encoding="utf-8") as f:
        f.write("-- 국가표준식품성분 DB 10.4 기반 초기 데이터\n")
        f.write("-- 출처: 농촌진흥청 국가표준식품성분 Database 10.4\n")
        f.write("-- 기준: 가식부 100g 당 영양소 함량\n\n")
        f.write("INSERT IGNORE INTO foods "
                "(food_code, name, category, serving_size, calories, protein, fat, carbs, tags, data_source) VALUES\n")

        rows = []
        for _, row in df_out.iterrows():
            name      = str(row["name"]).replace("'", "''")
            tags      = str(row["tags"]).replace("'", "''")
            food_code = str(row["food_code"]).replace("'", "''")
            rows.append(
                f"  ('{food_code}', '{name}', '{row['category']}', '100g', "
                f"{row['calories']}, {row['protein']}, {row['fat']}, {row['carbs']}, "
                f"'{tags}', 'MFDS_10.4')"
            )
        f.write(",\n".join(rows) + ";\n")

    print(f"\n✅ SQL 저장: data.sql")
    print("\n다음 단계:")
    print("  1. foods_clean.csv 확인")
    print("  2. data.sql → fitroute-backend/src/main/resources/data.sql 복사")
    print("  3. application.yml: spring.sql.init.mode=always 추가")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", default="food2.xlsx")
    args = parser.parse_args()
    main(args.input)