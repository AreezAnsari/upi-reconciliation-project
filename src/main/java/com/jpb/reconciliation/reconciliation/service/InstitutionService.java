package com.jpb.reconciliation.reconciliation.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.EnableStatus;
import com.jpb.reconciliation.reconciliation.dto.InstitutionDTO;
import com.jpb.reconciliation.reconciliation.entity.Institution;
import com.jpb.reconciliation.reconciliation.exception.DuplicateResourceException;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.mapper.InstitutionMapper;
import com.jpb.reconciliation.reconciliation.repository.InstitutionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InstitutionService {

    private final InstitutionRepository institutionRepository;
    private final InstitutionMapper institutionMapper;

    // ─── CREATE ───────────────────────────────────────────────────────────────

    public InstitutionDTO createInstitution(InstitutionDTO dto) {
        if (institutionRepository.existsByInstitutionName(dto.getInstitutionName())) {
            throw new DuplicateResourceException(
                    "Institution with name '" + dto.getInstitutionName() + "' already exists");
        }
        if (institutionRepository.existsByInstitutionUserId(dto.getInstitutionUserId())) {
            throw new DuplicateResourceException(
                    "Institution with user ID '" + dto.getInstitutionUserId() + "' already exists");
        }
        Institution institution = institutionMapper.toEntity(dto);
        Institution saved = institutionRepository.save(institution);
        return institutionMapper.toDTO(saved);
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InstitutionDTO getInstitutionById(Long id) {
        Institution institution = findOrThrow(id);
        return institutionMapper.toDTO(institution);
    }

    @Transactional(readOnly = true)
    public List<InstitutionDTO> getAllInstitutions() {
        return institutionRepository.findAll()
                .stream()
                .map(institutionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InstitutionDTO> getAllInstitutionsPaged(Pageable pageable) {
        return institutionRepository.findAll(pageable)
                .map(institutionMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<InstitutionDTO> getInstitutionsByStatus(EnableStatus status) {
        return institutionRepository.findByEnableStatus(status)
                .stream()
                .map(institutionMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    public InstitutionDTO updateInstitution(Long id, InstitutionDTO dto) {
        Institution existing = findOrThrow(id);

        // Check name uniqueness if changed
        if (!existing.getInstitutionName().equals(dto.getInstitutionName())
                && institutionRepository.existsByInstitutionName(dto.getInstitutionName())) {
            throw new DuplicateResourceException(
                    "Institution with name '" + dto.getInstitutionName() + "' already exists");
        }

        institutionMapper.updateEntityFromDTO(dto, existing);
        Institution updated = institutionRepository.save(existing);
        return institutionMapper.toDTO(updated);
    }

    /**
     * PATCH - change only the Enable Status
     */
    public InstitutionDTO updateStatus(Long id, EnableStatus status) {
        Institution existing = findOrThrow(id);
        existing.setEnableStatus(status);
        return institutionMapper.toDTO(institutionRepository.save(existing));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    /**
     * Per the manual: institution records cannot be deleted; status is changed instead.
     * This method changes status to INACTIVE as a "soft delete".
     */
    public void deleteInstitution(Long id) {
        Institution institution = findOrThrow(id);
        institution.setEnableStatus(EnableStatus.INACTIVE);
        institutionRepository.save(institution);
    }

    /**
     * Hard delete (use with caution — not recommended per business rules).
     */
    public void hardDeleteInstitution(Long id) {
        if (!institutionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Institution not found with ID: " + id);
        }
        institutionRepository.deleteById(id);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Institution findOrThrow(Long id) {
        return institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Institution not found with ID: " + id));
    }
}