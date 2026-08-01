package com.tempmailpro.domain.ports;

import com.tempmailpro.domain.models.Template;
import java.util.List;

public interface TemplateRepository {
    Template save(Template template);
    List<Template> findByOwnerId(Long userId);
    void deleteById(Long id);
}