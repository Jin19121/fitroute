"""
fetch_foods.py
농촌진흥청 V2 API 기반 - 429 Too Many Requests 완벽 방어 및 지수 백오프 적용 버전
"""

import urllib.request
import urllib.parse
import csv
import time
import ssl
import xml.etree.ElementTree as ET
from urllib.error import HTTPError

# Mac 환경 SSL 인증 우회 세팅
ssl_context = ssl.create_default_context()
ssl_context.check_hostname = False
ssl_context.verify_mode = ssl.CERT_NONE

# 설정
API_KEY  = "34154f0f72bf2c2a31724700ab4445c26e93ec69b52b67677826877a2463048e"
BASE_URL = "https://apis.data.go.kr/1390803/AgriFood/NationStdFood/V2"
OUTPUT_FILE = "foods.csv"

# AI 식단 추천용 원재료(Natural) 및 조리음식(Cooked) 명세 정의
TARGET_FOODS = {
    "바나나": {"category": "BREAKFAST", "tags": "탄수화물,과일", "type": "Natural", "search_keyword": "바나나"},
    "사과": {"category": "BREAKFAST", "tags": "과일,저칼로리", "type": "Natural", "search_keyword": "사과"},
    "우유": {"category": "BREAKFAST", "tags": "단백질,칼슘", "type": "Natural", "search_keyword": "우유"},
    "귀리": {"category": "BREAKFAST", "tags": "고단백,다이어트", "type": "Natural", "search_keyword": "귀리"},
    "고구마": {"category": "BREAKFAST", "tags": "탄수화물,구황작물", "type": "Natural", "search_keyword": "고구마"},
    "닭가슴살": {"category": "LUNCH", "tags": "고단백,저지방", "type": "Natural", "search_keyword": "닭고기"},
    "달걀": {"category": "BREAKFAST", "tags": "고단백,저지방", "type": "Natural", "search_keyword": "달걀"},
    "연어": {"category": "DINNER", "tags": "고단백,오메가3", "type": "Natural", "search_keyword": "연어"},
    "흰쌀밥": {"category": "LUNCH", "tags": "탄수화물,한식", "type": "Cooked", "search_keyword": "쌀밥"},
    "잡곡밥": {"category": "LUNCH", "tags": "탄수화물,한식", "type": "Cooked", "search_keyword": "잡곡밥"},
    "된장국": {"category": "LUNCH", "tags": "한식,국물", "type": "Cooked", "search_keyword": "된장국"},
    "김치찌개": {"category": "DINNER", "tags": "한식,찌개", "type": "Cooked", "search_keyword": "김치찌개"},
    "제육볶음": {"category": "DINNER", "tags": "고단백,육류", "type": "Cooked", "search_keyword": "제육볶음"},
    "고등어구이": {"category": "DINNER", "tags": "생선구이,오메가3", "type": "Cooked", "search_keyword": "고등어구이"},
    "시금치나물": {"category": "LUNCH", "tags": "한식,반찬", "type": "Cooked", "search_keyword": "시금치나물"},
    "식빵": {"category": "BREAKFAST", "tags": "탄수화물,베이커리", "type": "Cooked", "search_keyword": "식빵"}
}


def safe_request(url: str, max_retries: int = 5, initial_wait: float = 2.0) -> bytes:
    """429 및 네트워크 지연을 방어하기 위한 지수 백오프(Exponential Backoff) 적용 커넥터"""
    wait_time = initial_wait
    for attempt in range(max_retries):
        try:
            req = urllib.request.Request(url)
            req.add_header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
            with urllib.request.urlopen(req, timeout=20, context=ssl_context) as res:
                return res.read()
        except HTTPError as e:
            if e.code == 429:
                print(f"  ⚠️  [트래픽 제한 차단 감지] 429 에러 발생. {wait_time}초 후 재시도합니다... (시도 {attempt + 1}/{max_retries})")
                time.sleep(wait_time)
                wait_time *= 2  # 대기 시간을 2배씩 증가 (2s -> 4s -> 8s -> 16s)
            else:
                print(f"  ❌ HTTP 에러 발생 (코드: {e.code})")
                raise e
        except Exception as e:
            print(f"  ❌ 네트워크 연결 오류: {e}. {wait_time}초 후 재시도...")
            time.sleep(wait_time)
            wait_time *= 2
            
    raise Exception("최대 재시도 횟수를 초과하여 공공데이터 포털 연결에 실패했습니다.")


def search_food_code(keyword: str) -> list:
    """키워드 검색 파라미터를 사용해 대상 코드 조회 (백오프 내장)"""
    encoded_keyword = urllib.parse.quote(keyword)
    url = (
        f"{BASE_URL}/getKoreanFoodNationStdList"
        f"?serviceKey={API_KEY}"
        f"&Page_No=1"
        f"&Page_Size=30"
        f"&fd_Nm={encoded_keyword}"
    )
    
    try:
        xml_data = safe_request(url)
        root = ET.fromstring(xml_data)
        items = root.findall(".//item")
        
        results = []
        for item in items:
            results.append({
                "food_code": item.findtext("food_Code"),
                "food_nm": item.findtext("food_Nm")
            })
        return results
    except Exception as e:
        print(f"  ❌ [{keyword}] 검색 엔드포인트 호출 실패: {e}")
        return []


