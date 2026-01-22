package com.smartdata.smartruledatagen.config;

import com.smartdata.smartruledatagen.model.rules.RuleConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GeneratorConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(GeneratorConfigLoader.class);

    @Value("${generator.rules-path}")
    private String rulesPath;

    private final ResourceLoader resourceLoader;
    private RuleConfig ruleConfig;

    public GeneratorConfigLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws IOException {
        log.info("Loading generator rules from: {}", rulesPath);
        Resource resource = resourceLoader.getResource(rulesPath);
        if (!resource.exists()) {
            throw new IOException("Generator rules file not found: " + rulesPath);
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        this.ruleConfig = mapper.readValue(resource.getInputStream(), RuleConfig.class);
        log.info("Successfully loaded {} generator definitions.", ruleConfig.getGenerators().size());
    }

    public RuleConfig getRuleConfig() {
        return ruleConfig;
    }
}
