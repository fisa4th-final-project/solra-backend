# Solra-backend

## 1. 프로젝트 개요

Solra-Backend는 멀티 Kubernetes 클러스터를 동적으로 관리하기 위한 Java/Spring Boot 기반 백엔드 API입니다.  
주요 기능

- 여러 클러스터(온프레 K8s, AWS EKS) 연결 및 동적 클라이언트 생성
    
- 네임스페이스/리소스(CRUD) 조회·생성·수정·삭제
    
- ServiceAccount 토큰을 활용한 RBAC 기반 네임스페이스 필터링
    
- Deployment, Service 리소스의 동적 패치(PATCH) 지원
    
- Redis 세션 관리, MySQL 기반 데이터 저장소
    

---

## 2. 파일 구조 (간략화된 형태)

```
solra-backend/
└──	src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── fisa
    │   │           └── solra
    │   │               ├── SolraApplication.java
    │   │               ├── domain
    │   │               │   ├── cluster
    │   │               │   │   ├── controller
    │   │               │   │   │   └── ClusterController.java
    │   │               │   │   ├── dto
    │   │               │   │   │   ├── ClusterRequestDto.java
    │   │               │   │   │   └── ClusterResponseDto.java
    │   │               │   │   ├── entity
    │   │               │   │   │   └── Cluster.java
    │   │               │   │   ├── repository
    │   │               │   │   │   └── ClusterRepository.java
    │   │               │   │   └── service
    │   │               │   │       └── ClusterService.java
    │   │               │   ├── department
    │   │               │   │   ├── controller
    │   │               │   │   │   └── DepartmentController.java
    │   │               │   │   ├── dto
    │   │               │   │   │   ├── DepartmentRequestDto.java
    │   │               │   │   │   └── DepartmentResponseDto.java
    │   │               │   │   ├── entity
    │   │               │   │   │   └── Department.java
    │   │               │   │   ├── repository
    │   │               │   │   │   └── DepartmentRepository.java
    │   │               │   │   └── service
    │   │               │   │       └── DepartmentService.java
    │   │               │   ├── deployment
    │   │               │   │   ├── controller
    │   │               │   │   │   └── DeploymentController.java
    │   │               │   │   ├── dto
    │   │               │   │   │   ├── DeploymentCreateRequestDto.java
    │   │               │   │   │   ├── DeploymentCreateResponseDto.java
    │   │               │   │   │   ├── DeploymentRequestDto.java
    │   │               │   │   │   └── DeploymentResponseDto.java
    │   │               │   │   └── service
    │   │               │   │       └── DeploymentService.java
    │   │               │   ├── namespace
    │   │               │   │   ├── controller
    │   │               │   │   │   └── NamespaceController.java
    │   │               │   │   ├── dto
    │   │               │   │   │   ├── NamespaceRequestDto.java
    │   │               │   │   │   └── NamespaceResponseDto.java
    │   │               │   │   └── service
    │   │               │   │       └── NamespaceService.java
    │   │               └── global
    │   │                   ├── HealthCheckController.java
    │   │                   ├── aop
    │   │                   │   └── LoggingAspect.java
    │   │                   ├── auth
    │   │                   │   └── AuthUtil.java
    │   │                   ├── config
    │   │                   │   ├── CacheConfig.java
    │   │                   │   ├── Fabric8K8sConfig.java
    │   │                   │   ├── KubernetesClientProvider.java
    │   │                   │   ├── RedisHttpSessionConfig.java
    │   │                   │   └── SecurityConfig.java
    │   │                   ├── exception
    │   │                   │   ├── BusinessException.java
    │   │                   │   ├── ErrorCode.java
    │   │                   │   └── GlobalExceptionHandler.java
    │   │                   ├── jwt
    │   │                   │   └── JwtTokenProvider.java
    │   │                   ├── response
    │   │                   │   └── ApiResponse.java
    │   │                   ├── security
    │   │                   │   ├── JwtSessionAuthenticationFilter.java
    │   │                   │   └── UserPrincipal.java
    │   │                   └── util
    │   │                       └── SecurityUtil.java
    │   └── resources
    │       ├── application.yml
    │       ├── logback-spring.xml
    │       └── static
    │           └── favicon.ico
    └── test
        └── java
            └── com
                └── fisa
                    └── solra
                        ├── SolraApplicationTests.java
                        └── domain
                            ├── cluster
                            │   └── service
                            │       └── ClusterServiceTest.java
                            ├── department
                            │   └── service
                            │       └── DepartmentServiceTest.java
                            ├── deployment
                            │   └── service
                            │       └── DeploymentServiceTest.java
                            └── namespace
                                └── service
                                    └── NamespaceServiceTest.java
                            

```

