package com.greenmobility.modules.payment.repository;

import com.greenmobility.modules.payment.domain.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, String> {
}

