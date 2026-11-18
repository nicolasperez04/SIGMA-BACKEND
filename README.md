# 🎓 SIGMA — Sistema Interno de Gestión de Modalidades Académicas

**Backend** del sistema de gestión de modalidades de grado de la **Facultad de Ingeniería — Universidad Surcolombiana (USCO)**.

`Java 21` · `Spring Boot 3.5` · `Spring Security 6` · `MySQL 8` · `JWT` · `Maven`

Automatiza el ciclo de vida completo de las modalidades de grado (Proyecto de Grado, Práctica Profesional, Seminario, etc.): selección de modalidad y formación de grupos, carga y evaluación de documentos, asignación de jurados, sustentación, calificación con distinciones y expedición de certificados.

---

## Tabla de contenidos

- [Funcionalidades](#funcionalidades)
- [Stack tecnológico](#stack-tecnológico)
- [Modelo de dominio](#modelo-de-dominio)
- [Flujo de estados](#flujo-de-estados)
- [Configuración](#configuración)
- [Inicio rápido](#inicio-rápido)
- [API](#api)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Documentación](#documentación)
- [Pruebas](#pruebas)
- [Licencia](#licencia)

---

## Funcionalidades

### Autenticación y autorización

- Registro e inicio de sesión con **JWT** (access token).
- Roles: `SUPERADMIN`, `PROGRAM_HEAD`, `PROGRAM_CURRICULUM_COMMITTEE`, `STUDENT`, `PROJECT_DIRECTOR`, `EXAMINER`.
- Permisos granulares almacenados en base de datos, evaluados vía `@PreAuthorize`.
- Autoridades por programa académico (`ProgramAuthority`).
- Recuperación de contraseña por email y bloqueo de tokens (logout).
- Rate limiting en endpoints de autenticación.

### Gestión académica

- CRUD de **facultades** y **programas académicos**.
- Configuración de **modalidades disponibles por programa**, con requisitos de créditos y toggle de proceso de sustentación.

### Modalidades de grado

- Flujo completo de selección de modalidad (individual o grupal).
- **Formación de grupos**: el líder invita miembros, que aceptan o rechazan.
- Asignación de **director de proyecto**.
- Más de 40 estados de proceso que cubren revisión de jefatura, comité de currículo, jurados, sustentación y cierre.
- Historial de cambios de estado (auditoría).
- Gestión de seminarios: inscripción, capacidad, fechas y cancelación.

### Gestión documental

- Carga y reemplazo de documentos por el estudiante.
- Tipos de documento: `MANDATORY`, `SECONDARY` y `CANCELLATION`.
- **Flujo de revisión**: jefatura → comité de currículo → jurados.
- Evaluación de **propuestas** con rúbrica de 7 aspectos.
- Evaluación de **documentos finales** con rúbrica de hasta 11 aspectos.
- Revisión por jurados con votación individual y **desempate**.
- **Solicitudes de edición** de documentos aprobados con votación entre jurados.
- Historial de cambios de estado por documento.

### Sustentación y evaluación

- Programación de sustentación (fecha y lugar).
- Asignación de **2 jurados principales** + 1 **jurado de desempate**.
- Rúbrica de 5 criterios estándar (dominio, síntesis, argumentación, innovación, presentación).
- Rúbrica adicional para **emprendimiento** (5 criterios).
- Nota definitiva (0.0–5.0) según el Acuerdo 071/2023.
- Propuesta de **menciones**: Meritoria y Laureada, con revisión del comité de currículo.

### Certificados

- **Certificado de grado**: uno por modalidad, con número único y hash SHA-256.
- **Certificado de participación** para jurados.
- Generación en PDF con iTextPDF.

### Notificaciones

- Sistema **event-driven** con más de 20 tipos de eventos.
- Notificaciones en aplicación y por email (SMTP).
- Plantillas por tipo de notificación y rol destinatario.
- Reintento automático de envíos fallidos.

### Reportes

- 20 tipos de reportes institucionales (RF-45 a RF-61).
- Exportación a **PDF**, **Excel**, **CSV** y **JSON**.
- Reportes de modalidades activas y completadas, por director, por estado, cronograma de sustentaciones, estadísticas por programa y semestre, trazabilidad, entre otros.

---

## Stack tecnológico

| Componente | Versión |
|---|---|
| Java | 21 (con preview features) |
| Spring Boot | 3.5.8 |
| Spring Security | 6.x (JWT) |
| Spring Data JPA / Hibernate | 6.x |
| Spring Mail | — |
| MySQL | 8.x |
| JJWT | 0.12.6 |
| iTextPDF | 5.5.13.3 |
| Apache POI | 5.2.5 |
| Apache PDFBox | 2.0.30 |
| Tess4J (Tesseract OCR) | 5.8.0 |
| SpringDoc OpenAPI | 2.5.0 |
| Lombok | — |
| Maven | 3.9+ |

---

## Modelo de dominio

### Usuarios y seguridad

```
User ──┬── Role ── Permission
       ├── ProgramAuthority (User + AcademicProgram + ProgramRole)
       ├── PasswordResetToken
       └── StudentProfile (código, créditos aprobados, GPA, semestre)
```

### Estructura académica

```
Faculty ──┬── AcademicProgram ── ProgramDegreeModality ── DegreeModality
          └── DegreeModality
```

### Modalidad de grado

```
StudentModality ──┬── StudentModalityMember (grupo)
                  ├── ModalityInvitation
                  ├── DefenseExaminer (jurados)
                  ├── DefenseEvaluationCriteria (rúbrica)
                  ├── ExaminerCertificate
                  ├── AcademicCertificate
                  ├── Seminar
                  └── ModalityProcessStatusHistory
```

### Documentos

```
RequiredDocument ── StudentDocument ──┬── ProposalEvaluation
                                      ├── FinalDocumentEvaluation
                                      ├── ExaminerDocumentReview
                                      ├── DocumentEditRequest ── DocumentEditRequestVote
                                      └── StudentDocumentStatusHistory
```

---

## Flujo de estados

Estados principales de `ModalityProcessStatus`:

```
MODALITY_SELECTED
  → UNDER_REVIEW_PROGRAM_HEAD
  → READY_FOR_PROGRAM_CURRICULUM_COMMITTEE
  → UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE
  → PROPOSAL_APPROVED
  → PENDING_PROGRAM_HEAD_FINAL_REVIEW
  → DEFENSE_SCHEDULED
  → EXAMINERS_ASSIGNED
  → DEFENSE_COMPLETED
  → UNDER_EVALUATION_PRIMARY_EXAMINERS
  → EVALUATION_COMPLETED / DISAGREEMENT_REQUIRES_TIEBREAKER
  → GRADED_APPROVED / GRADED_FAILED
  → MODALITY_CLOSED
```

Existen además estados de corrección, cancelación, invitaciones y cierre anticipado que completan el total de más de 40 estados.

---

## Configuración

### Variables de entorno

La aplicación lee un archivo **`.env`** en la raíz del proyecto. Se carga mediante un `EnvLoader` personalizado **antes** de que Spring resuelva sus propiedades.

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | URL de conexión MySQL | `jdbc:mysql://localhost:3306/SIGMABD` |
| `DB_USERNAME` | Usuario MySQL | `root` |
| `DB_PASSWORD` | Contraseña MySQL | `root` |
| `JWT_SECRET` | Clave HMAC-SHA256 (64 hex) | `E54791C3...` |
| `MAIL_HOST` | Servidor SMTP | `smtp.gmail.com` |
| `MAIL_PORT` | Puerto SMTP | `587` |
| `MAIL_USERNAME` | Correo SMTP | `sigmausco@gmail.com` |
| `MAIL_PASSWORD` | Contraseña / app password | `xxxx` |
| `FRONTEND_URL` | Origen permitido por CORS | `http://localhost:5173` |
| `UPLOAD_DIR` | Directorio de archivos subidos | `./SIGMA-uploads/SIGMA-files` |

### Perfiles

| Perfil | Activo por defecto | `ddl-auto` | Swagger |
|---|---|---|---|
| `dev` | No | `update` | Habilitado |
| `prod` | **Sí** | `validate` | Deshabilitado |

### Seed de roles y permisos

`DataInitializer` corre con `@Profile("dev")` y crea de forma **idempotente** los permisos y los 6 roles base.

En `prod` el seed **no** se ejecuta: una base nueva requiere cargar roles y permisos manualmente antes del primer arranque, porque los `@PreAuthorize` de los endpoints dependen de ellos. Alternativas: arrancar una vez con `SPRING_PROFILES_ACTIVE=dev` contra la base en blanco, o insertarlos con un script SQL replicando `DataInitializer`.

---

## Inicio rápido

### Prerrequisitos

- Java 21 (JDK)
- MySQL 8.x
- Maven 3.9+ (o usar el wrapper `mvnw` incluido)

### Desarrollo local

```bash
# 1. Clonar el repositorio
git clone <repo-url>
cd SIGMA

# 2. Crear el archivo .env en la raíz con las variables de la tabla anterior
#    (DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, MAIL_*, FRONTEND_URL)

# 3. Ejecutar en modo desarrollo
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Compilar y ejecutar el artefacto:

```bash
./mvnw clean package -DskipTests
java --enable-preview -jar target/SIGMA-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
# Construir imagen
docker build -t sigma-backend .

# Ejecutar contenedor
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/SIGMABD \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=root \
  -e JWT_SECRET=... \
  sigma-backend
```

---

## API

| Recurso | URL |
|---|---|
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` |
| **OpenAPI JSON** | `http://localhost:8080/v3/api-docs` |

> Swagger está habilitado en `dev` y deshabilitado en `prod`.

### Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/auth/register` | Registrar usuario |
| `POST` | `/auth/login` | Iniciar sesión (JWT) |
| `POST` | `/auth/forgot-password` | Solicitar recuperación de contraseña |
| `POST` | `/auth/reset-password` | Restablecer contraseña |
| `GET` | `/admin/users` | Listar usuarios (paginado) |
| `POST` | `/admin/registerUser` | Registrar usuario (admin) |
| `GET` | `/faculties/all` | Listar facultades |
| `GET` | `/academic-programs/all` | Listar programas académicos |
| `GET` | `/modalities/{id}` | Detalle de modalidad |
| `GET` | `/students/profile` | Perfil de estudiante |
| `GET` | `/notifications` | Mis notificaciones |
| `GET` | `/reports/{reportType}` | Reportes institucionales |
| `GET` | `/certificate/{studentModalityId}` | Descargar certificado |

---

## Estructura del proyecto

```
SIGMA/
├── src/main/java/com/SIGMA/USCO/
│   ├── SigmaApplication.java
│   ├── config/                 # CORS, Swagger, async, DataInitializer
│   ├── security/               # JWT, SecurityConfig, filtros, rate limit
│   ├── common/                 # Transversal y sin dependencias de negocio
│   │   ├── exception/          # Excepciones de dominio + GlobalExceptionHandler
│   │   ├── security/           # Constantes de roles y permisos
│   │   ├── validation/         # FileValidator
│   │   └── web/                # ApiResponse, PaginatedResponse
│   ├── shared/util/            # Utilidades compartidas (traducciones, políticas de acceso)
│   ├── Users/                  # Autenticación, usuarios, roles, permisos
│   ├── academic/               # Facultades, programas, perfiles de estudiante
│   ├── Modalities/             # Modalidades, grupos, defensa, seminarios, certificados
│   ├── documents/              # Documentos, evaluaciones, solicitudes de edición
│   ├── notifications/          # Notificaciones, eventos, listeners, dispatcher
│   └── report/                 # Reportes institucionales y generadores PDF/Excel
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   └── application-prod.properties
├── documentation/              # Documentación institucional (ver sección Documentación)
├── .github/workflows/          # CI
├── Dockerfile
├── pom.xml
└── mvnw / mvnw.cmd
```

Cada módulo de negocio sigue la misma estructura interna: `controller/`, `service/`, `repository/`, `entity/`, `dto/`.

---

## Documentación

La documentación institucional completa —técnica, manuales de usuario, arquitectura por módulo, casos de uso, modelo entidad-relación y material de sustentación— vive en la carpeta **[`documentation/`](documentation/)**. Su índice está en **[`documentation/README.md`](documentation/README.md)**.

| Documento | Formatos |
|---|---|
| Documentación técnica | [PDF](documentation/DOCUMENTACI%C3%93N%20TECNICA.pdf) · [Word](documentation/DOCUMENTACI%C3%93N%20TECNICA.docx) |
| Manual de usuarios | [PDF](documentation/MANUAL%20DE%20USUARIOS.pdf) · [Word](documentation/MANUAL%20DE%20USUARIOS.docx) |
| Guion de sustentación | [PDF](documentation/GUI%C3%93N%20SUSTENTACI%C3%93N%20SIGMA.pdf) · [Word](documentation/GUI%C3%93N%20SUSTENTACI%C3%93N%20SIGMA.docx) |
| Certificado SIGMA | [Word](documentation/CERTIFICADO%20SIGMA.docx) |
| Arquitectura por módulo (9 módulos, con diagramas de clases) | [Abrir](documentation/ARQUITECTURA%20MODULOS/) |
| Casos de uso por rol | [Abrir](documentation/CASOS%20DE%20USO/) |
| Modelo entidad-relación y diagrama de clases general | [Abrir](documentation/ENTIDAD-RELACI%C3%93N/) |
| Material de sustentación (dominios, secuencias, casos de uso) | [Abrir](documentation/SUSTENTACI%C3%93N/) |

---

## Pruebas

```bash
./mvnw test
```

La suite corre sobre H2 en modo MySQL y cubre los flujos críticos de modalidades, documentos, notificaciones, seguridad y reportes.

---

## Licencia

Este proyecto es propiedad de la **Facultad de Ingeniería — Universidad Surcolombiana (USCO)**.

Desarrollado como proyecto de grado para la gestión interna de modalidades académicas.
