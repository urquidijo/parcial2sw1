package com.workflow.config;

import com.workflow.model.*;
import com.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class MongoBootstrapConfig {

    private final UserRepository                  userRepository;
    private final PasswordEncoder                 passwordEncoder;
    private final CompanyRepository               companyRepository;
    private final DepartmentRepository            departmentRepository;
    private final JobRoleRepository               jobRoleRepository;
    private final WorkflowRepository              workflowRepository;
    private final WorkflowNodoRepository          workflowNodoRepository;
    private final WorkflowTransitionRepository    workflowTransitionRepository;
    private final FormDefinitionRepository        formDefinitionRepository;
    private final TramiteRepository               tramiteRepository;
    private final HistorialTramiteRepository      historialTramiteRepository;
    private final MongoTemplate                   mongoTemplate;

    @Bean
    public ApplicationRunner seedData() {
        return args -> {
            if (userRepository.existsByEmail("josueurquidicarrillo@gmail.com")) return;

            // ── 1. SUPERADMIN ────────────────────────────────────────────────
            User josue = saveUser("Josue Urquidi", "josueurquidicarrillo@gmail.com", "123456", User.Role.SUPERADMIN, null, null, null);
            if (!userRepository.existsByEmail("julio@gmail.com"))
                saveUser("Julio Avila", "julio@gmail.com", "julioavila", User.Role.SUPERADMIN, null, null, null);

            // ── 2. COMPANY ───────────────────────────────────────────────────
            Company company = new Company();
            company.setName("FlowOS Bolivia");
            company = companyRepository.save(company);
            String cid = company.getId();

            // ── 3. DEPARTMENTS ───────────────────────────────────────────────
            Department dRRHH  = saveDept("Recursos Humanos", cid);
            Department dTech  = saveDept("Tecnología",       cid);
            Department dFin   = saveDept("Finanzas",         cid);
            Department dOps   = saveDept("Operaciones",      cid);

            // ── 4. JOB ROLES ─────────────────────────────────────────────────
            JobRole rAnalistaRR = saveRole("Analista RRHH",       cid, dRRHH.getId());
            JobRole rJefeRR     = saveRole("Jefe de RRHH",        cid, dRRHH.getId());
            JobRole rDev        = saveRole("Desarrollador",        cid, dTech.getId());
            JobRole rTechLead   = saveRole("Tech Lead",            cid, dTech.getId());
            JobRole rQA         = saveRole("QA Engineer",          cid, dTech.getId());
            JobRole rAnalFin    = saveRole("Analista Financiero",  cid, dFin.getId());
            JobRole rContador   = saveRole("Contador",             cid, dFin.getId());
            JobRole rCoordOps   = saveRole("Coordinador",          cid, dOps.getId());
            JobRole rSuperOps   = saveRole("Supervisor",           cid, dOps.getId());

            // ── 5. USERS ─────────────────────────────────────────────────────
            User admin   = saveUser("Admin FlowOS",   "admin@flowos.com",   "admin123",   User.Role.ADMIN,           cid, null,           null);
            User ana     = saveUser("Ana García",     "ana@flowos.com",     "ana123",     User.Role.ATENCION_CLIENTE, cid, dRRHH.getId(),  rAnalistaRR.getId());
            User roberto = saveUser("Roberto Mamani", "roberto@flowos.com", "roberto123", User.Role.ATENCION_CLIENTE, cid, dRRHH.getId(),  rJefeRR.getId());
            User carlos  = saveUser("Carlos Ruiz",    "carlos@flowos.com",  "carlos123",  User.Role.VALIDADOR,       cid, dTech.getId(),   rDev.getId());
            User lucia   = saveUser("Lucía Torres",   "lucia@flowos.com",   "lucia123",   User.Role.TECNICO,         cid, dTech.getId(),   rQA.getId());
            User pedro   = saveUser("Pedro Quispe",   "pedro@flowos.com",   "pedro123",   User.Role.VALIDADOR,       cid, dFin.getId(),    rContador.getId());
            User maria   = saveUser("María López",    "maria@flowos.com",   "maria123",   User.Role.TECNICO,         cid, dFin.getId(),    rAnalFin.getId());
            User sofia   = saveUser("Sofía Condori",  "sofia@flowos.com",   "sofia123",   User.Role.ATENCION_CLIENTE, cid, dOps.getId(),  rCoordOps.getId());
            User luis    = saveUser("Luis Chávez",    "luis@flowos.com",    "luis123",    User.Role.CLIENTE,         cid, null,            null);

            // ── 6. WORKFLOW 1: Solicitud de Vacaciones ───────────────────────
            Workflow wf1 = new Workflow();
            wf1.setName("Solicitud de Vacaciones");
            wf1.setDescription("Proceso para gestionar y aprobar solicitudes de vacaciones del personal.");
            wf1.setCompanyId(cid);
            wf1 = workflowRepository.save(wf1);
            String w1 = wf1.getId();

            WorkflowNodo v0 = saveNodo(w1, "Inicio",               "inicio",   0, null,          null,               null,                    false, 0,    100.0, 200.0, null,      null);
            WorkflowNodo v1 = saveNodo(w1, "Registro Solicitud",   "proceso",  1, dRRHH.getId(), rAnalistaRR.getId(), User.Role.ATENCION_CLIENTE, true, 60,   320.0, 200.0, null,      null);
            WorkflowNodo v2 = saveNodo(w1, "Revisión RRHH",        "proceso",  2, dRRHH.getId(), rJefeRR.getId(),    User.Role.ATENCION_CLIENTE, false, 120, 540.0, 200.0, null,      null);
            WorkflowNodo v3 = saveNodo(w1, "¿Aprobado?",           "decision", 3, null,          null,               null,                    false, 0,    760.0, 200.0, "Aprobar", "Rechazar");
            WorkflowNodo v4 = saveNodo(w1, "Notificar Aprobación", "proceso",  4, dRRHH.getId(), rAnalistaRR.getId(), User.Role.ATENCION_CLIENTE, false, 30,  980.0,  80.0, null,      null);
            WorkflowNodo v5 = saveNodo(w1, "Notificar Rechazo",    "proceso",  5, dRRHH.getId(), rAnalistaRR.getId(), User.Role.ATENCION_CLIENTE, false, 30,  980.0, 340.0, null,      null);
            WorkflowNodo v6 = saveNodo(w1, "Fin",                  "fin",      6, null,          null,               null,                    false, 0,   1200.0, 200.0, null,      null);

            workflowTransitionRepository.saveAll(List.of(
                mkTrans(w1, v0.getId(), v1.getId(), null),
                mkTrans(w1, v1.getId(), v2.getId(), null),
                mkTrans(w1, v2.getId(), v3.getId(), null),
                mkTrans(w1, v3.getId(), v4.getId(), "Aprobar"),
                mkTrans(w1, v3.getId(), v5.getId(), "Rechazar"),
                mkTrans(w1, v4.getId(), v6.getId(), null),
                mkTrans(w1, v5.getId(), v6.getId(), null)
            ));

            saveForm(v1.getId(), "Formulario de Vacaciones", List.of(
                mkField("f1", "Motivo de la solicitud", FormDefinition.FieldType.TEXT,   true,  0),
                mkField("f2", "Fecha de inicio",        FormDefinition.FieldType.DATE,   true,  1),
                mkField("f3", "Fecha de retorno",       FormDefinition.FieldType.DATE,   true,  2),
                mkField("f4", "Días solicitados",       FormDefinition.FieldType.NUMBER, true,  3),
                mkField("f5", "Observaciones",          FormDefinition.FieldType.TEXT,   false, 4)
            ));

            // ── TRAMITES WORKFLOW 1 — COMPLETADO (8) ─────────────────────────
            Map<String,Object> fVac = Map.of("Motivo de la solicitud", "Vacaciones anuales", "Fecha de inicio", "2024-01-10", "Fecha de retorno", "2024-01-24", "Días solicitados", 14);

            completedVac(w1, v0,v1,v2,v3,v4,v6, "VAC-001", "Vacaciones Enero · Ana García",     ana.getId(),    roberto.getId(), fVac, 45, 5,  60, 120, 20, 30);
            completedVac(w1, v0,v1,v2,v3,v4,v6, "VAC-002", "Vacaciones Febrero · Carlos Ruiz",  carlos.getId(), roberto.getId(), Map.of("Motivo de la solicitud","Carnaval","Fecha de inicio","2024-02-12","Fecha de retorno","2024-02-17","Días solicitados",5), 38, 3, 55, 110, 15, 25);
            completedVac(w1, v0,v1,v2,v3,v4,v6, "VAC-003", "Vacaciones Marzo · Lucía Torres",   lucia.getId(),  ana.getId(),    Map.of("Motivo de la solicitud","Descanso médico","Fecha de inicio","2024-03-04","Fecha de retorno","2024-03-11","Días solicitados",7), 30, 4, 65, 130, 25, 35);
            completedVac(w1, v0,v1,v2,v3,v4,v6, "VAC-004", "Vacaciones Abril · Pedro Quispe",   pedro.getId(),  roberto.getId(), Map.of("Motivo de la solicitud","Semana Santa","Fecha de inicio","2024-03-28","Fecha de retorno","2024-04-06","Días solicitados",9), 25, 6, 70, 140, 30, 40);
            completedVac(w1, v0,v1,v2,v3,v4,v6, "VAC-005", "Vacaciones Mayo · Sofía Condori",   sofia.getId(),  ana.getId(),    Map.of("Motivo de la solicitud","Vacaciones","Fecha de inicio","2024-05-01","Fecha de retorno","2024-05-08","Días solicitados",7), 20, 3, 50, 100, 20, 28);
            completedVac(w1, v0,v1,v2,v3,v4,v6, "VAC-006", "Vacaciones Junio · Luis Chávez",    luis.getId(),   roberto.getId(), Map.of("Motivo de la solicitud","Cumpleaños","Fecha de inicio","2024-06-15","Fecha de retorno","2024-06-20","Días solicitados",5), 15, 7, 80, 160, 35, 45);
            completedVac(w1, v0,v1,v2,v3,v4,v6, "VAC-007", "Vacaciones Julio · María López",    maria.getId(),  ana.getId(),    Map.of("Motivo de la solicitud","Viaje familiar","Fecha de inicio","2024-07-01","Fecha de retorno","2024-07-14","Días solicitados",13), 12, 4, 58, 115, 22, 32);
            completedVac(w1, v0,v1,v2,v3,v4,v6, "VAC-008", "Vacaciones Express · Carlos Ruiz",  carlos.getId(), roberto.getId(), Map.of("Motivo de la solicitud","Urgencia","Fecha de inicio","2024-07-22","Fecha de retorno","2024-07-25","Días solicitados",3), 8,  2, 40,  90, 15, 20);

            // ── TRAMITES WORKFLOW 1 — RECHAZADO (3) ──────────────────────────
            rejectedVac(w1, v0,v1,v2,v3,v5,v6, "VAC-009", "Rechazo Agosto · Sofía Condori",    sofia.getId(),  roberto.getId(), Map.of("Motivo de la solicitud","Fin de mes","Fecha de inicio","2024-08-28","Fecha de retorno","2024-09-05","Días solicitados",8),  18, 5, 60, 150, 25, 30);
            rejectedVac(w1, v0,v1,v2,v3,v5,v6, "VAC-010", "Rechazo Junio · Luis Chávez",       luis.getId(),   roberto.getId(), Map.of("Motivo de la solicitud","Viaje","Fecha de inicio","2024-06-01","Fecha de retorno","2024-06-15","Días solicitados",14), 28, 4, 55, 120, 20, 28);
            rejectedVac(w1, v0,v1,v2,v3,v5,v6, "VAC-011", "Rechazo Mayo · Lucía Torres",       lucia.getId(),  ana.getId(),    Map.of("Motivo de la solicitud","Examen","Fecha de inicio","2024-05-15","Fecha de retorno","2024-05-22","Días solicitados",7),  10, 3, 52, 105, 18, 25);

            // ── TRAMITES WORKFLOW 1 — ACTIVOS ────────────────────────────────
            activeTramite(w1, "VAC-012", "Vacaciones Pendiente · María López",   maria.getId(),  roberto.getId(), v2.getId(), Tramite.Status.EN_PROGRESO, Map.of("Motivo de la solicitud","Congreso","Fecha de inicio","2024-09-10","Fecha de retorno","2024-09-17","Días solicitados",7), 3);
            activeTramite(w1, "VAC-013", "Vacaciones Nueva · Pedro Quispe",      pedro.getId(),  ana.getId(),     v1.getId(), Tramite.Status.EN_PROGRESO, Map.of("Motivo de la solicitud","Vacaciones","Fecha de inicio","2024-09-20","Fecha de retorno","2024-09-28","Días solicitados",8), 2);
            activeTramite(w1, "VAC-014", "Vacaciones Reciente · Sofía Condori",  sofia.getId(),  roberto.getId(), v1.getId(), Tramite.Status.PENDIENTE,   Map.of(), 1);
            activeTramite(w1, "VAC-015", "Vacaciones Urgente · Carlos Ruiz",     carlos.getId(), ana.getId(),     v2.getId(), Tramite.Status.EN_PROGRESO, Map.of("Motivo de la solicitud","Emergencia","Fecha de inicio","2024-09-05","Fecha de retorno","2024-09-10","Días solicitados",5), 5);

            // ── 7. WORKFLOW 2: Incorporación de Empleado ─────────────────────
            Workflow wf2 = new Workflow();
            wf2.setName("Incorporación de Empleado");
            wf2.setDescription("Proceso de onboarding para nuevos empleados de la organización.");
            wf2.setCompanyId(cid);
            wf2 = workflowRepository.save(wf2);
            String w2 = wf2.getId();

            WorkflowNodo i0 = saveNodo(w2, "Inicio",              "inicio",  0, null,          null,               null,                    false, 0,   100.0, 250.0, null, null);
            WorkflowNodo i1 = saveNodo(w2, "Registro de Datos",   "proceso", 1, dRRHH.getId(), rAnalistaRR.getId(), User.Role.ATENCION_CLIENTE, true, 90,  320.0, 250.0, null, null);
            WorkflowNodo i2 = saveNodo(w2, "Configurar Accesos",  "proceso", 2, dTech.getId(), rDev.getId(),       User.Role.VALIDADOR,       false, 60,  540.0, 250.0, null, null);
            WorkflowNodo i3 = saveNodo(w2, "Revisión Final RRHH", "proceso", 3, dRRHH.getId(), rJefeRR.getId(),    User.Role.ATENCION_CLIENTE, false, 45,  760.0, 250.0, null, null);
            WorkflowNodo i4 = saveNodo(w2, "Fin",                 "fin",     4, null,          null,               null,                    false, 0,   980.0, 250.0, null, null);

            workflowTransitionRepository.saveAll(List.of(
                mkTrans(w2, i0.getId(), i1.getId(), null),
                mkTrans(w2, i1.getId(), i2.getId(), null),
                mkTrans(w2, i2.getId(), i3.getId(), null),
                mkTrans(w2, i3.getId(), i4.getId(), null)
            ));

            saveForm(i1.getId(), "Datos del Nuevo Empleado", List.of(
                mkField("g1", "Nombre completo",   FormDefinition.FieldType.TEXT,     true,  0),
                mkField("g2", "Email corporativo", FormDefinition.FieldType.EMAIL,    true,  1),
                mkField("g3", "Cargo asignado",    FormDefinition.FieldType.TEXT,     true,  2),
                mkField("g4", "Departamento",      FormDefinition.FieldType.TEXT,     true,  3),
                mkField("g5", "Fecha de inicio",   FormDefinition.FieldType.DATE,     true,  4),
                mkField("g6", "Equipos entregados",FormDefinition.FieldType.CHECKBOX, false, 5)
            ));

            // ── TRAMITES WORKFLOW 2 — COMPLETADO (4) ─────────────────────────
            completedInc(w2, i0,i1,i2,i3,i4, "INC-001", "Incorporación · Laura Quispe",   admin.getId(), ana.getId(),    Map.of("Nombre completo","Laura Quispe Mamani","Email corporativo","laura@flowos.com","Cargo asignado","Analista Jr","Departamento","Finanzas","Fecha de inicio","2024-05-01"), 35, 5,  85, 58, 42);
            completedInc(w2, i0,i1,i2,i3,i4, "INC-002", "Incorporación · Roberto Flores", admin.getId(), carlos.getId(), Map.of("Nombre completo","Roberto Flores Jr","Email corporativo","rflores@flowos.com","Cargo asignado","Dev Jr","Departamento","Tecnología","Fecha de inicio","2024-06-01"),   22, 4,  92, 65, 38);
            completedInc(w2, i0,i1,i2,i3,i4, "INC-003", "Incorporación · Diego Mamani",   roberto.getId(),lucia.getId(), Map.of("Nombre completo","Diego Mamani Cruz","Email corporativo","diego@flowos.com","Cargo asignado","QA Jr","Departamento","Tecnología","Fecha de inicio","2024-07-01"),      14, 3,  78, 55, 40);
            completedInc(w2, i0,i1,i2,i3,i4, "INC-004", "Incorporación · Carmen Vargas",  admin.getId(), ana.getId(),    Map.of("Nombre completo","Carmen Vargas Ríos","Email corporativo","cvargas@flowos.com","Cargo asignado","Contadora","Departamento","Finanzas","Fecha de inicio","2024-08-01"),    6,  6, 100, 70, 50);

            // ── TRAMITES WORKFLOW 2 — ACTIVOS ────────────────────────────────
            activeTramite(w2, "INC-005", "Incorporación · Nuevo Dev",          admin.getId(), carlos.getId(), i2.getId(), Tramite.Status.EN_PROGRESO, Map.of("Nombre completo","Javier Flores","Email corporativo","jflores@flowos.com","Cargo asignado","Dev","Departamento","Tecnología","Fecha de inicio","2024-09-01"), 4);
            activeTramite(w2, "INC-006", "Incorporación · Nuevo Analista",     admin.getId(), ana.getId(),    i1.getId(), Tramite.Status.PENDIENTE,   Map.of(), 1);
        };
    }

    // ── helpers: tramites ─────────────────────────────────────────────────────

    /**
     * Tramite aprobado completo: n0→n1→n2→n3→n4(aprobado)→n6
     */
    private void completedVac(
            String wfId,
            WorkflowNodo n0, WorkflowNodo n1, WorkflowNodo n2,
            WorkflowNodo n3, WorkflowNodo n4, WorkflowNodo n6,
            String code, String title,
            String requestedBy, String assignedTo,
            Map<String, Object> formData,
            int daysAgo, int durationDays,
            int minN1, int minN2, int minN3, int minN4) {

        Tramite t = new Tramite();
        t.setCode(code); t.setTitle(title); t.setWorkflowId(wfId);
        t.setStatus(Tramite.Status.COMPLETADO);
        t.setCurrentNodoId(n6.getId());
        t.setRequestedById(requestedBy); t.setAssignedUserId(assignedTo);
        t.setFormData(formData);
        t = tramiteRepository.save(t);
        setCreatedAt(t.getId(), daysAgo);

        Instant d0 = ago(daysAgo);
        Instant d1 = ago(daysAgo - 1);
        Instant d2 = ago(daysAgo - 2);
        Instant d3 = ago(daysAgo - 3);
        Instant d4 = ago(daysAgo - durationDays);

        hist(t.getId(), null,         n1.getId(), "INICIADO",  requestedBy, "Solicitud enviada",              0,    d0);
        hist(t.getId(), n1.getId(),   n2.getId(), "AVANZADO",  requestedBy, "Formulario completado",          minN1, d1);
        hist(t.getId(), n2.getId(),   n3.getId(), "AVANZADO",  assignedTo,  "Revisión RRHH aprobada",         minN2, d2);
        hist(t.getId(), n3.getId(),   n4.getId(), "AVANZADO",  assignedTo,  "Decisión: Aprobar",              minN3, d3);
        hist(t.getId(), n4.getId(),   n6.getId(), "COMPLETADO",assignedTo,  "Notificación enviada al empleado",minN4, d4);
    }

    /**
     * Tramite rechazado completo: n0→n1→n2→n3→n5(rechazo)→n6
     */
    private void rejectedVac(
            String wfId,
            WorkflowNodo n0, WorkflowNodo n1, WorkflowNodo n2,
            WorkflowNodo n3, WorkflowNodo n5, WorkflowNodo n6,
            String code, String title,
            String requestedBy, String assignedTo,
            Map<String, Object> formData,
            int daysAgo, int durationDays,
            int minN1, int minN2, int minN3, int minN5) {

        Tramite t = new Tramite();
        t.setCode(code); t.setTitle(title); t.setWorkflowId(wfId);
        t.setStatus(Tramite.Status.RECHAZADO);
        t.setCurrentNodoId(n6.getId());
        t.setRequestedById(requestedBy); t.setAssignedUserId(assignedTo);
        t.setFormData(formData);
        t = tramiteRepository.save(t);
        setCreatedAt(t.getId(), daysAgo);

        Instant d0 = ago(daysAgo);
        Instant d1 = ago(daysAgo - 1);
        Instant d2 = ago(daysAgo - 2);
        Instant d3 = ago(daysAgo - 3);
        Instant d4 = ago(daysAgo - durationDays);

        hist(t.getId(), null,       n1.getId(), "INICIADO",  requestedBy, "Solicitud enviada",                 0,    d0);
        hist(t.getId(), n1.getId(), n2.getId(), "AVANZADO",  requestedBy, "Formulario completado",             minN1, d1);
        hist(t.getId(), n2.getId(), n3.getId(), "AVANZADO",  assignedTo,  "Revisión RRHH finalizada",          minN2, d2);
        hist(t.getId(), n3.getId(), n5.getId(), "AVANZADO",  assignedTo,  "Decisión: Rechazar — sin personal", minN3, d3);
        hist(t.getId(), n5.getId(), n6.getId(), "RECHAZADO", assignedTo,  "Empleado notificado del rechazo",   minN5, d4);
    }

    /**
     * Tramite de incorporación completado: n0→n1→n2→n3→n4
     */
    private void completedInc(
            String wfId,
            WorkflowNodo n0, WorkflowNodo n1, WorkflowNodo n2,
            WorkflowNodo n3, WorkflowNodo n4,
            String code, String title,
            String requestedBy, String assignedTo,
            Map<String, Object> formData,
            int daysAgo, int durationDays,
            int minN1, int minN2, int minN3) {

        Tramite t = new Tramite();
        t.setCode(code); t.setTitle(title); t.setWorkflowId(wfId);
        t.setStatus(Tramite.Status.COMPLETADO);
        t.setCurrentNodoId(n4.getId());
        t.setRequestedById(requestedBy); t.setAssignedUserId(assignedTo);
        t.setFormData(formData);
        t = tramiteRepository.save(t);
        setCreatedAt(t.getId(), daysAgo);

        Instant d0 = ago(daysAgo);
        Instant d1 = ago(daysAgo - 1);
        Instant d2 = ago(daysAgo - 2);
        Instant d3 = ago(daysAgo - 3);
        Instant d4 = ago(daysAgo - durationDays);

        hist(t.getId(), null,       n1.getId(), "INICIADO",  requestedBy, "Proceso iniciado",               0,    d0);
        hist(t.getId(), n1.getId(), n2.getId(), "AVANZADO",  assignedTo,  "Datos del empleado registrados", minN1, d1);
        hist(t.getId(), n2.getId(), n3.getId(), "AVANZADO",  assignedTo,  "Accesos configurados",           minN2, d2);
        hist(t.getId(), n3.getId(), n4.getId(), "COMPLETADO",requestedBy, "Revisión final aprobada",        minN3, d4);
    }

    /**
     * Tramite activo (EN_PROGRESO / PENDIENTE)
     */
    private void activeTramite(
            String wfId, String code, String title,
            String requestedBy, String assignedTo,
            String currentNodoId, Tramite.Status status,
            Map<String, Object> formData, int daysStarted) {

        Tramite t = new Tramite();
        t.setCode(code); t.setTitle(title); t.setWorkflowId(wfId);
        t.setStatus(status);
        t.setCurrentNodoId(currentNodoId);
        t.setRequestedById(requestedBy); t.setAssignedUserId(assignedTo);
        if (!formData.isEmpty()) t.setFormData(formData);
        t = tramiteRepository.save(t);
        setCreatedAt(t.getId(), daysStarted);

        hist(t.getId(), null, currentNodoId, "INICIADO", requestedBy, "Solicitud iniciada", 0, ago(daysStarted));
    }

    // ── helpers: builders ─────────────────────────────────────────────────────

    private User saveUser(String name, String email, String pass, User.Role role,
                          String cid, String deptId, String roleId) {
        User u = new User();
        u.setName(name); u.setEmail(email);
        u.setPassword(passwordEncoder.encode(pass));
        u.setRole(role); u.setCompanyId(cid);
        u.setDepartmentId(deptId); u.setJobRoleId(roleId);
        return userRepository.save(u);
    }

    private Department saveDept(String name, String cid) {
        Department d = new Department();
        d.setName(name); d.setCompanyId(cid);
        return departmentRepository.save(d);
    }

    private JobRole saveRole(String name, String cid, String deptId) {
        JobRole r = new JobRole();
        r.setName(name); r.setCompanyId(cid); r.setDepartmentId(deptId);
        return jobRoleRepository.save(r);
    }

    private WorkflowNodo saveNodo(String wfId, String name, String type, int order,
                                   String deptId, String roleId, User.Role role,
                                   boolean requiresForm, int avgMin,
                                   Double posX, Double posY, String trueL, String falseL) {
        WorkflowNodo n = new WorkflowNodo();
        n.setWorkflowId(wfId); n.setName(name); n.setNodeType(type); n.setOrder(order);
        n.setResponsibleDepartmentId(deptId); n.setResponsibleJobRoleId(roleId);
        n.setResponsibleRole(role); n.setRequiresForm(requiresForm); n.setAvgMinutes(avgMin);
        n.setPosX(posX); n.setPosY(posY); n.setTrueLabel(trueL); n.setFalseLabel(falseL);
        return workflowNodoRepository.save(n);
    }

    private WorkflowTransition mkTrans(String wfId, String from, String to, String cond) {
        WorkflowTransition t = new WorkflowTransition();
        t.setWorkflowId(wfId); t.setFromNodoId(from); t.setToNodoId(to); t.setCondition(cond);
        return t;
    }

    private void saveForm(String nodoId, String title, List<FormDefinition.FormField> fields) {
        FormDefinition fd = new FormDefinition();
        fd.setNodoId(nodoId); fd.setTitle(title); fd.setFields(fields);
        formDefinitionRepository.save(fd);
    }

    private FormDefinition.FormField mkField(String id, String name,
                                              FormDefinition.FieldType type,
                                              boolean required, int order) {
        FormDefinition.FormField f = new FormDefinition.FormField();
        f.setId(id); f.setName(name); f.setType(type); f.setRequired(required); f.setOrder(order);
        return f;
    }

    private void hist(String tramiteId, String from, String to, String action,
                      String changedBy, String comment, int durationMin, Instant at) {
        HistorialTramite h = new HistorialTramite();
        h.setTramiteId(tramiteId); h.setFromNodoId(from); h.setToNodoId(to);
        h.setAction(action); h.setChangedById(changedBy); h.setComment(comment);
        h.setDurationInNodo(durationMin > 0 ? durationMin : null);
        h.setChangedAt(at);
        historialTramiteRepository.save(h);
    }

    private void setCreatedAt(String tramiteId, int daysAgo) {
        mongoTemplate.updateFirst(
            new org.springframework.data.mongodb.core.query.Query(
                Criteria.where("_id").is(tramiteId)),
            new Update().set("createdAt", ago(daysAgo)),
            Tramite.class
        );
    }

    private Instant ago(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }
}