---

## 3. 사용 기술 및 라이브러리

- **언어 및 프레임워크**
    
    - Java 17
        
    - Spring Boot (Web, Security, Cache)
        
    - Spring Data JPA (MySQL 연동)
        
    - Spring Session (Redis 기반 세션 스토어)
        
    - Lombok (Getter/Setter, Builder, RequiredArgsConstructor 등)
        
    - jjwt (JWT 기반 인증/인가, application.yml에서 `jwt.secret`, `jwt.expiration` 사용)
        
- **데이터베이스**
    
    - MySQL (JDBC Driver: `com.mysql.cj.jdbc.Driver`)
        
    - Redis (세션 관리)
        
- **쿠버네티스 클라이언트**
    
    - Fabric8 Kubernetes Client (v5+)
        
        - `io.fabric8.kubernetes.client.KubernetesClient`
            
        - `DeploymentBuilder`, `ServiceBuilder`, `NamespaceBuilder` 등 사용
            
- **인증/인가**
    
    - Spring Security (인증 컨텍스트에서 `Authentication.getAuthorities()`로 권한 체크)
        
    - ServiceAccount 토큰 (SA) + ClusterRoleBinding (`cluster-admin`)
        
    - RBAC 기반 네임스페이스 필터링: `ROLE_ROOT` 계정만 기본 네임스페이스 접근 허용, 그 외는 `EXCLUDED_NAMESPACES` 차단
        
- **빌드 도구**
    
    - Gradle
        
- **배포 환경**
    
    - AWS EKS (Private Subnet)
        
    - Public Bastion에 WireGuard VPN을 통한 API 서버 접근
        
    - Terraform/Ansible로 인프라 코드(IaC) 관리 (별도 모듈)
        
    - CI/CD: Jenkins + ArgoCD (배포 자동화)
        

---

## 4. 주요 기능 및 구현 시 유의사항

### 4.1. KubernetesClientProvider

- **역할**: 클러스터 ID를 인자로 받아, DB에 저장된 CA/SA/API 서버 정보를 기반으로 Fabric8 `KubernetesClient` 인스턴스를 생성하고, `@Cacheable("k8sClient")` 로 캐시
    
- **예외 처리**: 연결 오류 발생 시 `BusinessException(ErrorCode.CLUSTER_CONNECTION_FAILED)` 발생
    
- **Cache Evict**: 클러스터 설정 변경 시 `evictClient(clusterId)` 호출로 캐시 무효화
    

### 4.2. NamespaceService (네임스페이스 권한 분리)

- **EXCLUDED_NAMESPACES**: 기본 시스템 네임스페이스(`default`, `kube-node-lease`, `kube-public`, `kube-system`, `cilium-system`)
    
- **isRootUser()**: Spring Security `Authentication` 에서 `ROLE_ROOT` 여부 확인
    
    - ROOT 권한일 때는 전체 네임스페이스 반환
        
    - 그 외 권한일 때는 `stream().filter(ns -> !EXCLUDED_NAMESPACES.contains(name))` 필터링
        
- **단일 조회/생성/수정/삭제**: ROOT만 가능, 그 외는 `ErrorCode.NAMESPACE_FORBIDDEN` 예외
    

