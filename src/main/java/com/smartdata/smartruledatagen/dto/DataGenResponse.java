package com.smartdata.smartruledatagen.dto;

import lombok.Data;
import java.util.List;

@Data
public class DataGenResponse {
    private boolean success;
    private String message;
    private int generatedCount;
    private int successInsertCount;
    private List<String> sqls;
}
