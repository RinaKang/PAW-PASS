-- 2026-09-12: KCISA 시설 대표사진용 Google Places (New) 연동.
-- place_id는 구글 정책상 장기 저장 가능(사진 이름/URL과 달리 캐싱 제약 없음) - 배치 동기화 시점에
-- 시설명+주소로 한 번 검색해서 저장해두고, 실제 사진은 요청 시점마다 이 id로 새로 조회한다.
ALTER TABLE pet_facilities
    ADD COLUMN google_place_id VARCHAR(255) NULL;
