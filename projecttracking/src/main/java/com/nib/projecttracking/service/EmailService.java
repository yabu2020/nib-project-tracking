package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserNotificationService notificationService;

    @Autowired
    private ActivityLogService activityLogService; 

    @Value("${app.email.from:noreply@nib.com}")
    private String fromEmail;

    @Value("${app.email.from-name:NIB IT Project Tracking}")
    private String fromName;

    @Value("${app.base-url:http://localhost:5174}")
    private String baseUrl;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        return null;
    }

    /**
     * Send overdue task notification (email + in-app)
     */
    @Async
    public void sendOverdueTaskAlert(String toEmail, Long userId, String userName,
                                     String taskName, String projectName, String dueDate) {

        String subject = "⚠️ Overdue Task Alert: " + taskName;
        String htmlContent = buildOverdueTaskEmail(userName, taskName, projectName, dueDate);

        boolean emailSuccess = sendHtmlEmail(toEmail, subject, htmlContent);

        User actor = getCurrentUser(); 
        String details = String.format("Sent overdue task alert to %s (%s) for task '%s' in project '%s' (due: %s)",
                userName, toEmail, taskName, projectName, dueDate);

        activityLogService.logAction(
                actor,
                emailSuccess ? "EMAIL_SENT_OVERDUE_TASK" : "EMAIL_FAILED_OVERDUE_TASK",
                "Task",
                null,  
                details
        );

        if (userId != null) {
            notificationService.createNotification(
                    new User() {{ setId(userId); }},
                    "⚠️ Task Overdue",
                    "The task \"" + taskName + "\" in project \"" + projectName + "\" is overdue (was due: " + dueDate + ")",
                    "TASK_OVERDUE",
                    "HIGH",
                    "Task",
                    null
            );
            System.out.println("🔔 In-app notification created for user " + userId);
        }
    }

    /**
     * Send overdue milestone notification (email + in-app)
     */
    @Async
    public void sendOverdueMilestoneAlert(String toEmail, Long userId, String userName,
                                          String milestoneName, String projectName, String dueDate) {

        String subject = "🎯 Overdue Milestone Alert: " + milestoneName;
        String htmlContent = buildOverdueMilestoneEmail(userName, milestoneName, projectName, dueDate);

        boolean emailSuccess = sendHtmlEmail(toEmail, subject, htmlContent);
        User actor = getCurrentUser();
        String details = String.format("Sent overdue milestone alert to %s (%s) for milestone '%s' in project '%s' (due: %s)",
                userName, toEmail, milestoneName, projectName, dueDate);

        activityLogService.logAction(
                actor,
                emailSuccess ? "EMAIL_SENT_OVERDUE_MILESTONE" : "EMAIL_FAILED_OVERDUE_MILESTONE",
                "Milestone",
                null,
                details
        );
        if (userId != null) {
            notificationService.createNotification(
                    new User() {{ setId(userId); }},
                    "🎯 Milestone Overdue",
                    "The milestone \"" + milestoneName + "\" in project \"" + projectName + "\" is overdue (was due: " + dueDate + ")",
                    "MILESTONE_OVERDUE",
                    "HIGH",
                    "Milestone",
                    null
            );
            System.out.println("🔔 In-app notification created for user " + userId);
        }
    }

    /**
     * Send HTML email and return success status
     */
    private boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("✅ Email sent to: " + to + " | Subject: " + subject);
            return true;

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email to " + to + ": " + e.getMessage());
            e.printStackTrace();
            User actor = getCurrentUser();
            activityLogService.logAction(
                    actor,
                    "EMAIL_SEND_FAILED",
                    "Email",
                    null,
                    "Failed to send '" + subject + "' to " + to + ": " + e.getMessage()
            );

            return false;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error sending email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Build HTML email for overdue task
     */
    private String buildOverdueTaskEmail(String userName, String taskName, String projectName, String dueDate) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #dc3545; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background: #f8f9fa; padding: 20px; border: 1px solid #dee2e6; border-radius: 0 0 8px 8px; }
                    .task-info { background: white; padding: 15px; margin: 15px 0; border-left: 4px solid #dc3545; border-radius: 4px; }
                    .btn { display: inline-block; background: #003366; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h2>⚠️ Task Overdue</h2></div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>This is a reminder that the following task is now <strong style="color: #dc3545;">OVERDUE</strong>:</p>
                        <div class="task-info">
                            <p><strong>📋 Task:</strong> %s</p>
                            <p><strong>📁 Project:</strong> %s</p>
                            <p><strong>📅 Due Date:</strong> %s</p>
                            <p><strong>⏰ Status:</strong> <span style="color: #dc3545; font-weight: bold;">OVERDUE</span></p>
                        </div>
                        <p>Please update the task status or contact your project manager if you need assistance.</p>
                        <a href="%s/tasks" class="btn">View My Tasks</a>
                        <p style="margin-top: 20px;">Best regards,<br><strong>NIB IT Project Tracking Team</strong></p>
                    </div>
                    <div class="footer">
                        <p>This is an automated notification. Please do not reply to this email.</p>
                        <p>© 2026 National IT Bank - Project Tracking System</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, taskName, projectName, dueDate, baseUrl);
    }

    /**
     * Build HTML email for overdue milestone
     */
    private String buildOverdueMilestoneEmail(String userName, String milestoneName, String projectName, String dueDate) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #ffc107; color: #000; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background: #f8f9fa; padding: 20px; border: 1px solid #dee2e6; border-radius: 0 0 8px 8px; }
                    .milestone-info { background: white; padding: 15px; margin: 15px 0; border-left: 4px solid #ffc107; border-radius: 4px; }
                    .btn { display: inline-block; background: #003366; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h2>🎯 Milestone Overdue</h2></div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>This is a reminder that the following milestone is now <strong style="color: #dc3545;">OVERDUE</strong>:</p>
                        <div class="milestone-info">
                            <p><strong>🎯 Milestone:</strong> %s</p>
                            <p><strong>📁 Project:</strong> %s</p>
                            <p><strong>📅 Target Date:</strong> %s</p>
                            <p><strong>⏰ Status:</strong> <span style="color: #dc3545; font-weight: bold;">OVERDUE</span></p>
                        </div>
                        <p>Please review the milestone progress and update accordingly.</p>
                        <a href="%s/milestones" class="btn">View Milestones</a>
                        <p style="margin-top: 20px;">Best regards,<br><strong>NIB IT Project Tracking Team</strong></p>
                    </div>
                    <div class="footer">
                        <p>This is an automated notification. Please do not reply to this email.</p>
                        <p>© 2026 National IT Bank - Project Tracking System</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, milestoneName, projectName, dueDate, baseUrl);
    }

    /**
     * Test email method (useful for debugging)
     */
    @Async
    public void sendTestEmail(String toEmail) {
        String subject = "✅ NIB Project Tracking - Email Test";
        String htmlContent = String.format("""
            <html><body>
            <h2>✅ Email Configuration Test Successful!</h2>
            <p>This is a test email from NIB IT Project Tracking System.</p>
            <p>If you received this, your email notifications are working correctly.</p>
            <p><strong>Time:</strong> %s</p>
            </body></html>
            """, LocalDateTime.now());

        boolean success = sendHtmlEmail(toEmail, subject, htmlContent);

        User actor = getCurrentUser();
        activityLogService.logAction(
                actor,
                success ? "EMAIL_TEST_SENT" : "EMAIL_TEST_FAILED",
                "Email",
                null,
                "Test email sent to " + toEmail + (success ? " successfully" : " FAILED")
        );
    }
}