package com.nib.projecttracking.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.Task;
import com.nib.projecttracking.entity.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;  
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportExportService {
    
    
    
    /**
     * ✅ Export Projects to PDF
     */
    public byte[] exportProjectsToPdf(List<Project> projects) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4.rotate());
            
          
            Paragraph title = new Paragraph("NIB IT Project Tracking - Projects Report")
                .setFontSize(18).setBold().setTextAlignment(TextAlignment.CENTER).setMarginBottom(20);
            document.add(title);
            
          
            Paragraph date = new Paragraph("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")))
                .setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(20);
            document.add(date);
            
          
            float[] columnWidths = {3, 2, 2, 2, 3, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();
            
          
            String[] headers = {"Project Name", "Type", "Status", "RAG", "Manager", "Completion"};
            for (String header : headers) {
                Cell cell = new Cell().add(new Paragraph(header))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY).setBold()
                    .setTextAlignment(TextAlignment.CENTER);
                table.addHeaderCell(cell);
            }
           
            for (Project project : projects) {
                table.addCell(new Cell().add(new Paragraph(project.getProjectName() != null ? project.getProjectName() : "N/A")));
                table.addCell(new Cell().add(new Paragraph(project.getProjectType() != null ? project.getProjectType() : "N/A")));
                table.addCell(new Cell().add(new Paragraph(project.getStatus() != null ? project.getStatus().toString() : "N/A")));
                
                Cell ragCell = new Cell().add(new Paragraph(project.getRagStatus() != null ? project.getRagStatus().toString() : "N/A"));
                if (project.getRagStatus() != null) {
                    switch (project.getRagStatus()) {
                        case GREEN: ragCell.setBackgroundColor(ColorConstants.GREEN); break;
                        case AMBER: ragCell.setBackgroundColor(ColorConstants.ORANGE); break;
                        case RED: ragCell.setBackgroundColor(ColorConstants.RED); break;
                    }
                }
                table.addCell(ragCell);
                
                table.addCell(new Cell().add(new Paragraph(
                    project.getManager() != null && project.getManager().getFullName() != null 
                        ? project.getManager().getFullName() : "N/A")));
                
                table.addCell(new Cell().add(new Paragraph(
                    project.getCompletionPercentage() != null 
                        ? project.getCompletionPercentage() + "%" : "0%")));
            }
            
            document.add(table);
            document.add(new Paragraph("\nTotal Projects: " + projects.size()).setFontSize(12).setBold().setMarginTop(20));
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * ✅ Export Projects to Excel
     */
    public byte[] exportProjectsToExcel(List<Project> projects) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Projects Report");
            
           
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
          
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Project Name", "Type", "Status", "RAG Status", "Manager", "Start Date", "End Date", "Completion %"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
          
            int rowNum = 1;
            for (Project project : projects) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(project.getProjectName() != null ? project.getProjectName() : "N/A");
                row.createCell(1).setCellValue(project.getProjectType() != null ? project.getProjectType() : "N/A");
                row.createCell(2).setCellValue(project.getStatus() != null ? project.getStatus().toString() : "N/A");
                row.createCell(3).setCellValue(project.getRagStatus() != null ? project.getRagStatus().toString() : "N/A");
                row.createCell(4).setCellValue(project.getManager() != null && project.getManager().getFullName() != null 
                    ? project.getManager().getFullName() : "N/A");
                row.createCell(5).setCellValue(project.getStartDate() != null ? project.getStartDate().toString() : "N/A");
                row.createCell(6).setCellValue(project.getEndDate() != null ? project.getEndDate().toString() : "N/A");
                row.createCell(7).setCellValue(project.getCompletionPercentage() != null ? project.getCompletionPercentage() : 0);
            }
            
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating Excel: " + e.getMessage(), e);
        }
    }
    
    /**
     * ✅ Export Tasks to Excel
     */
    public byte[] exportTasksToExcel(List<Task> tasks) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Tasks Report");
            
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Task Name", "Project", "Assigned To", "Status", "Due Date", "Priority"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int rowNum = 1;
            for (Task task : tasks) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(task.getTaskName() != null ? task.getTaskName() : "N/A");
                row.createCell(1).setCellValue(task.getProject() != null && task.getProject().getProjectName() != null 
                    ? task.getProject().getProjectName() : "N/A");
                row.createCell(2).setCellValue(task.getAssignedTo() != null && task.getAssignedTo().getFullName() != null 
                    ? task.getAssignedTo().getFullName() : "N/A");
                row.createCell(3).setCellValue(task.getStatus() != null ? task.getStatus().toString() : "N/A");
                row.createCell(4).setCellValue(task.getDueDate() != null ? task.getDueDate().toString() : "N/A");
                row.createCell(5).setCellValue(task.getPriority() != null ? task.getPriority().toString() : "N/A");
            }
            
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating Excel: " + e.getMessage(), e);
        }
    }
    
    
    /**
     * ✅ Export Dashboard Summary to PDF (with Team Performance)
     */
    public byte[] exportDashboardSummaryToPdf(Map<String, Object> summary, 
                                               List<Map<String, Object>> teamPerformance,
                                               LocalDate startDate, 
                                               LocalDate endDate) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
         
            Paragraph title = new Paragraph("NIB IT Project Tracking - Dashboard Summary Report")
                .setFontSize(18).setBold().setTextAlignment(TextAlignment.CENTER).setMarginBottom(10);
            document.add(title);
            
        
            String dateRange = (startDate != null && endDate != null) 
                ? startDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) + " - " + endDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                : "All Time";
            document.add(new Paragraph("Period: " + dateRange).setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
            
          
            document.add(new Paragraph("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")))
                .setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(20));
            
         
            document.add(new Paragraph("Key Metrics").setFontSize(14).setBold().setMarginBottom(10));
            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            addStatRow(statsTable, "Total Projects", String.valueOf(summary.getOrDefault("totalProjects", 0)));
            addStatRow(statsTable, "Active Projects", String.valueOf(summary.getOrDefault("activeProjects", 0)));
            addStatRow(statsTable, "Critical Projects", String.valueOf(summary.getOrDefault("criticalProjects", 0)));
            addStatRow(statsTable, "Completed Projects", String.valueOf(summary.getOrDefault("completedProjects", 0)));
            addStatRow(statsTable, "Overdue Tasks", String.valueOf(summary.getOrDefault("overdueTasks", 0)));
            addStatRow(statsTable, "Overdue Milestones", String.valueOf(summary.getOrDefault("overdueMilestones", 0)));
            addStatRow(statsTable, "Active Blockers", String.valueOf(summary.getOrDefault("activeBlockers", 0)));
            document.add(statsTable);
       
            if (summary.get("ragStatus") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Long> ragStatus = (Map<String, Long>) summary.get("ragStatus");
                document.add(new Paragraph("\nRAG Status Distribution").setFontSize(14).setBold().setMarginBottom(10));
                Table ragTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
                ragTable.addHeaderCell(new Cell().add(new Paragraph("Status")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
                ragTable.addHeaderCell(new Cell().add(new Paragraph("Count")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
                ragTable.addCell(new Cell().add(new Paragraph("Green")));
                ragTable.addCell(new Cell().add(new Paragraph(String.valueOf(ragStatus.getOrDefault("GREEN", 0L)))));
                ragTable.addCell(new Cell().add(new Paragraph("Amber")));
                ragTable.addCell(new Cell().add(new Paragraph(String.valueOf(ragStatus.getOrDefault("AMBER", 0L)))));
                ragTable.addCell(new Cell().add(new Paragraph("Red")));
                ragTable.addCell(new Cell().add(new Paragraph(String.valueOf(ragStatus.getOrDefault("RED", 0L)))));
                document.add(ragTable);
            }
            
            
            if (teamPerformance != null && !teamPerformance.isEmpty()) {
                document.add(new Paragraph("\n👥 Team Performance").setFontSize(14).setBold().setMarginBottom(10));
                Table perfTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1, 1, 1, 1}))
                    .useAllAvailableWidth();
                
                String[] perfHeaders = {"Member", "Total", "Completed", "Pending", "Rate", "Performance"};
                for (String h : perfHeaders) {
                    perfTable.addHeaderCell(new Cell().add(new Paragraph(h))
                        .setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER));
                }
                
                for (Map<String, Object> member : teamPerformance) {
                    perfTable.addCell(new Cell().add(new Paragraph(member.get("userName").toString())));
                    perfTable.addCell(new Cell().add(new Paragraph(member.get("totalTasks").toString())).setTextAlignment(TextAlignment.CENTER));
                    perfTable.addCell(new Cell().add(new Paragraph(member.get("completedTasks").toString())).setTextAlignment(TextAlignment.CENTER));
                    perfTable.addCell(new Cell().add(new Paragraph(member.get("pendingTasks") != null ? member.get("pendingTasks").toString() : "0")).setTextAlignment(TextAlignment.CENTER));
                    perfTable.addCell(new Cell().add(new Paragraph(member.get("completionRate").toString())).setTextAlignment(TextAlignment.CENTER));
                    
                  
                    Cell perfCell = new Cell().add(new Paragraph(member.get("performanceRating").toString())).setTextAlignment(TextAlignment.CENTER);
                    String rating = member.get("performanceRating").toString();
                    if ("Excellent".equals(rating)) perfCell.setBackgroundColor(ColorConstants.GREEN);
                    else if ("Good".equals(rating)) perfCell.setBackgroundColor(ColorConstants.ORANGE);
                    else perfCell.setBackgroundColor(ColorConstants.RED);
                    perfTable.addCell(perfCell);
                }
                document.add(perfTable);
            }
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * ✅ Export Dashboard Summary to Excel (with Team Performance)
     */
    public byte[] exportDashboardSummaryToExcel(Map<String, Object> summary, 
                                                 List<Map<String, Object>> teamPerformance,
                                                 LocalDate startDate, 
                                                 LocalDate endDate) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Dashboard Summary");
            
       
            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("NIB IT Project Tracking - Dashboard Summary Report");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short)16);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
    
            String dateRange = (startDate != null && endDate != null) 
                ? startDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) + " - " + endDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                : "All Time";
            Row dateRow = sheet.createRow(2);
            dateRow.createCell(0).setCellValue("Period: " + dateRange);
            
           
            Row genDateRow = sheet.createRow(3);
            genDateRow.createCell(0).setCellValue("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
            
            Row metricsHeaderRow = sheet.createRow(5);
            org.apache.poi.ss.usermodel.Cell metricsHeaderCell = metricsHeaderRow.createCell(0);
            metricsHeaderCell.setCellValue("Key Metrics");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            metricsHeaderCell.setCellStyle(headerStyle);
            
            int rowNum = 6;
            rowNum = addMetricRow(sheet, rowNum, "Total Projects", String.valueOf(summary.getOrDefault("totalProjects", 0)), workbook);
            rowNum = addMetricRow(sheet, rowNum, "Active Projects", String.valueOf(summary.getOrDefault("activeProjects", 0)), workbook);
            rowNum = addMetricRow(sheet, rowNum, "Critical Projects", String.valueOf(summary.getOrDefault("criticalProjects", 0)), workbook);
            rowNum = addMetricRow(sheet, rowNum, "Completed Projects", String.valueOf(summary.getOrDefault("completedProjects", 0)), workbook);
            rowNum = addMetricRow(sheet, rowNum, "Overdue Tasks", String.valueOf(summary.getOrDefault("overdueTasks", 0)), workbook);
            rowNum = addMetricRow(sheet, rowNum, "Overdue Milestones", String.valueOf(summary.getOrDefault("overdueMilestones", 0)), workbook);
            rowNum = addMetricRow(sheet, rowNum, "Active Blockers", String.valueOf(summary.getOrDefault("activeBlockers", 0)), workbook);
            
         
            if (summary.get("ragStatus") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Long> ragStatus = (Map<String, Long>) summary.get("ragStatus");
                Row ragHeaderRow = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell ragHeaderCell = ragHeaderRow.createCell(0);
                ragHeaderCell.setCellValue("RAG Status Distribution");
                ragHeaderCell.setCellStyle(headerStyle);
                rowNum = addMetricRow(sheet, rowNum, "Green", String.valueOf(ragStatus.getOrDefault("GREEN", 0L)), workbook);
                rowNum = addMetricRow(sheet, rowNum, "Amber", String.valueOf(ragStatus.getOrDefault("AMBER", 0L)), workbook);
                rowNum = addMetricRow(sheet, rowNum, "Red", String.valueOf(ragStatus.getOrDefault("RED", 0L)), workbook);
            }
            
            if (teamPerformance != null && !teamPerformance.isEmpty()) {
                rowNum += 2;
                Row perfHeaderRow = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell perfHeaderCell = perfHeaderRow.createCell(0);
                perfHeaderCell.setCellValue("👥 Team Performance");
                perfHeaderCell.setCellStyle(headerStyle);
                
                Row perfHeadersRow = sheet.createRow(rowNum++);
                String[] perfHeaders = {"Member", "Total", "Completed", "Pending", "Completion Rate", "Performance"};
                for (int i = 0; i < perfHeaders.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = perfHeadersRow.createCell(i);
                    cell.setCellValue(perfHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                for (Map<String, Object> member : teamPerformance) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(member.get("userName").toString());
                    row.createCell(1).setCellValue(Integer.parseInt(member.get("totalTasks").toString()));
                    row.createCell(2).setCellValue(Integer.parseInt(member.get("completedTasks").toString()));
                    row.createCell(3).setCellValue(Integer.parseInt(member.get("pendingTasks") != null ? member.get("pendingTasks").toString() : "0"));
                    row.createCell(4).setCellValue(member.get("completionRate").toString());
                    
                    org.apache.poi.ss.usermodel.Cell perfCell = row.createCell(5);
                    String rating = member.get("performanceRating").toString();
                    perfCell.setCellValue(rating);
                    
                    CellStyle perfStyle = workbook.createCellStyle();
                    if ("Excellent".equals(rating)) {
                        perfStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
                    } else if ("Good".equals(rating)) {
                        perfStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                    } else {
                        perfStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
                    }
                    perfStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    perfCell.setCellStyle(perfStyle);
                }
            }
            
            for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);
            workbook.write(baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating Excel: " + e.getMessage(), e);
        }
    }
    
    
    /**
     * ✅ Calculate Team Performance Metrics - ONLY TECHNICAL STAFF
     */
    public List<Map<String, Object>> calculateTeamPerformance(List<Task> tasks, List<User> users) {
        List<Map<String, Object>> teamPerformance = new ArrayList<>();
        
        List<String> technicalRoles = Arrays.asList(
            "DEVELOPER",
            "SENIOR_IT_OFFICER",
            "JUNIOR_IT_OFFICER",
            "IT_GRADUATE_TRAINEE"
        );
        
        for (User user : users) {
            
            if (user.getRole() == null || !technicalRoles.contains(user.getRole().toString())) {
                continue;
            }
            
            List<Task> userTasks = tasks.stream()
                .filter(t -> t.getAssignedTo() != null && 
                            t.getAssignedTo().getId() != null &&
                            t.getAssignedTo().getId().equals(user.getId()))
                .collect(Collectors.toList());
            
            long totalTasks = userTasks.size();
            long completedTasks = userTasks.stream()
                .filter(t -> t.getStatus() != null && "COMPLETED".equals(t.getStatus().toString()))
                .count();
            long pendingTasks = userTasks.stream()
                .filter(t -> t.getStatus() != null && 
                            !"COMPLETED".equals(t.getStatus().toString()))
                .count();
            
            double completionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0;
            
            String performanceRating;
            if (totalTasks == 0) {
                performanceRating = "No Tasks";
            } else if (completionRate >= 80) {
                performanceRating = "Excellent";
            } else if (completionRate >= 50) {
                performanceRating = "Good";
            } else {
                performanceRating = "Needs Improvement";
            }
            
            Map<String, Object> userStats = new HashMap<>();
            userStats.put("userId", user.getId());
            userStats.put("userName", user.getFullName() != null ? user.getFullName() : user.getUsername());
            userStats.put("role", user.getRole() != null ? user.getRole().toString() : "N/A");
            userStats.put("totalTasks", totalTasks);
            userStats.put("completedTasks", completedTasks);
            userStats.put("pendingTasks", pendingTasks);
            userStats.put("completionRate", String.format("%.1f%%", completionRate));
            userStats.put("performanceRating", performanceRating);
            userStats.put("productivity", performanceRating);
            
            teamPerformance.add(userStats);
        }
        
        teamPerformance.sort((a, b) -> {
            Double rateA = Double.parseDouble(a.get("completionRate").toString().replace("%", ""));
            Double rateB = Double.parseDouble(b.get("completionRate").toString().replace("%", ""));
            return rateB.compareTo(rateA);
        });
        
        return teamPerformance;
    }
    
    
    private void addStatRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label)).setBold());
        table.addCell(new Cell().add(new Paragraph(value)));
    }
    
    private int addMetricRow(Sheet sheet, int rowNum, String label, String value, Workbook workbook) {
        Row row = sheet.createRow(rowNum);
        org.apache.poi.ss.usermodel.Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        CellStyle labelStyle = workbook.createCellStyle();
        Font labelFont = workbook.createFont();
        labelFont.setBold(true); 
        labelStyle.setFont(labelFont);
        labelCell.setCellStyle(labelStyle);
        org.apache.poi.ss.usermodel.Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        return rowNum + 1;
    }
    
    
    /**
     * Generate detailed project report PDF (placeholder - uses dashboard summary)
     */
   /**
 * Generate detailed project report PDF with filtered blockers
 */
