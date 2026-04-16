package com.example.eventsphere.controller;

import com.example.eventsphere.dto.CategoryDTO;
import com.example.eventsphere.dto.ChangeAppStatusDTO;
import com.example.eventsphere.dto.WorkspaceDTO;
import com.example.eventsphere.dto.WorkspaceListDTO;
import com.example.eventsphere.service.CategoryService;
import com.example.eventsphere.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspace")
@Slf4j
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-workspaces")
    public ResponseEntity<List<WorkspaceListDTO>> getAllWorkspaces() {
        List<WorkspaceListDTO> workspaces = workspaceService.getAllWorkspaces(); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok(workspaces);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-workspace-by-id/{id}")
    public ResponseEntity<WorkspaceDTO> getWorkspaceById(@PathVariable UUID id) {
        WorkspaceDTO workspace = workspaceService.getWorkspaceById(id); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok(workspace);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/update-workspace-status/{id}")
    public ResponseEntity<WorkspaceDTO> updateStatus(@PathVariable UUID id, @Valid @RequestBody ChangeAppStatusDTO status) {
        workspaceService.changeWorkspaceStatus(status,id); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok().build();
    }
}
