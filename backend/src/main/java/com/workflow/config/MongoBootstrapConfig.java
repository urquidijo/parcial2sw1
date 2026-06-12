package com.workflow.config;

import com.workflow.model.*;
import com.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class MongoBootstrapConfig {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final CompanyRepository     companyRepository;
    private final DepartmentRepository  departmentRepository;
    private final JobRoleRepository     jobRoleRepository;
    private final WorkflowRepository         workflowRepository;
    private final WorkflowNodoRepository     workflowNodoRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final FormDefinitionRepository   formDefinitionRepository;
    private final TramiteRepository          tramiteRepository;
    private final HistorialTramiteRepository historialTramiteRepository;

    @Bean
    public ApplicationRunner seedData() {
        return args -> {
            // Only seed if the test user doesn't exist yet
            if (userRepository.existsByEmail("josueurquidicarrillo@gmail.com")) return;

            // ── 1. SUPERADMIN ────────────────────────────────────────────────────────
            User josue = makeUser("Josue Urquidi", "josueurquidicarrillo@gmail.com", "123456", User.Role.SUPERADMIN, null, null, null);
            userRepository.save(josue);

            // ── 2. COMPANY ───────────────────────────────────────────────────────────
            Company company = new Company();
            company.setName("FlowOS Bolivia");
            company = companyRepository.save(company);
            String cid = company.getId();

            // ── 3. DEPARTMENTS ───────────────────────────────────────────────────────
            Department deptRRHH      = saveDept("Recursos Humanos", cid);
            Department deptTech      = saveDept("Tecnología", cid);
            Department deptFinanzas  = saveDept("Finanzas", cid);

            // ── 4. JOB ROLES ─────────────────────────────────────────────────────────
            JobRole roleAnalistaRRHH = saveRole("Analista RRHH",         cid, deptRRHH.getId());
            JobRole roleJefeRRHH     = saveRole("Jefe de RRHH",          cid, deptRRHH.getId());
            JobRole roleDevTech      = saveRole("Desarrollador",          cid, deptTech.getId());
            JobRole roleTechLead     = saveRole("Tech Lead",              cid, deptTech.getId());
            JobRole roleAnalFin      = saveRole("Analista Financiero",    cid, deptFinanzas.getId());
            JobRole roleContador     = saveRole("Contador",               cid, deptFinanzas.getId());

            // ── 5. USERS ─────────────────────────────────────────────────────────────
            User admin  = makeUser("Admin FlowOS",  "admin@flowos.com",   "admin123",  User.Role.ADMIN,           cid, null,                null);
            User ana    = makeUser("Ana García",    "ana@flowos.com",     "ana123",    User.Role.ATENCION_CLIENTE, cid, deptRRHH.getId(),    roleAnalistaRRHH.getId());
            User carlos = makeUser("Carlos Ruiz",   "carlos@flowos.com",  "carlos123", User.Role.VALIDADOR,       cid, deptTech.getId(),     roleDevTech.getId());
            User maria  = makeUser("María López",   "maria@flowos.com",   "maria123",  User.Role.TECNICO,         cid, deptFinanzas.getId(), roleAnalFin.getId());
            User roberto= makeUser("Roberto Mamani","roberto@flowos.com", "roberto123",User.Role.ATENCION_CLIENTE, cid, deptRRHH.getId(),   roleJefeRRHH.getId());
            User luis   = makeUser("Luis Condori",  "luis@flowos.com",    "luis123",   User.Role.CLIENTE,         cid, null,                null);
            userRepository.saveAll(List.of(admin, ana, carlos, maria, roberto, luis));

            // Keep the original superadmin too
            if (!userRepository.existsByEmail("julio@gmail.com")) {
                userRepository.save(makeUser("Julio", "julio@gmail.com", "julioavila", User.Role.SUPERADMIN, null, null, null));
            }

            // ── 6. WORKFLOW 1: Solicitud de Vacaciones ───────────────────────────────
            Workflow wf1 = new Workflow();
            wf1.setName("Solicitud de Vacaciones");
            wf1.setDescription("Proceso para gestionar y aprobar solicitudes de vacaciones del personal.");
            wf1.setCompanyId(cid);
            wf1 = workflowRepository.save(wf1);
            String wf1id = wf1.getId();

            WorkflowNodo wf1n0  = saveNodo(wf1id, "Inicio",                   "inicio",    0,  null,               null,                    null,                    false, 0,   100.0, 200.0, null, null);
            WorkflowNodo wf1n1  = saveNodo(wf1id, "Registro de Solicitud",    "proceso",   1,  deptRRHH.getId(),   roleAnalistaRRHH.getId(), User.Role.ATENCION_CLIENTE, true, 60,  320.0, 200.0, null, null);
            WorkflowNodo wf1n2  = saveNodo(wf1id, "Revisión RRHH",            "proceso",   2,  deptRRHH.getId(),   roleJefeRRHH.getId(),    User.Role.ATENCION_CLIENTE, false, 120, 540.0, 200.0, null, null);
            WorkflowNodo wf1n3  = saveNodo(wf1id, "¿Aprobado?",               "decision",  3,  null,               null,                    null,                    false, 0,   760.0, 200.0, "Aprobar", "Rechazar");
            WorkflowNodo wf1n4  = saveNodo(wf1id, "Notificar Aprobación",     "proceso",   4,  deptRRHH.getId(),   roleAnalistaRRHH.getId(), User.Role.ATENCION_CLIENTE, false, 30, 980.0, 80.0, null, null);
            WorkflowNodo wf1n5  = saveNodo(wf1id, "Notificar Rechazo",        "proceso",   5,  deptRRHH.getId(),   roleAnalistaRRHH.getId(), User.Role.ATENCION_CLIENTE, false, 30, 980.0, 340.0, null, null);
            WorkflowNodo wf1n6  = saveNodo(wf1id, "Fin",                      "fin",       6,  null,               null,                    null,                    false, 0,  1200.0, 200.0, null, null);

            workflowTransitionRepository.saveAll(List.of(
                makeTrans(wf1id, wf1n0.getId(), wf1n1.getId(), null),
                makeTrans(wf1id, wf1n1.getId(), wf1n2.getId(), null),
                makeTrans(wf1id, wf1n2.getId(), wf1n3.getId(), null),
                makeTrans(wf1id, wf1n3.getId(), wf1n4.getId(), "Aprobar"),
                makeTrans(wf1id, wf1n3.getId(), wf1n5.getId(), "Rechazar"),
                makeTrans(wf1id, wf1n4.getId(), wf1n6.getId(), null),
                makeTrans(wf1id, wf1n5.getId(), wf1n6.getId(), null)
            ));

            // Form for "Registro de Solicitud"
            saveForm(wf1n1.getId(), "Formulario de Vacaciones", List.of(
                makeField("f1", "Motivo de la solicitud", FormDefinition.FieldType.TEXT,     true,  0),
                makeField("f2", "Fecha de inicio",        FormDefinition.FieldType.DATE,     true,  1),
                makeField("f3", "Fecha de retorno",       FormDefinition.FieldType.DATE,     true,  2),
                makeField("f4", "Días solicitados",       FormDefinition.FieldType.NUMBER,   true,  3),
                makeField("f5", "Observaciones",          FormDefinition.FieldType.TEXT,     false, 4)
            ));

            // ── 7. WORKFLOW 2: Incorporación de Empleado ─────────────────────────────
            Workflow wf2 = new Workflow();
            wf2.setName("Incorporación de Empleado");
            wf2.setDescription("Proceso de onboarding para nuevos empleados de la organización.");
            wf2.setCompanyId(cid);
            wf2 = workflowRepository.save(wf2);
            String wf2id = wf2.getId();

            WorkflowNodo wf2n0 = saveNodo(wf2id, "Inicio",              "inicio",  0, null,              null,                    null,                    false, 0,  100.0, 250.0, null, null);
            WorkflowNodo wf2n1 = saveNodo(wf2id, "Registro de Datos",   "proceso", 1, deptRRHH.getId(),  roleAnalistaRRHH.getId(), User.Role.ATENCION_CLIENTE, true, 90, 320.0, 250.0, null, null);
            WorkflowNodo wf2n2 = saveNodo(wf2id, "Configurar Accesos",  "proceso", 2, deptTech.getId(),  roleDevTech.getId(),     User.Role.VALIDADOR,       false, 60, 540.0, 250.0, null, null);
            WorkflowNodo wf2n3 = saveNodo(wf2id, "Revisión Final RRHH", "proceso", 3, deptRRHH.getId(),  roleJefeRRHH.getId(),    User.Role.ATENCION_CLIENTE, false, 45, 760.0, 250.0, null, null);
            WorkflowNodo wf2n4 = saveNodo(wf2id, "Fin",                 "fin",     4, null,              null,                    null,                    false, 0,  980.0, 250.0, null, null);

            workflowTransitionRepository.saveAll(List.of(
                makeTrans(wf2id, wf2n0.getId(), wf2n1.getId(), null),
                makeTrans(wf2id, wf2n1.getId(), wf2n2.getId(), null),
                makeTrans(wf2id, wf2n2.getId(), wf2n3.getId(), null),
                makeTrans(wf2id, wf2n3.getId(), wf2n4.getId(), null)
            ));

            saveForm(wf2n1.getId(), "Datos del Nuevo Empleado", List.of(
                makeField("g1", "Nombre completo",   FormDefinition.FieldType.TEXT,   true,  0),
                makeField("g2", "Email corporativo", FormDefinition.FieldType.EMAIL,  true,  1),
                makeField("g3", "Cargo asignado",    FormDefinition.FieldType.TEXT,   true,  2),
                makeField("g4", "Departamento",      FormDefinition.FieldType.TEXT,   true,  3),
                makeField("g5", "Fecha de inicio",   FormDefinition.FieldType.DATE,   true,  4),
                makeField("g6", "Equipo asignado",   FormDefinition.FieldType.CHECKBOX, false, 5)
            ));

            // ── 8. TRAMITES (expedientes) ─────────────────────────────────────────────
            Instant now = Instant.now();

            // Tramite 1 - COMPLETADO
            Tramite t1 = new Tramite();
            t1.setCode("VAC-001");
            t1.setTitle("Vacaciones Enero - Ana García");
            t1.setDescription("Solicitud de vacaciones del 15 al 31 de enero");
            t1.setWorkflowId(wf1id);
            t1.setStatus(Tramite.Status.COMPLETADO);
            t1.setCurrentNodoId(wf1n6.getId());
            t1.setRequestedById(ana.getId());
            t1.setAssignedUserId(roberto.getId());
            t1.setFormData(Map.of("Motivo de la solicitud", "Vacaciones anuales", "Fecha de inicio", "2024-01-15", "Fecha de retorno", "2024-02-01", "Días solicitados", 17));
            t1 = tramiteRepository.save(t1);
            addHistorial(t1.getId(), null,           wf1n1.getId(), "INICIADO",  ana.getId(),    "Solicitud enviada",                60,  now.minus(10, ChronoUnit.DAYS));
            addHistorial(t1.getId(), wf1n1.getId(),  wf1n2.getId(), "AVANZADO",  ana.getId(),    "Formulario completado",            60,  now.minus(9,  ChronoUnit.DAYS));
            addHistorial(t1.getId(), wf1n2.getId(),  wf1n3.getId(), "AVANZADO",  roberto.getId(), "Revisión aprobada por RRHH",      120, now.minus(8,  ChronoUnit.DAYS));
            addHistorial(t1.getId(), wf1n3.getId(),  wf1n4.getId(), "AVANZADO",  roberto.getId(), "Decisión: Aprobar",               30,  now.minus(7,  ChronoUnit.DAYS));
            addHistorial(t1.getId(), wf1n4.getId(),  wf1n6.getId(), "COMPLETADO",roberto.getId(), "Empleado notificado y completado", 20, now.minus(6,  ChronoUnit.DAYS));

            // Tramite 2 - EN_PROGRESO en Revisión RRHH
            Tramite t2 = new Tramite();
            t2.setCode("VAC-002");
            t2.setTitle("Vacaciones Febrero - Carlos Ruiz");
            t2.setDescription("Solicitud de vacaciones para semana de carnaval");
            t2.setWorkflowId(wf1id);
            t2.setStatus(Tramite.Status.EN_PROGRESO);
            t2.setCurrentNodoId(wf1n2.getId());
            t2.setRequestedById(carlos.getId());
            t2.setAssignedUserId(roberto.getId());
            t2.setFormData(Map.of("Motivo de la solicitud", "Carnaval", "Fecha de inicio", "2024-02-12", "Fecha de retorno", "2024-02-17", "Días solicitados", 5));
            t2 = tramiteRepository.save(t2);
            addHistorial(t2.getId(), null,           wf1n1.getId(), "INICIADO",  carlos.getId(), "Solicitud enviada",           60, now.minus(2, ChronoUnit.DAYS));
            addHistorial(t2.getId(), wf1n1.getId(),  wf1n2.getId(), "AVANZADO",  carlos.getId(), "Formulario completado",       60, now.minus(1, ChronoUnit.DAYS));

            // Tramite 3 - PENDIENTE de Incorporación
            Tramite t3 = new Tramite();
            t3.setCode("INC-001");
            t3.setTitle("Incorporación - Roberto Flores");
            t3.setDescription("Onboarding de nuevo desarrollador del equipo de tecnología");
            t3.setWorkflowId(wf2id);
            t3.setStatus(Tramite.Status.PENDIENTE);
            t3.setCurrentNodoId(wf2n1.getId());
            t3.setRequestedById(admin.getId());
            t3.setAssignedUserId(ana.getId());
            t3 = tramiteRepository.save(t3);
            addHistorial(t3.getId(), null, wf2n1.getId(), "INICIADO", admin.getId(), "Proceso iniciado por administración", 0, now.minus(1, ChronoUnit.HOURS));

            // Tramite 4 - RECHAZADO
            Tramite t4 = new Tramite();
            t4.setCode("VAC-003");
            t4.setTitle("Vacaciones Urgentes - María López");
            t4.setDescription("Solicitud rechazada por falta de personal disponible");
            t4.setWorkflowId(wf1id);
            t4.setStatus(Tramite.Status.RECHAZADO);
            t4.setCurrentNodoId(wf1n6.getId());
            t4.setRequestedById(maria.getId());
            t4.setAssignedUserId(roberto.getId());
            t4.setFormData(Map.of("Motivo de la solicitud", "Urgencia familiar", "Fecha de inicio", "2024-03-01", "Fecha de retorno", "2024-03-08", "Días solicitados", 7));
            t4 = tramiteRepository.save(t4);
            addHistorial(t4.getId(), null,           wf1n1.getId(), "INICIADO",   maria.getId(),   "Solicitud enviada",                 45, now.minus(5, ChronoUnit.DAYS));
            addHistorial(t4.getId(), wf1n1.getId(),  wf1n2.getId(), "AVANZADO",   maria.getId(),   "Formulario completado",             45, now.minus(4, ChronoUnit.DAYS));
            addHistorial(t4.getId(), wf1n2.getId(),  wf1n3.getId(), "AVANZADO",   roberto.getId(), "Revisado - insuficiente personal",  150,now.minus(3, ChronoUnit.DAYS));
            addHistorial(t4.getId(), wf1n3.getId(),  wf1n5.getId(), "AVANZADO",   roberto.getId(), "Decisión: Rechazar",                15, now.minus(3, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));
            addHistorial(t4.getId(), wf1n5.getId(),  wf1n6.getId(), "RECHAZADO",  roberto.getId(), "Empleado notificado del rechazo",   20, now.minus(2, ChronoUnit.DAYS));

            // Tramite 5 - EN_PROGRESO en Configurar Accesos (workflow 2)
            Tramite t5 = new Tramite();
            t5.setCode("INC-002");
            t5.setTitle("Incorporación - Laura Quispe");
            t5.setDescription("Onboarding analista financiero área finanzas");
            t5.setWorkflowId(wf2id);
            t5.setStatus(Tramite.Status.EN_PROGRESO);
            t5.setCurrentNodoId(wf2n2.getId());
            t5.setRequestedById(admin.getId());
            t5.setAssignedUserId(carlos.getId());
            t5.setFormData(Map.of("Nombre completo", "Laura Quispe Mamani", "Email corporativo", "laura@flowos.com", "Cargo asignado", "Analista Jr", "Departamento", "Finanzas", "Fecha de inicio", "2024-03-15"));
            t5 = tramiteRepository.save(t5);
            addHistorial(t5.getId(), null,           wf2n1.getId(), "INICIADO",  admin.getId(), "Proceso iniciado",             90, now.minus(3, ChronoUnit.DAYS));
            addHistorial(t5.getId(), wf2n1.getId(),  wf2n2.getId(), "AVANZADO",  ana.getId(),   "Datos registrados correctamente",90, now.minus(2, ChronoUnit.DAYS));
        };
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private User makeUser(String name, String email, String pass, User.Role role,
                          String companyId, String deptId, String jobRoleId) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(pass));
        u.setRole(role);
        u.setCompanyId(companyId);
        u.setDepartmentId(deptId);
        u.setJobRoleId(jobRoleId);
        return u;
    }

    private Department saveDept(String name, String companyId) {
        Department d = new Department();
        d.setName(name);
        d.setCompanyId(companyId);
        return departmentRepository.save(d);
    }

    private JobRole saveRole(String name, String companyId, String deptId) {
        JobRole r = new JobRole();
        r.setName(name);
        r.setCompanyId(companyId);
        r.setDepartmentId(deptId);
        return jobRoleRepository.save(r);
    }

    private WorkflowNodo saveNodo(String workflowId, String name, String nodeType, int order,
                                   String deptId, String jobRoleId, User.Role role,
                                   boolean requiresForm, int avgMinutes,
                                   Double posX, Double posY,
                                   String trueLabel, String falseLabel) {
        WorkflowNodo n = new WorkflowNodo();
        n.setWorkflowId(workflowId);
        n.setName(name);
        n.setNodeType(nodeType);
        n.setOrder(order);
        n.setResponsibleDepartmentId(deptId);
        n.setResponsibleJobRoleId(jobRoleId);
        n.setResponsibleRole(role);
        n.setRequiresForm(requiresForm);
        n.setAvgMinutes(avgMinutes);
        n.setPosX(posX);
        n.setPosY(posY);
        n.setTrueLabel(trueLabel);
        n.setFalseLabel(falseLabel);
        return workflowNodoRepository.save(n);
    }

    private WorkflowTransition makeTrans(String workflowId, String fromId, String toId, String condition) {
        WorkflowTransition t = new WorkflowTransition();
        t.setWorkflowId(workflowId);
        t.setFromNodoId(fromId);
        t.setToNodoId(toId);
        t.setCondition(condition);
        return t;
    }

    private void saveForm(String nodoId, String title, List<FormDefinition.FormField> fields) {
        FormDefinition fd = new FormDefinition();
        fd.setNodoId(nodoId);
        fd.setTitle(title);
        fd.setFields(fields);
        formDefinitionRepository.save(fd);
    }

    private FormDefinition.FormField makeField(String id, String name, FormDefinition.FieldType type,
                                                boolean required, int order) {
        FormDefinition.FormField f = new FormDefinition.FormField();
        f.setId(id);
        f.setName(name);
        f.setType(type);
        f.setRequired(required);
        f.setOrder(order);
        return f;
    }

    private void addHistorial(String tramiteId, String fromNodoId, String toNodoId,
                               String action, String changedById, String comment,
                               int durationMinutes, Instant changedAt) {
        HistorialTramite h = new HistorialTramite();
        h.setTramiteId(tramiteId);
        h.setFromNodoId(fromNodoId);
        h.setToNodoId(toNodoId);
        h.setAction(action);
        h.setChangedById(changedById);
        h.setComment(comment);
        h.setDurationInNodo(durationMinutes > 0 ? durationMinutes : null);
        h.setChangedAt(changedAt);
        historialTramiteRepository.save(h);
    }
}
