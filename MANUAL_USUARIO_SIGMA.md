# MANUAL DE USUARIO SIGMA (POR ROL)

Version: 1.0  
Fecha: 2026-04-04  
Proyecto: SIGMA Backend (Spring Boot)

---

## 1. Proposito del manual

Este manual describe, de forma operativa y detallada, como usar SIGMA para cada rol institucional existente en el sistema.

Se construye con base en:

- endpoints REST implementados en controladores (`Users`, `academic`, `Modalities`, `documents`, `notifications`, `report`),
- reglas de seguridad (`SecurityConfig` + anotaciones `@PreAuthorize`),
- permisos/roles cargados en inicializacion (`DataInitializer`),
- flujo academico transversal de modalidad de grado y documentos.

Objetivo principal: permitir que cada actor ejecute su trabajo con claridad, reduciendo errores operativos y tiempos de soporte.

---

## 2. Alcance

Incluye:

- autenticacion, recuperacion de acceso y cierre de sesion;
- gestion academica del estudiante y su modalidad;
- flujo de revision/aprobacion por jefatura, comite, director y jurados;
- gestion administrativa (usuarios, roles, permisos, estructura academica);
- notificaciones y reporteria institucional;
- casos de uso detallados por rol (pasos, resultado esperado, errores comunes).

No incluye:

- instalacion tecnica de infraestructura;
- detalle de base de datos a nivel de DDL;
- interfaz frontend especifica (este manual es funcional y API-oriented).

---

## 3. Roles oficiales del sistema

Roles tecnicos detectados en configuracion de datos y autorizacion:

- `SUPERADMIN`
- `STUDENT`
- `PROGRAM_HEAD`
- `PROGRAM_CURRICULUM_COMMITTEE`
- `PROJECT_DIRECTOR`
- `EXAMINER`

Actores institucionales adicionales (no necesariamente rol tecnico directo en backend):

- Secretaria academica / apoyo administrativo
- Servicio tecnico (scheduler, reportes, notificaciones asincronas)

---

## 4. Reglas base de acceso y seguridad

- Todos los endpoints exigen JWT excepto `/auth/**`.
- El token se envia en header `Authorization: Bearer <token>`.
- El control de acceso combina:
  - rol (`hasRole('STUDENT')`),
  - autoridad/permiso (`hasAuthority('PERM_...')`).
- El cierre de sesion invalida token via blacklist.
- El sistema trabaja en modo stateless (sin sesion server-side).

