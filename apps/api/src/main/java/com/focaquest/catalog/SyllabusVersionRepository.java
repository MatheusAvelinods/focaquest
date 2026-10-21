package com.focaquest.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SyllabusVersionRepository extends JpaRepository<SyllabusVersion, String> {
    Optional<SyllabusVersion> findFirstByRoleIdAndCurrentTrue(String roleId);

    List<SyllabusVersion> findByRoleId(String roleId);
}
