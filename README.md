# Price Analyzer (가격 분석기)

이 프로젝트는 Spring AI, Ollama(LLM), 그리고 Qdrant(Vector Database)를 활용하여 사용자가 제안한 제품 가격이 시장에서 경쟁력이 있는지 분석해주는 RAG(Retrieval-Augmented Generation) 기반의 애플리케이션입니다.

## 1. 프로젝트 구조

```text
price-analyzer/
├── src/main/java/com/zoontopia/priceanalyzer/
│   ├── controller/
│   │   ├── PageController.java             # 메인 화면(index.html) 제공
│   │   └── PriceAnalysisRestController.java # 가격 분석 API 제공
│   ├── dto/
│   │   ├── AnalyzeRequest.java              # 분석 요청 데이터 구조
│   │   └── AnalyzeResponse.java             # 분석 응답 데이터 구조
│   ├── model/
│   │   └── Product.java                     # 제품 데이터 모델
│   ├── service/
│   │   ├── PriceAnalysisService.java        # AI 분석 로직 (RAG 구현)
│   │   ├── DataIngestionService.java        # JSON 데이터를 Qdrant에 적재
│   │   └── ChartService.java                # (선택적) 차트 데이터 처리
│   └── PriceAnalyzerApplication.java        # Spring Boot 메인 클래스
├── src/main/resources/
│   ├── application-local.yml                # 로컬 환경 설정 (Ollama, Qdrant 연결 정보)
│   ├── products.json                        # 시장 제품 데이터 (시뮬레이션용)
│   └── templates/
│       └── index.html                       # 사용자 웹 인터페이스
└── build.gradle                             # 프로젝트 의존성 관리 (Spring AI 등)
```

## 2. 주요 소스코드 설명

- **`PriceAnalysisService`**:
    - 사용자가 입력한 제품명으로 Qdrant에서 유사한 제품들을 검색(Similarity Search)합니다.
    - 검색된 시장 가격 데이터를 프롬프트에 포함하여 Ollama(LLM)에게 분석을 요청합니다.
    - LLM은 시장 평균가 계산, 경쟁력 평가, 추천 사항을 포함한 보고서를 생성합니다.
- **`DataIngestionService`**:
    - 애플리케이션 시작 시 `products.json` 파일을 읽어 제품 정보를 Embedding으로 변환한 후 Qdrant Vector Store에 저장합니다.
    - 이미 데이터가 존재하는 경우 중복 적재를 방지하는 로직이 포함되어 있습니다.
- **`PriceAnalysisRestController`**:
    - `/api/analyze` 엔드포인트를 통해 클라이언트의 요청을 받아 분석 결과를 반환합니다.

## 3. 로컬 실행을 위한 사전 준비

이 프로젝트를 로컬에서 실행하려면 **Java 17**, **Ollama**, **Qdrant**가 설치 및 실행 중이어야 합니다.

### 공통 필수 사항 (Ollama 모델 다운로드)
Ollama가 설치된 후 터미널(또는 CMD)에서 아래 명령어를 실행하여 모델을 다운로드해야 합니다.
```bash
# LLM 모델 (EXAONE 3.5 2.4B)
ollama pull exaone3.5:2.4b

# Embedding 모델 (BGE-M3)
ollama pull bge-m3
```

---

### [Windows 버전]

1.  **JDK 17 설치**:
    - [Oracle JDK 17](https://www.oracle.com/java/technologies/downloads/#java17) 또는 [Adoptium(Temurin)](https://adoptium.net/temurin/releases/?version=17)에서 설치 프로그램을 다운로드하여 실행합니다.
    - 환경 변수(`JAVA_HOME`)가 설정되어 있는지 확인합니다 (`java -version`).
2.  **Ollama 설치**:
    - [Ollama 공식 홈페이지](https://ollama.com/download/windows)에서 Windows용 설치 파일을 다운로드하여 설치합니다.
3.  **Docker Desktop 설치 (Qdrant 실행용)**:
    - [Docker Desktop](https://www.docker.com/products/docker-desktop/)을 설치합니다.
    - 터미널(PowerShell/CMD)에서 Qdrant 컨테이너를 실행합니다:
      ```powershell
      docker run -d -p 6333:6333 -p 6334:6334 qdrant/qdrant
      ```

### [macOS 버전]

1.  **JDK 17 설치**:
    - Homebrew를 사용하여 설치하는 것을 권장합니다:
      ```bash
      brew install openjdk@17
      # 심볼릭 링크 설정 (필요한 경우)
      sudo ln -sfn /usr/local/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
      ```
2.  **Ollama 설치**:
    - [Ollama 공식 홈페이지](https://ollama.com/download/mac)에서 Mac용 앱을 다운로드하여 설치합니다.
3.  **Qdrant 실행**:
    - Docker가 설치되어 있다면 아래 명령어를 사용합니다:
      ```bash
      docker run -d -p 6333:6333 -p 6334:6334 qdrant/qdrant
      ```
    - 또는 Homebrew를 통해 직접 설치할 수도 있습니다 (Docker 권장).

---

## 4. 프로젝트 실행 방법

1.  **애플리케이션 실행**:
    프로젝트 루트 디렉토리에서 아래 명령어를 입력합니다.
    ```bash
    # Windows
    ./gradlew.bat bootRun

    # macOS/Linux
    ./gradlew bootRun
    ```
2.  **접속**:
    브라우저에서 `http://localhost:40000`으로 접속합니다. (포트 번호는 `application-local.yml`에 정의되어 있습니다.)

## 5. 참고 사항
- 초기 구동 시 `products.json`의 데이터를 Qdrant에 적재하는 과정에서 시간이 소요될 수 있습니다. (로그를 통해 진행 상황 확인 가능)
- `application-local.yml`에서 Ollama 및 Qdrant의 호스트/포트 정보를 수정할 수 있습니다.