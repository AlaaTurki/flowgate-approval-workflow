package com.flowgate.backend.startup;

import com.flowgate.backend.user.repository.RoleRepository;
import com.flowgate.backend.user.entity.Role;
import com.flowgate.backend.workflow.entity.RequestType;
import com.flowgate.backend.workflow.entity.Workflow;
import com.flowgate.backend.workflow.entity.WorkflowStep;
import com.flowgate.backend.workflow.repository.RequestTypeRepository;
import com.flowgate.backend.workflow.repository.WorkflowRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class WorkflowSeeder implements ApplicationRunner {

    private final RequestTypeRepository requestTypeRepository;
    private final WorkflowRepository workflowRepository;
    private final RoleRepository roleRepository;

    public WorkflowSeeder(RequestTypeRepository requestTypeRepository,
                          WorkflowRepository workflowRepository,
                          RoleRepository roleRepository) {
        this.requestTypeRepository = requestTypeRepository;
        this.workflowRepository = workflowRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        RequestType requestType = requestTypeRepository.findByName("Expense Reimbursement")
                .orElseGet(() -> requestTypeRepository.save(RequestType.builder()
                        .name("Expense Reimbursement")
                        .description("Travel and business expense reimbursement requests")
                        .active(true)
                        .createdAt(OffsetDateTime.now())
                        .build()));

        if (workflowRepository.findFirstByRequestTypeId(requestType.getId()).isEmpty()) {
            Role managerRole = roleRepository.findByName("ROLE_MANAGER")
                    .orElseThrow(() -> new IllegalStateException("Manager role must exist before workflow seeding"));

            Workflow workflow = Workflow.builder()
                    .name("Standard expense approval")
                    .requestType(requestType)
                    .active(true)
                    .createdAt(OffsetDateTime.now())
                    .build();

            WorkflowStep step = WorkflowStep.builder()
                    .workflow(workflow)
                    .name("Manager approval")
                    .orderIndex(0)
                    .approverRole(managerRole.getName())
                    .requiresComment(true)
                    .build();

            workflow.setSteps(List.of(step));
            workflowRepository.save(workflow);
        }
    }
}
