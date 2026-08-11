package com.example.myapplication

data class SmishingCase(
    val tag: String,
    val title: String,
    val description: String,
    val newsQuery: String,
)

val smishingCases =
    listOf(
        SmishingCase(
            tag = "택배 사칭",
            title = "\"배송 실패\" 택배 문자",
            description = "배송 실패·주소 확인을 이유로 단축 URL 클릭을 유도해요. 실제 택배사는 문자로 개인정보 재입력을 요구하지 않아요.",
            newsQuery = "택배 사칭 스미싱",
        ),
        SmishingCase(
            tag = "정부지원금 사칭",
            title = "\"지원금 대상자\" 안내 문자",
            description = "재난지원금·환급금 등을 미끼로 링크를 눌러 신청하게 유도해요. 정부기관은 문자 링크로 개인정보를 받지 않아요.",
            newsQuery = "정부지원금 사칭 스미싱",
        ),
        SmishingCase(
            tag = "지인 사칭",
            title = "\"폰이 고장났어\" 지인 사칭",
            description = "가족·지인을 사칭해 폰 파손을 이유로 다른 번호로 연락하게 하고, 상품권 구매나 송금을 요구해요.",
            newsQuery = "가족 지인 사칭 문자 사기",
        ),
        SmishingCase(
            tag = "청첩장·부고 사칭",
            title = "모바일 청첩장·부고장 문자",
            description = "모르는 번호로 온 청첩장·부고 링크는 악성 앱 설치를 유도할 수 있어요. 발신자를 반드시 확인하세요.",
            newsQuery = "모바일 청첩장 부고장 스미싱",
        ),
        SmishingCase(
            tag = "공공기관 사칭",
            title = "건강보험공단·국세청 사칭",
            description = "환급금·보험료 정산을 미끼로 링크 클릭이나 앱 설치를 유도해요. 공식 앱이나 홈페이지에서 직접 확인해야 해요.",
            newsQuery = "건강보험공단 국세청 사칭 문자",
        ),
        SmishingCase(
            tag = "결제 취소 사칭",
            title = "\"결제 완료\" 알림 문자",
            description = "구매한 적 없는 결제 확인 문자로 놀라게 한 뒤, 취소를 위해 링크나 전화를 걸도록 유도해요.",
            newsQuery = "결제 확인 문자 스미싱",
        ),
        SmishingCase(
            tag = "수사기관 사칭",
            title = "검찰·경찰 사칭 출석 요구",
            description = "사건에 연루됐다며 링크나 앱 설치, 계좌 이체를 요구해요. 수사기관은 문자로 이런 절차를 진행하지 않아요.",
            newsQuery = "검찰 경찰 사칭 문자 보이스피싱",
        ),
        SmishingCase(
            tag = "은행 앱 사칭",
            title = "은행 앱 업데이트 안내",
            description = "보안 강화를 이유로 가짜 앱 설치 링크를 보내 금융정보를 탈취해요. 앱은 반드시 공식 스토어에서만 설치하세요.",
            newsQuery = "은행 앱 업데이트 사칭 스미싱",
        ),
    )
