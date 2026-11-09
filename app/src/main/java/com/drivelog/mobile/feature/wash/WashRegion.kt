package com.drivelog.mobile.feature.wash

/** Codes match the existing Wash recommendation `region` query parameter. */
enum class WashRegion(val label: String) {
    SEOUL("서울"), BUSAN("부산"), DAEGU("대구"), INCHEON("인천"),
    GWANGJU("광주"), DAEJEON("대전"), ULSAN("울산"), SEJONG("세종"),
    GYEONGGI("경기 · 수원"), GANGWON("강원 · 춘천"),
    CHUNGBUK("충북 · 청주"), CHUNGNAM("충남 · 홍성"),
    JEONBUK("전북 · 전주"), JEONNAM("전남 · 무안"),
    GYEONGBUK("경북 · 안동"), GYEONGNAM("경남 · 창원"), JEJU("제주")
}
