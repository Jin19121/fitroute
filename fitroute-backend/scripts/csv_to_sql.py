"""
csv_to_sql.py
foods.csv → src/main/resources/data.sql 변환

사용법:
    python3 csv_to_sql.py

출력:
    data.sql — Spring Boot 시작 시 자동 적재
"""

import csv

INPUT_FILE = "foods.csv"
OUTPUT_FILE = "data.sql"


def main():
    rows = []
    with open(INPUT_FILE, encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)

    lines = [
        "-- 식약처 식품영양성분DB 기반 초기 데이터",
        "-- 자동 생성: csv_to_sql.py",
        "-- 출처: 공공데이터포털 (apis.data.go.kr)",
        "",
        "INSERT IGNORE INTO foods (food_code, name, category, serving_size, calories, protein, fat, carbs, tags, data_source) VALUES",
    ]

    value_lines = []
    for row in rows:
        name        = row["name"].replace("'", "''")
        tags        = row["tags"].replace("'", "''")
        serving     = row["serving_size"].replace("'", "''")

        value_lines.append(
            f"  ('{row['food_code']}', '{name}', '{row['category']}', '{serving}', "
            f"{row['calories']}, {row['protein']}, {row['fat']}, {row['carbs']}, "
            f"'{tags}', '{row['data_source']}')"
        )

    lines.append(",\n".join(value_lines) + ";")

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    print(f"✅ {len(rows)}개 → {OUTPUT_FILE} 생성 완료")
    print(f"📁 이 파일을 fitroute-backend/src/main/resources/data.sql 로 복사하세요")


if __name__ == "__main__":
    main()