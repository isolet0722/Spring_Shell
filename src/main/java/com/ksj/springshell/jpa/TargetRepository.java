package com.ksj.springshell.jpa;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface TargetRepository extends JpaRepository<TargetEntity, Long> {}