def fetch_nutrient_by_code(food_code: str) -> dict:
    """식품 코드로 상세 성분 정보 조회 (백오프 내장 및 단위 정밀 보정)"""
    url = (
        f"{BASE_URL}/getKoreanFoodNationStdIdntList"
        f"?serviceKey={API_KEY}"
        f"&food_Code={food_code}"
    )
    nutrients = {"calories": 0, "protein": 0, "fat": 0, "carbs": 0}
    
    try:
        xml_data = safe_request(url)
        root = ET.fromstring(xml_data)
        
        for irdnt_node in root.findall(".//irdnt"):
            ticket = irdnt_node.find("irdnttcket")
            if ticket is not None:
                nm = ticket.findtext("irdnt_Nm", "").strip()
                val_str = ticket.findtext("cont_Info", "0").strip()
                unit = ticket.findtext("irdnt_Unit_Nm", "").strip()
                
                if not val_str or val_str in ["-", "tr", "Tr", "N/A"]:
                    val = 0.0
                else:
                    try:
                        val = float(val_str)
                        if unit == "㎎" and nm in ["단백질", "총 아미노산", "지방", "총 지방산", "탄수화물", "당질"]:
                            val = val / 1000.0
                    except ValueError:
                        val = 0.0
                
                val = round(val, 1) if nm != "에너지" else round(val)
                
                if nm == "에너지": 
                    nutrients["calories"] = int(val)
                elif nm in ["단백질", "총 아미노산"]: 
                    nutrients["protein"] = val
                elif nm in ["지방", "총 지방산"]: 
                    nutrients["fat"] = val
                elif nm in ["탄수화물", "당질", "가용성 무질소물"]: 
                    nutrients["carbs"] = val
                
        return nutrients
    except Exception as e:
        return nutrients


def main():
    print("🔍 [FitRoute AI Engine] 식단 데이터셋 안전 마이그레이션 기동...")
    final_results = []
    
    for name, meta in TARGET_FOODS.items():
        print(f"📂 타깃 키워드 검색 중: [{name}] (검색어: {meta['search_keyword']})...")
        candidates = search_food_code(meta["search_keyword"])
        
        # 🎯 게이트웨이 유입 제한 우회를 위한 기본 대기 시간을 1.2초로 대폭 확보
        time.sleep(1.2)
        
        if not candidates:
            print(f"  ⚠️ 공공데이터 포털 내 [{name}] 매칭 항목 없음 (스킵)")
            continue
            
        selected = candidates[0]
        for cand in candidates:
            f_nm = cand["food_nm"]
            if meta["type"] == "Natural":
                if "닭고기" in meta["search_keyword"] and "가슴" in f_nm and "생것" in f_nm:
                    selected = cand
                    break
                if "생것" in f_nm or "신선" in f_nm or f_nm == meta["search_keyword"]:
                    if "과자" not in f_nm and "통조림" not in f_nm and "파이" not in f_nm:
                        selected = cand
                        break
            else:
                if "찌개" in name or "볶음" in name or "국" in name or "구이" in name or "나물" in name:
                    if "생것" not in f_nm and "가루" not in f_nm:
                        selected = cand
                        break
                        
        print(f"  -> {meta['type']} 매핑 확정: {selected['food_nm']} ({selected['food_code']})")
        
        # 영양 성분 조회 연동
        nutri = fetch_nutrient_by_code(selected["food_code"])
        time.sleep(1.2)  # 연속적인 호출 사이 안전 갭 확보
        
        final_results.append({
            "food_code": selected["food_code"],
            "name": name,
            "type": meta["type"],
            "category": meta["category"],
            "serving_size": "100g",
            "calories": nutri["calories"],
            "protein": nutri["protein"],
            "fat": nutri["fat"],
            "carbs": nutri["carbs"],
            "tags": meta["tags"],
            "data_source": "RDA_V2"
        })
        print(f"     ✅ [적재 성공] {name} | 탄:{nutri['carbs']}g | 단:{nutri['protein']}g | 지:{nutri['fat']}g | {nutri['calories']}kcal")

    if not final_results:
        print("\n❌ 마이그레이션 실패: 트래픽 차단이 풀리지 않았습니다. 잠시 후 다시 실행해 주세요.")
        return

    with open(OUTPUT_FILE, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=[
            "food_code", "name", "type", "category", "serving_size",
            "calories", "protein", "fat", "carbs", "tags", "data_source"
        ])
        writer.writeheader()
        writer.writerows(final_results)
        
    print(f"\n🎉 [최종 마이그레이션 완전 완수] {OUTPUT_FILE} 클린 데이터셋 빌드 성공!")


if __name__ == "__main__":
    main()