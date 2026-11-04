# 모바일 디자인 근거

2026-11-04 사용자 제공 URL에서 공식 디자인 문서로 이동해 아래 본문을 확인했다.

- 시작: https://document.pleos.ai/docs/pleos-only/getting-started
- 디자인 개요: https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro
- 색상: https://document.pleos.ai/docs/connect/guide/docs-design/foundations/colors
- 타이포그래피: https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography
- 기본 버튼: https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button

## 확인한 기준

일관된 용어와 상호작용, 짧고 명확한 정보, 충분한 대비, 처리 중 시각적 피드백을 사용한다. 색상은 배경·표면·텍스트·버튼 등 역할별 토큰으로 구성한다. 글자는 정보 위계를 일관되게 유지한다. 공식 시스템 서체 참조는 Asta Sans다. 기본 버튼은 눌림·비활성·로딩 상태를 구분하고, 로딩 중 중복 조작을 막는다.

## 적용 범위와 추가 확인

이 문서는 차량용이다. 독립 모바일 앱에서는 시각적 일관성을 참고하되 Vehicle SDK, 자동차 권한, 차량용 멀티윈도우 구조를 도입하지 않는다. 기존 Compose 테마와 패키지 안에서 확장한다.

색상 수치, 글자 크기 위계, 버튼 치수는 연결된 이미지 자산에 포함되어 있다. 현재 확인한 텍스트 본문만으로 해당 수치를 검증했다고 주장하지 않는다. 이미지 자산과 내비게이션·입력·알림 컴포넌트 상세 확인 후 테마에 반영해야 한다. 기존 기본 보라색 Material 테마를 Pleos 디자인 준수로 간주하지 않는다.

루트 README의 로컬 docs/guides 문서 및 server/README.md는 여전히 저장소에 없다. 위 공식 문서는 디자인 근거를 제공하지만 누락된 프로젝트 고유 아키텍처 문서를 대체하지 않는다.
