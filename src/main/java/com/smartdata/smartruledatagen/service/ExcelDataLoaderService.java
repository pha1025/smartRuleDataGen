package com.smartdata.smartruledatagen.service;

import com.smartdata.smartruledatagen.model.CustMgrData;
import com.smartdata.smartruledatagen.model.CustomerData;
import com.smartdata.smartruledatagen.model.EnumMapping;
import com.smartdata.smartruledatagen.model.RegionProvinceData;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelDataLoaderService {

    public List<CustMgrData> loadCustMgrData(String filePath) {
        List<CustMgrData> dataList = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // 假设在第一个sheet
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) { // 跳过表头
                    firstRow = false;
                    continue;
                }
                if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                    continue; // 跳过空行
                }

                CustMgrData data = new CustMgrData();
                // 根据您图片中的Excel结构解析
                // 假设列顺序是：大区代码，客户经理ID，客户经理名称，网点代码，网点名称，大区名称
                // 需要您根据实际Excel列与模型字段的对应关系进行调整
                data.setCustMgrBigRegionCode(getCellValue(row.getCell(0)));
                data.setCustMgrBigRegionName(getCellValue(row.getCell(1))); // 假设大区名称在第6列
                data.setCustMgrOutletCode(getCellValue(row.getCell(2)));
                data.setCustMgrOutletName(getCellValue(row.getCell(3)));
                data.setCustMgrId(getCellValue(row.getCell(4)));
                data.setCustMgrName(getCellValue(row.getCell(5)));

                // 如果Excel中包含groupId/groupName，继续解析
                data.setGroupId(getCellValue(row.getCell(6)));
                data.setGroupName(getCellValue(row.getCell(7)));

                dataList.add(data);
            }

        } catch (IOException e) {
            e.printStackTrace();
            // 抛出自定义异常或处理错误
        }
        return dataList;
    }

    public Map<String, List<EnumMapping>> loadEnumMappings(String filePath) {
        Map<String, List<EnumMapping>> enumMap = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = WorkbookFactory.create(fis)) {

            // 假设每个sheet代表一个枚举类别 (e.g., sheet1: business_class, sheet2: order_type)
            // 或者在一个sheet中，第一列是 category, 第二列是 code, 第三列是 name
            // 这里我们以每个Sheet为一个Category为例
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String category = sheet.getSheetName(); // sheet名作为 category
                List<EnumMapping> mappings = new ArrayList<>();
                boolean firstRow = true;
                for (Row row : sheet) {
                    if (firstRow) {
                        firstRow = false;
                        continue;
                    }
                    if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                        continue;
                    }
                    EnumMapping mapping = new EnumMapping();
                    mapping.setCategory(category);
                    mapping.setCode(getCellValue(row.getCell(0))); // 假设第一列是 code
                    mapping.setName(getCellValue(row.getCell(1))); // 假设第二列是 name
                    mappings.add(mapping);
                }
                enumMap.put(category, mappings);
            }

        } catch (IOException e) {
            e.printStackTrace();
            // 抛出自定义异常或处理错误
        }
        return enumMap;
    }

    public List<CustomerData> loadCustomerData(String filePath) {
        List<CustomerData> dataList = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false;
                    continue;
                }
                if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                    continue;
                }

                CustomerData data = new CustomerData();
                // Assumed columns: 0: customer_id, 1: customer_name, 2: customer_type, 3: customer_manage_id
                data.setCustomerId(getCellValue(row.getCell(0)));
                data.setCustomerName(getCellValue(row.getCell(1)));
                data.setCustomerType(getCellValue(row.getCell(2)));
                data.setCustomerManageId(getCellValue(row.getCell(3)));

                dataList.add(data);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataList;
    }

    public List<RegionProvinceData> loadRegionProvinceData(String filePath) {
        List<RegionProvinceData> dataList = new ArrayList<>();
        // 如果文件不存在，返回空列表
        File file = new File(filePath);
        if (!file.exists()) {
            return dataList;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false;
                    continue;
                }
                if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                    continue;
                }

                RegionProvinceData data = new RegionProvinceData();
                // Assumed columns: 0: big_region_code, 1: province_city_area_code
                data.setBigRegionCode(getCellValue(row.getCell(0)));
                data.setProvinceCityAreaCode(getCellValue(row.getCell(1)));

                dataList.add(data);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataList;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue()); // 防止数字被读成浮点数
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula(); // 可以考虑 evaluateFormulaCell
            case BLANK -> null;
            default -> null;
        };
    }
}
