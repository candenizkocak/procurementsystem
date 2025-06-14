package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File, Integer> {
}