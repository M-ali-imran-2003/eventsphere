package com.example.eventsphere.service;

import com.example.eventsphere.dto.ChangeAppStatusDTO;
import com.example.eventsphere.dto.WorkspaceDTO;
import com.example.eventsphere.dto.WorkspaceListDTO;
import com.example.eventsphere.entity.Workspace;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final GenericMapper mapper;
    private final WorkspaceRepository workspaceRepository;

    @Autowired
    public WorkspaceService(GenericMapper mapper, WorkspaceRepository workspaceRepository) {
        this.mapper = mapper;
        this.workspaceRepository = workspaceRepository;
    }

    public List<WorkspaceListDTO> getAllWorkspaces(){
        return mapper.mapList(workspaceRepository.findAll(), WorkspaceListDTO.class);
    }

    public WorkspaceDTO getWorkspaceById(UUID id){
        Workspace workspace = workspaceRepository.findById(id).orElseThrow(()-> new RuntimeException("Workspace Not Found with ID: "+id));
        WorkspaceDTO wk = mapper.map(workspaceRepository.findById(id), WorkspaceDTO.class);
        wk.setPlanName("Basic");
        return wk;
    }

    public void changeWorkspaceStatus(ChangeAppStatusDTO status, UUID id){
        Workspace workspace = workspaceRepository.findById(id).orElseThrow(()-> new RuntimeException("Workspace Not Found with ID: "+id));
        if (status.getStatus() != null && !status.getStatus().name().isBlank()){
            workspace.setStatus(status.getStatus());
        }
        workspaceRepository.save(workspace);
    }
}