### 4.1 Endpoints de autenticacion

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`
- `POST /auth/logout`

---

## 5. Flujo funcional transversal de SIGMA

1. El estudiante completa su perfil academico (manual o por PDF).
2. El estudiante inicia modalidad (individual o grupal) segun configuracion de programa.
3. El estudiante carga documentos requeridos.
4. Jefatura/comite/jurados revisan documentos y emiten decisiones.
5. Se aprueba/rechaza la modalidad en etapas segun estado del proceso.
6. Director propone sustentacion; jefatura agenda y asigna jurados.
7. Jurados realizan evaluacion final.
8. Se consolida resultado final y se habilita trazabilidad/reporteria.
9. Notificaciones informan cada cambio relevante.

---

## 6. Modulos y funciones para usuario final

### 6.1 Users

- registro, login, recuperacion de clave,
- gestion de usuarios y asignacion de roles,
- perfil academico del estudiante.

### 6.2 Academic

- facultades,
- programas academicos,
- configuracion programa-modalidad.

### 6.3 Modalities

- creacion/configuracion de modalidades,
- ejecucion del proceso de grado,
- documentos de modalidad,
- seminarios,
- cancelaciones,
- sustentaciones y evaluaciones.

### 6.4 Documents

- documentos requeridos por modalidad,
- plantillas descargables.

### 6.5 Notifications

- bandeja de notificaciones,
- detalle,
- marcar leido,
- conteo de no leidas.

### 6.6 Report

- reportes globales,
- historicos,
- comparativos,
- trazabilidad,
- calendario de sustentaciones,
- exportacion PDF.

---

## 7. Manual por rol

## 7.1 Rol `STUDENT` (Estudiante)

### Objetivo del rol

Gestionar su proceso de grado de punta a punta: perfil, inicio de modalidad, carga de documentos, seguimiento de estado, correcciones y solicitudes asociadas.

### Capacidades principales

- actualizar perfil academico,
- iniciar modalidad individual o grupal,
- cargar documentos,
- consultar historial de revisiones,
- solicitar cancelacion,
- consultar notificaciones,
- consultar resultado final de sustentacion.

### Endpoints clave

- `POST /students/profile`
- `POST /students/profile/from-academic-history`
- `GET /students/profile`
- `GET /students/modality/current`
- `GET /students/my-documents`
- `GET /students/documents/{studentDocumentId}/history`
- `POST /modalities/{modalityId}/start`
- `POST /modality-groups/{modalityId}/start-group`
- `POST /modalities/{studentModalityId}/documents/{requiredDocumentId}`
- `POST /students/{studentModalityId}/request-cancellation`
- `POST /students/cancellation-document/{studentModalityId}`
- `GET /notifications`

### Caso STU-01: Completar perfil academico manual

Precondiciones:

- Usuario autenticado con rol `STUDENT`.

Pasos:

1. Abrir formulario de perfil academico.
2. Enviar `POST /students/profile` con datos solicitados.
3. Confirmar respuesta exitosa.

Resultado esperado:

- Perfil persistido/actualizado para validaciones posteriores de modalidad.

Errores comunes:

- Datos incompletos o inconsistentes.
- Token vencido o ausente.

### Caso STU-02: Completar perfil desde historial PDF

Precondiciones:

- PDF academico legible.

Pasos:

1. Cargar archivo con `POST /students/profile/from-academic-history` (`multipart/form-data`).
2. Validar extraccion de datos.
3. Revisar perfil final en `GET /students/profile`.

Resultado esperado:

- Perfil completado automaticamente con informacion del historial.

Errores comunes:

- PDF no valido o no compatible.
- Campos no detectables por calidad del archivo.

### Caso STU-03: Iniciar modalidad de grado individual

Precondiciones:

- Perfil academico completo.
- Modalidad activa disponible para su programa.

Pasos:

1. Consultar modalidades activas.
2. Ejecutar `POST /modalities/{modalityId}/start`.
3. Consultar modalidad activa en `GET /students/modality/current`.

Resultado esperado:

- Se crea registro de modalidad estudiantil en estado inicial del flujo.

### Caso STU-04: Iniciar modalidad grupal e invitar companeros

Pasos:

1. `POST /modality-groups/{modalityId}/start-group`.
2. Consultar elegibles en `GET /modality-groups/eligible-students`.
3. Invitar con `POST /modality-groups/invite`.
4. El invitado acepta/rechaza:
   - `POST /modality-groups/invitations/{invitationId}/accept`
   - `POST /modality-groups/invitations/{invitationId}/reject`

Resultado esperado:

- Grupo consolidado para modalidad con miembros activos.

### Caso STU-05: Cargar documentos requeridos

Pasos:

1. Consultar documentos requeridos de la modalidad.
2. Subir archivo con `POST /modalities/{studentModalityId}/documents/{requiredDocumentId}`.
3. Verificar listado en `GET /students/my-documents`.

Resultado esperado:

- Documento almacenado y disponible para revision de autoridades.

Errores comunes:

- Tipo/tamano de archivo no permitido.
- Documento fuera de la etapa habilitada.

### Caso STU-06: Atender correcciones y reenviar documento

Pasos:

1. Revisar observaciones/historial:
   - `GET /students/documents/{studentDocumentId}/history`
2. Corregir archivo.
3. Reenviar con `POST /modalities/{studentModalityId}/documents/{documentId}/resubmit-correction`.

Resultado esperado:

- Documento reingresa al flujo de revision.

### Caso STU-07: Solicitar cancelacion de modalidad

Pasos:

1. Enviar `POST /students/{studentModalityId}/request-cancellation`.
2. Cargar soporte con `POST /students/cancellation-document/{studentModalityId}`.
3. Hacer seguimiento por notificaciones.

Resultado esperado:

- Solicitud pasa a evaluacion de director/comite segun reglas del proceso.

### Caso STU-08: Solicitar edicion de propuesta aprobada

Pasos:

1. Enviar `POST /modalities/documents/{studentDocumentId}/request-edit` con motivo.
2. Consultar estado en:
   - `GET /modalities/my-document-edit-requests`
   - `GET /modalities/{studentModalityId}/my-document-edit-requests`
3. Revisar detalle `GET /modalities/document-edit-requests/{editRequestId}`.

Resultado esperado:

- Solicitud queda en votacion de jurados (incluye desempate cuando aplique).

---

## 7.2 Rol `SUPERADMIN` (Administrador)

### Objetivo del rol

Administrar estructura, usuarios, roles/permisos y parametrizacion institucional completa del sistema.

### Capacidades principales

- crear/editar roles y permisos,
- asignar roles y activar/desactivar usuarios,
- registrar usuarios administrativos,
- asignar autoridades por programa (jefatura, director, comite, jurados),
- administrar facultades, programas, modalidades y documentos requeridos,
- consultar reportes ejecutivos.

### Endpoints clave

Administracion de seguridad/usuarios:

- `POST /admin/createRole`
- `PUT /admin/updateRole/{id}`
- `POST /admin/createPermission`
- `POST /admin/assignRole`
- `POST /admin/register-user`
- `GET /admin/getUsers`
- `PUT /admin/changeUserStatus/{userId}`

Asignacion de autoridades:

- `POST /admin/assign-program-head`
- `POST /admin/assign-project-director`
- `POST /admin/assign-committee-member`
- `POST /admin/assign-examiner`
- `POST /admin/examiner/assign-programs`
- `POST /admin/examiner/assign-program`
- `DELETE /admin/examiner/{userId}/program/{academicProgramId}`

Catalogos academicos y modalidades:

- `POST /faculties/create`, `PUT /faculties/update/{id}`
- `POST /academic-programs/create`, `PUT /academic-programs/update/{id}`
- `POST /modalities/create`, `PUT /modalities/update/{modalityId}`
- `POST /required-documents/create`, `PUT /required-documents/update/{documentId}`

### Caso ADM-01: Crear rol y permisos

Pasos:

1. Crear permiso (si no existe) con `POST /admin/createPermission`.
2. Crear rol con `POST /admin/createRole`.
3. Verificar con `GET /admin/getRoles` y `GET /admin/getPermissions`.

Resultado esperado:

- Nuevo rol utilizable en asignacion a usuarios.

### Caso ADM-02: Registrar usuario administrativo y asignar rol

Pasos:

1. Registrar con `POST /admin/register-user`.
2. Asignar rol con `POST /admin/assignRole`.
3. Validar listado con `GET /admin/getUsers`.

Resultado esperado:

- Usuario habilitado para operar segun rol asignado.

### Caso ADM-03: Configurar estructura academica base

Pasos:

1. Crear facultad.
2. Crear programa academico.
3. Asociar configuracion programa-modalidad (`/program-degree-modalities/create`).
4. Crear modalidad de grado y requisitos documentales.

Resultado esperado:

- Programa queda listo para que estudiantes inicien modalidades.

### Caso ADM-04: Asignar autoridades institucionales

Pasos:

1. Asignar jefe de programa.
2. Asignar comite curricular.
3. Asignar director(es) de proyecto.
4. Asignar jurados y programas asociados.

Resultado esperado:

- Cadena de aprobacion y evaluacion operativa.

---

## 7.3 Rol `PROGRAM_HEAD` (Jefatura de programa)

### Objetivo del rol

Coordinar, validar y aprobar el avance de modalidades de su programa; gestionar seminarios y decisiones academicas intermedias/finales.

### Capacidades principales

- revisar documentos de estudiantes,
- aprobar modalidad en etapas de jefatura,
- asignar/cambiar director de proyecto,
- gestionar propuestas de sustentacion,
- crear y administrar seminarios,
- consultar reportes.

### Endpoints clave

- `GET /modalities/students`
- `GET /modalities/students/{studentModalityId}`
- `GET /modalities/{studentModalityId}/documents`
- `PUT /modalities/documents/{studentDocumentId}/review`
- `POST /modalities/{studentModalityId}/approve-program-head`
- `POST /modalities/{studentModalityId}/assign-director/{directorId}`
- `PUT /modalities/{studentModalityId}/change-director`
- `GET /modalities/defense-proposals/pending`
- `POST /modalities/{studentModalityId}/defense-proposals/approve`
- `POST /modalities/{studentModalityId}/defense-proposals/reschedule`
- `POST /modalities/{studentModalityId}/examiners/assign`
- `POST /modalities/seminar/create`
- `GET /modalities/seminars`

### Caso JEF-01: Revisar y aprobar propuesta de modalidad

Pasos:

1. Listar modalidades de estudiantes por estado.
2. Abrir detalle de una modalidad.
3. Revisar documentos cargados.
4. Emitir revision/aprobacion documental.
5. Aprobar etapa de jefatura.

Resultado esperado:

- Modalidad avanza a siguiente actor del proceso.

### Caso JEF-02: Gestionar sustentacion

Pasos:

1. Revisar propuestas pendientes de sustentacion enviadas por director.
2. Aprobar o reprogramar fecha/hora.
3. Asignar jurados.

Resultado esperado:

- Sustentacion queda oficialmente programada y notificada.

### Caso JEF-03: Administrar seminarios

Pasos:

1. Crear seminario con cupos y fechas.
2. Iniciar/cancelar/cerrar inscripciones/completar seminario.
3. Monitorear listado de seminarios.

Resultado esperado:

- Oferta de seminarios disponible para estudiantes elegibles.

---

## 7.4 Rol `PROGRAM_CURRICULUM_COMMITTEE` (Comite curricular)

### Objetivo del rol

Ejecutar evaluacion colegiada: aprobaciones/rechazos finales, cierre por excepcion, cancelaciones y decisiones especiales (distinciones).

### Capacidades principales

- revisar modalidades de su programa,
- aprobar/rechazar modalidad en fase comite,
- resolver cancelaciones,
- cerrar modalidad por causal,
- revisar propuestas de distincion honorifica,
- consultar listados de estudiantes del programa.

### Endpoints clave

- `GET /modalities/students/committee`
- `GET /modalities/students/{studentModalityId}/committee`
- `POST /modalities/{studentModalityId}/approve-committee`
- `POST /modalities/{studentModalityId}/approve-final-by-committee`
- `POST /modalities/{studentModalityId}/reject-final-by-committee`
- `POST /modalities/{studentModalityId}/close-by-committee`
- `GET /modalities/cancellation-request`
- `POST /modalities/{studentModalityId}/cancellation/approve`
- `POST /modalities/{studentModalityId}/cancellation/reject`
- `GET /modalities/committee/program-students`
- `GET /modalities/committee/pending-distinction-proposals`
- `POST /modalities/{studentModalityId}/committee/accept-distinction`
- `POST /modalities/{studentModalityId}/committee/reject-distinction`

### Caso COM-01: Aprobar fase comite y notificar continuidad

Pasos:

1. Consultar cola de modalidades del comite.
2. Analizar detalle y soportes.
3. Aprobar por comite.

Resultado esperado:

- Modalidad continua a etapas posteriores del flujo.

### Caso COM-02: Resolver cancelacion

Pasos:

1. Consultar solicitudes de cancelacion pendientes.
2. Revisar documento soporte de cancelacion.
3. Aprobar o rechazar con motivacion.

Resultado esperado:

- Estado de modalidad actualizado con trazabilidad de decision.

### Caso COM-03: Decidir distincion honorifica

Pasos:

1. Revisar propuestas pendientes de distincion.
2. Analizar argumentos de jurados.
3. Aceptar o rechazar la distincion.

Resultado esperado:

- Resultado final consolidado (con o sin distincion).

---

## 7.5 Rol `PROJECT_DIRECTOR` (Director de proyecto)

### Objetivo del rol

Acompanamiento tecnico-academico del trabajo de grado hasta dejarlo listo para sustentacion.

### Capacidades principales

- consultar modalidades asignadas,
- revisar avance y documentos,
- proponer fecha de sustentacion,
- marcar modalidad lista para defensa,
- resolver (en su etapa) solicitudes de cancelacion.

### Endpoints clave

- `GET /modalities/students/director`
- `GET /modalities/students/{studentModalityId}/director`
- `POST /modalities/{studentModalityId}/propose-defense-director`
- `POST /modalities/{studentModalityId}/ready-for-defense`
- `POST /modalities/{studentModalityId}/cancellation/director/approve`
- `POST /modalities/{studentModalityId}/cancellation/director/reject`

### Caso DIR-01: Proponer sustentacion

Pasos:

1. Verificar que modalidad este lista.
2. Enviar propuesta de sustentacion con fecha/hora.
3. Esperar aprobacion/reprogramacion por jefatura.

Resultado esperado:

- Propuesta registrada y enviada a decision de jefatura.

### Caso DIR-02: Declarar modalidad lista para defensa

Pasos:

1. Validar cumplimiento de requisitos.
2. Ejecutar `POST /modalities/{studentModalityId}/ready-for-defense`.

Resultado esperado:

- Modalidad pasa a estado de preparacion de sustentacion.

### Caso DIR-03: Resolver cancelacion en etapa director

Pasos:

1. Revisar causal y soporte.
2. Aprobar o rechazar solicitud de cancelacion con razon.

Resultado esperado:

- Solicitud avanza/resuelve conforme reglas del proceso.

---

## 7.6 Rol `EXAMINER` (Jurado evaluador)

### Objetivo del rol

Evaluar tecnicamente documentos y sustentacion final, emitiendo decisiones trazables en cada etapa.

### Capacidades principales

- consultar modalidades asignadas,
- revisar documentos de propuesta/final,
- aprobar/rechazar en etapa de jurados,
- registrar evaluacion de sustentacion,
- resolver solicitudes de edicion de documentos aprobados,
- consultar calendario de defensas.

### Endpoints clave

- `GET /modalities/students/examiner`
- `GET /modalities/students/{studentModalityId}/examiner`
- `PUT /modalities/documents/{studentDocumentId}/review-examiner`
- `PUT /modalities/documents/{studentDocumentId}/review-examiner-final-document`
- `POST /modalities/{studentModalityId}/approve-examiners`
- `POST /modalities/{studentModalityId}/final-review-completed`
- `POST /modalities/{studentModalityId}/final-evaluation/register`
- `GET /modalities/examiner/defense-calendar`
- `GET /modalities/{studentModalityId}/document-edit-requests/pending`
- `POST /modalities/document-edit-requests/{editRequestId}/resolve`

### Caso JUR-01: Revisar documento final y emitir concepto

Pasos:

1. Abrir modalidades asignadas.
2. Consultar detalle documental.
3. Emitir revision (aprobacion/rechazo con observaciones).

Resultado esperado:

- Decision individual registrada en historial.

### Caso JUR-02: Registrar evaluacion de sustentacion

Pasos:

1. Consultar calendario de defensas.
2. Ejecutar evaluacion final con criterios definidos.
3. Registrar evaluacion en `final-evaluation/register`.

Resultado esperado:

- Calificacion final consolidada segun reglas de negocio.

### Caso JUR-03: Resolver solicitud de edicion de propuesta aprobada

Pasos:

1. Revisar solicitudes pendientes.
2. Votar aprobacion/rechazo con notas.
3. Validar resultado por consenso/desempate.

Resultado esperado:

- Solicitud queda aprobada o rechazada con trazabilidad de votos.

---

## 8. Funciones comunes a todos los roles autenticados

## 8.1 Notificaciones

- `GET /notifications`: bandeja del usuario.
- `GET /notifications/unread-count`: contador de no leidas.
- `GET /notifications/{notificationId}`: detalle.
- `PUT /notifications/{notificationId}/read`: marcar leida.

Buenas practicas:

- revisar notificaciones al iniciar jornada,
- marcar como leidas para mantener trazabilidad personal,
- usar detalle para identificar accion requerida y plazo.

## 8.2 Descarga de plantillas

- `GET /templates/{id}/download`

Uso recomendado:

- descargar siempre la version vigente antes de diligenciar documento.

---

## 9. Reportes institucionales (roles con `PERM_VIEW_REPORT`)

Principales reportes disponibles:

- global de modalidades (`/reports/global/modalities` + PDF),
- estudiantes por modalidad/semestre,
- directores por modalidad,
- modalidades por director,
- comparativo de modalidades,
- historico por modalidad,
- listado de estudiantes con filtros,
- modalidades completadas,
- trazabilidad por modalidad o por estudiante,
- calendario de sustentaciones.

Sugerencias de uso:

- comite: priorizar trazabilidad y calendario de defensas,
- jefatura: usar reportes por director y listados por estado,
- administracion: usar reportes globales y comparativos para planeacion.

---

## 10. Matriz rapida rol -> acciones

| Rol | Puede iniciar modalidad | Puede revisar documentos | Puede aprobar modalidad | Puede programar defensa | Puede evaluar defensa | Puede administrar usuarios |
|---|---|---|---|---|---|---|
| STUDENT | Si | No | No | No | No | No |
| PROJECT_DIRECTOR | No | Si (seguimiento funcional) | Parcial (etapa director) | Propone | No | No |
| EXAMINER | No | Si | Si (etapa jurados) | No | Si | No |
| PROGRAM_HEAD | No | Si | Si | Si | No | Parcial |
| PROGRAM_CURRICULUM_COMMITTEE | No | Si (decision colegiada) | Si (etapa comite/final) | No | No | Parcial |
| SUPERADMIN | No | Si | Si | Si | No | Si |

Nota: la autorizacion efectiva depende de permisos exactos (`PERM_*`) configurados al rol.

---

## 11. Errores frecuentes y resolucion

- `401 Unauthorized`: token ausente o invalido.
  - Accion: volver a iniciar sesion.
- `403 Forbidden`: rol/permisos insuficientes.
  - Accion: validar rol asignado y autoridad requerida.
- `404 Not Found`: recurso no existe o no pertenece al usuario.
  - Accion: verificar ID y alcance del recurso.
- `400 Bad Request`: datos incompletos o invalidos.
  - Accion: revisar payload y reglas de negocio.
- `500 Internal Server Error`: error inesperado.
  - Accion: registrar caso y escalar a soporte tecnico.

---

## 12. Buenas practicas operativas por rol

- Estudiante: subir documentos con nombres claros y versionados.
- Jefatura/comite: registrar observaciones completas y accionables.
- Director: no proponer defensa hasta validar estado documental final.
- Jurado: mantener consistencia entre observaciones y decision emitida.
- Administrador: usar filtros de usuarios por estado/rol para auditoria periodica.

---

## 13. Checklist de cierre por caso

Antes de cerrar un caso funcional, validar:

- autenticacion valida del usuario ejecutor,
- cumplimiento del permiso requerido,
- cambio de estado esperado en modalidad/documento,
- generacion de notificacion asociada,
- trazabilidad visible en historial o reportes.

---

## 14. Consideraciones finales

- Este manual cubre los roles tecnicos actualmente implementados en backend.
- Puede coexistir con un manual UI especifico del frontend institucional.
- Ante cambios en endpoints, permisos o estados del flujo, se debe versionar este manual.

Fin del documento.