### 4.3. DeploymentService (Deployment CRUD + Patch)

- **getDeployments**, **getDeployment**, **createDeployment**, **deleteDeployment**: 표준 Fabric8 코드
    
- **updateDeployment** (`edit()` 방식)
    
    - `dto.getReplicas()` 가 있으면 `d.getSpec().setReplicas(...)`
        
    - `dto.getContainer().getImage()`, `dto.getContainer().getPort()` 가 있으면 PodTemplate 내 컨테이너 객체(`.getContainers().get(0)`) 수정
        
    - **컨테이너 이름 변경 불가**(immutable) → 요청 시 DTO에 `name` 포함되더라도 무시
        
- **DeploymentResponseDto**:
    
    - 필드: `name`, `replicas`, `readyReplicas`, `selector`, `images (List<String>)`, `containerName`, `containerPort`
        
    - `from(Deployment d)` 에서 첫 번째 컨테이너 정보만 읽어와 응답
        

### 4.4. ServiceService (Service CRUD + 다중 포트)

- **DTO 구조**
    
    - `ServiceRequestDto`: `name`, `type` (ClusterIP, NodePort 등), `selector(Map<String,String>)`, `ports(List<Port>)`
        
    - `Port`: `name (필수)`, `port(int)`, `targetPort(int)`, `protocol(String)`, `nodePort(Integer)`
        
    - `ServiceResponseDto`: `name`, `type`, `clusterIP`, `selector`, `ports(List<PortResponse>)` → `PortResponse` 에는 위와 동일한 필드 포함
        
- **createService** (`ServiceBuilder`)
    
    - `withPorts(dto.getPorts().stream().map(...) .collect(Collectors.toList()))`
        
    - 각 포트별 반드시 `withName(p.getName())` 콜, 안 그러면 `422: spec.ports[x].name: Required value` 오류
        
- **updateService** (`edit()` 방식)
    
    - nodePort 충돌 검사(`.getSpec().getPorts().stream().map(p->p.getNodePort()) …`)
        
    - `svc.getSpec().setPorts(dto.getPorts().stream().map(...) .collect(Collectors.toList()))` 로 기존 리스트 전체 교체
        
    - 나머지 `Selector`, `Type` 동기화
        

### 4.5. RBAC & ServiceAccount 토큰

- `solra-admin` SA 생성
    
    - `kubectl create namespace solra-access`
        
    - `kubectl create sa solra-admin -n solra-access`
        
    - `kubectl create clusterrolebinding solra-admin-binding --clusterrole=cluster-admin --serviceaccount=solra-access:solra-admin`
        
- **레거시 Secret 방식 (solra-admin-token-secret.yaml)** 를 통한 토큰 발급
    
    - Secret 매니페스트 적용 → 컨트롤러 자동 생성된 `solra-admin-token` Secret 확인 → 토큰(base64 디코딩) 추출
        
- Fabric8 `Config` 설정 시 `withMasterUrl(...)`, `withCaCertFile(...)`, `withOauthToken(...)`만 주입하면 로컬 클라이언트에서 API 서버 연결 가능
    

---

## 5. 중요한 점 요약

1. **컨테이너 이름 변경 불가**
    
    - Kubernetes API 상 `containers[].name` 은 immutable. 변경하려면 Deployment 자체를 재생성해야 함.
        
2. **ServicePort.name 필수**
    
    - 여러 포트 설정 시 `name`을 빠뜨리면 422 오류(`Required value`) 발생.
        
3. **RBAC 필터링**
    
    - `ROLE_ROOT` 계정만 모든 네임스페이스 접근 가능, 그 외는 `EXCLUDED_NAMESPACES` 차단
        
4. **SA 토큰 + CA**
    
    - Fabric8 Client 초기화에 필수 (API 서버 엔드포인트 + CA.crt + Bearer token)
        
5. **캐시 설정**
    
    - `@Cacheable("k8sClient")` 적용 시, 클러스터 메타 변경 시 반드시 `@CacheEvict` 호출
        
