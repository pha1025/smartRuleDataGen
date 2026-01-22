package com.smartdata.smartruledatagen.generator;

import com.smartdata.smartruledatagen.model.CustMgrData;
import com.smartdata.smartruledatagen.model.EnumMapping;
import com.smartdata.smartruledatagen.service.ReferenceDataManager;
import com.smartdata.smartruledatagen.service.SqlTemplateRepository;
import com.smartdata.smartruledatagen.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractTableGenerator {

    @Autowired
    protected ReferenceDataManager referenceDataManager;
    @Autowired
    protected SqlTemplateRepository sqlTemplateRepository;

    protected final Random random = new Random();

    // 常用工具方法，子类可以直接使用
    protected String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    protected String formatDate(LocalDate date) {
        return DateUtil.formatDate(date);
    }

    protected String formatMonth(LocalDate date) {
        return DateUtil.formatMonth(date);
    }

    protected int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    protected <T> T randomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(random.nextInt(list.size()));
    }

    // 处理 SQL 模板中的 NULL 占位符
    protected String quote(String value) {
        return value == null ? "NULL" : "'" + value + "'";
    }
    protected String quote(Integer value) {
        return value == null ? "NULL" : String.valueOf(value);
    }
    protected String quote(Long value) {
        return value == null ? "NULL" : String.valueOf(value);
    }
    // ... 可以添加更多重载方法来处理不同类型的 NULL

    public abstract List<String> generateSqls(int count, Map<String, Object> params);
    public abstract String getDbKey(); // 获取当前生成器对应的数据库key
}