public byte[] generateDetailedProjectPdf(ReportService.DetailedProjectReportDTO report) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        
      
        Paragraph title = new Paragraph("NIB IT Project Tracking - Detailed Project Report")
            .setFontSize(18).setBold().setTextAlignment(TextAlignment.CENTER).setMarginBottom(10);
        document.add(title);
        
        document.add(new Paragraph(report.getProjectName()).setFontSize(16).setBold().setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
        document.add(new Paragraph("Period: " + report.getReportPeriodStart() + " to " + report.getReportPeriodEnd())
            .setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));
        
        
        if (report.getSummary() != null) {
            document.add(new Paragraph("Summary Statistics").setFontSize(14).setBold().setMarginBottom(10));
            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            addStatRow(statsTable, "Total Tasks", String.valueOf(report.getSummary().getTotalTasks()));
            addStatRow(statsTable, "Completed Tasks", String.valueOf(report.getSummary().getCompletedTasks()));
            addStatRow(statsTable, "Completion Rate", report.getSummary().getCompletionPercentage() + "%");
         
            long activeBlockers = report.getDailyUpdates() != null 
                ? report.getDailyUpdates().stream()
                    .filter(u -> u.getBlockers() != null && !u.getBlockers().isEmpty() 
                        && !"SOLVED".equals(u.getBlockerStatus()))
                    .count()
                : 0;
            addStatRow(statsTable, "Active Blockers", String.valueOf(activeBlockers));
            
            document.add(statsTable);
        }
        
        if (report.getDailyUpdates() != null && !report.getDailyUpdates().isEmpty()) {
            document.add(new Paragraph("\nActive Blockers").setFontSize(14).setBold().setMarginBottom(10));
            
            Table blockersTable = new Table(UnitValue.createPercentArray(new float[]{2, 3, 2, 2}))
                .useAllAvailableWidth();
            blockersTable.addHeaderCell(new Cell().add(new Paragraph("Date")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
            blockersTable.addHeaderCell(new Cell().add(new Paragraph("Blocker")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
            blockersTable.addHeaderCell(new Cell().add(new Paragraph("Submitted By")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
            blockersTable.addHeaderCell(new Cell().add(new Paragraph("Status")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
            
            List<ReportService.DetailedProjectReportDTO.ProgressUpdateDTO> activeBlockers = 
                report.getDailyUpdates().stream()
                    .filter(u -> u.getBlockers() != null && !u.getBlockers().isEmpty() 
                        && !"SOLVED".equals(u.getBlockerStatus()))
                    .collect(Collectors.toList());
            
            if (activeBlockers.isEmpty()) {
                
                blockersTable.addCell(new Cell().add(new Paragraph("No active blockers")).setBold());
                blockersTable.addCell(new Cell().add(new Paragraph("-")));
                blockersTable.addCell(new Cell().add(new Paragraph("-")));
                blockersTable.addCell(new Cell().add(new Paragraph("-")));
            } else {
                for (ReportService.DetailedProjectReportDTO.ProgressUpdateDTO update : activeBlockers) {
                    blockersTable.addCell(new Cell().add(new Paragraph(
                        update.getUpdateDate() != null ? update.getUpdateDate().toString() : "N/A")));
                    blockersTable.addCell(new Cell().add(new Paragraph(update.getBlockers())));
                    blockersTable.addCell(new Cell().add(new Paragraph(
                        update.getSubmittedByName() != null ? update.getSubmittedByName() : "Unknown")));
                    
                    Cell statusCell = new Cell().add(new Paragraph("OPEN"));
                    statusCell.setBackgroundColor(ColorConstants.ORANGE);
                    blockersTable.addCell(statusCell);
                }
            }
            document.add(blockersTable);
        }
        
        document.close();
        return baos.toByteArray();
        
    } catch (Exception e) {
        throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
    }
}
    
    /**
     * Generate detailed project report Excel (placeholder - uses dashboard summary)
     */
   /**
 * Generate detailed project report Excel with filtered blockers
 */
public byte[] generateDetailedProjectExcel(ReportService.DetailedProjectReportDTO report) {
    try (Workbook workbook = new XSSFWorkbook();
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        
        Sheet sheet = workbook.createSheet("Detailed Report");
        
       
        Row titleRow = sheet.createRow(0);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("NIB IT Project Tracking - Detailed Project Report");
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short)16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);
        
        sheet.createRow(1).createCell(0).setCellValue(report.getProjectName());
        sheet.createRow(2).createCell(0).setCellValue("Period: " + report.getReportPeriodStart() + " to " + report.getReportPeriodEnd());
        
        
        if (report.getSummary() != null) {
            Row summaryHeader = sheet.createRow(4);
            summaryHeader.createCell(0).setCellValue("Summary Statistics");
            summaryHeader.getCell(0).setCellStyle(createHeaderStyle(workbook));
            
            int rowNum = 5;
            rowNum = addMetricRow(sheet, rowNum, "Total Tasks", String.valueOf(report.getSummary().getTotalTasks()), workbook);
            rowNum = addMetricRow(sheet, rowNum, "Completed Tasks", String.valueOf(report.getSummary().getCompletedTasks()), workbook);
            rowNum = addMetricRow(sheet, rowNum, "Completion Rate", report.getSummary().getCompletionPercentage() + "%", workbook);
           
            long activeBlockers = report.getDailyUpdates() != null 
                ? report.getDailyUpdates().stream()
                    .filter(u -> u.getBlockers() != null && !u.getBlockers().isEmpty() 
                        && !"SOLVED".equals(u.getBlockerStatus()))
                    .count()
                : 0;
            rowNum = addMetricRow(sheet, rowNum, "Active Blockers", String.valueOf(activeBlockers), workbook);
        }
        
       
        if (report.getDailyUpdates() != null && !report.getDailyUpdates().isEmpty()) {
            Row blockersHeader = sheet.createRow(sheet.getLastRowNum() + 2);
            blockersHeader.createCell(0).setCellValue("Active Blockers");
            blockersHeader.getCell(0).setCellStyle(createHeaderStyle(workbook));
            
            Row headersRow = sheet.createRow(sheet.getLastRowNum() + 1);
            String[] headers = {"Date", "Blocker", "Submitted By", "Status"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headersRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }
            
            List<ReportService.DetailedProjectReportDTO.ProgressUpdateDTO> activeBlockers = 
                report.getDailyUpdates().stream()
                    .filter(u -> u.getBlockers() != null && !u.getBlockers().isEmpty() 
                        && !"SOLVED".equals(u.getBlockerStatus()))
                    .collect(Collectors.toList());
            
            int rowNum = sheet.getLastRowNum() + 1;
            if (activeBlockers.isEmpty()) {
                Row emptyRow = sheet.createRow(rowNum);
                emptyRow.createCell(0).setCellValue("No active blockers");
           
                for (int i = 1; i < 4; i++) {
                    emptyRow.createCell(i).setCellValue("-");
                }
            } else {
                for (ReportService.DetailedProjectReportDTO.ProgressUpdateDTO update : activeBlockers) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(update.getUpdateDate() != null ? update.getUpdateDate().toString() : "N/A");
                    row.createCell(1).setCellValue(update.getBlockers());
                    row.createCell(2).setCellValue(update.getSubmittedByName() != null ? update.getSubmittedByName() : "Unknown");
                    
                    org.apache.poi.ss.usermodel.Cell statusCell = row.createCell(3);
                    statusCell.setCellValue("OPEN");
                    CellStyle statusStyle = workbook.createCellStyle();
                    statusStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
                    statusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    statusCell.setCellStyle(statusStyle);
                }
            }
        }
        
        for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);
        workbook.write(baos);
        return baos.toByteArray();
        
    } catch (Exception e) {
        throw new RuntimeException("Error generating Excel: " + e.getMessage(), e);
    }
}

private CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return style;
}
}