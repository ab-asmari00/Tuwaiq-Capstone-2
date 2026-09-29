package com.example.jura.Service;

import com.example.jura.Model.Appointment;
import com.example.jura.Model.User;
import com.example.jura.Repository.UserRepository;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.util.HtmlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentEmailService {

    private final GmailService gmailService;
    private final UserRepository userRepository;

    @Value("${jura.frontend-url:http://localhost:8080/}")
    private String frontendUrl = "http://localhost:8080/";

    public void notifyRequested(Appointment appointment) {
        User patient = userRepository.findById(appointment.getPatientId()).orElse(null);
        User doctor = userRepository.findById(appointment.getDoctorId()).orElse(null);
        if (patient == null || doctor == null) {
            log.warn("Appointment request email recipients were not found");
            return;
        }
        // Capture message values before commit; no database reads in the callback.
        String patientEmail = patient.getEmail();
        String doctorEmail = doctor.getEmail();
        String details = details(appointment);
        String patientMessage = "مرحباً " + patient.getName() + "،\n\n"
                + "تم إرسال طلب موعدك مع الطبيب " + doctor.getName() + ". الطلب بانتظار الموافقة.\n"
                + details + "\n\nافتح جرعة لمتابعة الطلب:\n" + frontendUrl;
        String doctorMessage = "مرحباً دكتور " + doctor.getName() + "،\n\n"
                + "لديك طلب موعد جديد من " + patient.getName() + ".\n"
                + details + "\n\nسجّل الدخول إلى جرعة لمراجعة الطلب:\n" + frontendUrl;
        String patientHtml = buildHtml(appointment, patient.getName(), doctor.getName(), false, false);
        String doctorHtml = buildHtml(appointment, doctor.getName(), patient.getName(), true, false);
        afterCommit(() -> {
            sendSafely(patientEmail, "جرعة — تم إرسال طلب موعدك", patientMessage, patientHtml);
            sendSafely(doctorEmail, "جرعة — طلب موعد جديد", doctorMessage, doctorHtml);
        });
    }

    public void notifyApproved(Appointment appointment) {
        User patient = userRepository.findById(appointment.getPatientId()).orElse(null);
        User doctor = userRepository.findById(appointment.getDoctorId()).orElse(null);
        if (patient == null || doctor == null) {
            log.warn("Appointment approval email recipients were not found");
            return;
        }
        String recipient = patient.getEmail();
        String message = "مرحباً " + patient.getName() + "،\n\n"
                + "تمت الموافقة على موعدك مع الطبيب " + doctor.getName() + ".\n"
                + details(appointment) + "\n\n"
                + "افتح جرعة، ثم الاستشارات. يتاح زر الانضمام خلال وقت الموعد فقط.\n"
                + frontendUrl;
        String html = buildHtml(appointment, patient.getName(), doctor.getName(), false, true);
        afterCommit(() -> sendSafely(recipient, "جرعة — تمت الموافقة على موعدك", message, html));
    }

    private String buildHtml(Appointment appointment, String recipientName, String otherName,
                             boolean doctor, boolean approved) {
        String template;
        try (var input = new ClassPathResource("templates/email/appointment.html").getInputStream()) {
            template = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            log.warn("Appointment HTML template unavailable; using plain text");
            return null;
        }
        String title = approved ? "موعدك مؤكد" : doctor ? "لديك طلب موعد جديد" : "وصلنا طلب موعدك";
        String intro = approved ? "وافق الطبيب على طلبك، وتم تجهيز الاجتماع للاستشارة."
                : doctor ? "أرسل أحد المرضى طلب استشارة. راجع التفاصيل ثم وافق على الطلب أو ارفضه من المنصة."
                : "تم إرسال طلبك إلى الطبيب بنجاح. سنرسل لك إشعاراً بالبريد عند الموافقة على الموعد.";
        java.util.Map<String, String> values = new java.util.HashMap<>();
        values.put("PREHEADER", title);
        values.put("STATUS_BG", approved ? "#e4f5eb" : "#fff3db");
        values.put("STATUS_COLOR", approved ? "#17603e" : "#815719");
        values.put("STATUS", approved ? "تمت الموافقة" : "بانتظار الموافقة");
        values.put("TITLE", title);
        values.put("GREETING", "مرحباً " + (doctor ? "دكتور " : "") + recipientName + "،");
        values.put("INTRO", intro);
        values.put("ID", String.valueOf(appointment.getId()));
        values.put("PERSON_LABEL", doctor ? "المريض" : "الطبيب");
        values.put("PERSON", otherName);
        values.put("DATE", appointment.getStartAt().format(DateTimeFormatter.ofPattern("dd / MM / yyyy")));
        values.put("TIME", appointment.getStartAt().format(DateTimeFormatter.ofPattern("HH:mm"))
                + " – " + appointment.getEndAt().format(DateTimeFormatter.ofPattern("HH:mm"))
                + (appointment.getEndAt().toLocalDate().equals(appointment.getStartAt().toLocalDate()) ? ""
                : " (" + appointment.getEndAt().format(DateTimeFormatter.ofPattern("dd / MM / yyyy")) + ")"));
        values.put("NOTE", approved ? "سجّل الدخول ثم افتح الاستشارات. يظهر زر الانضمام خلال وقت الموعد فقط."
                : doctor ? "هذا طلب جديد ولم يتم تأكيد الموعد بعد. يمكنك مراجعته من صفحة المواعيد."
                : "الموعد غير مؤكد حتى موافقة الطبيب. يمكنك متابعة حالة الطلب من صفحة الاستشارات.");
        values.put("BUTTON", doctor ? "مراجعة طلب الموعد" : "فتح الاستشارات في جرعة");
        values.put("URL", frontendUrl);
        // Replace original placeholders once so user-supplied text cannot become another placeholder.
        var matcher = java.util.regex.Pattern.compile("\\{\\{([A-Z_]+)\\}\\}").matcher(template);
        StringBuilder html = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(html, java.util.regex.Matcher.quoteReplacement(
                    HtmlUtils.htmlEscape(values.getOrDefault(matcher.group(1), ""))));
        }
        matcher.appendTail(html);
        return html.toString();
    }

    private String details(Appointment appointment) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return "رقم الطلب: " + appointment.getId()
                + "\nالبداية: " + appointment.getStartAt().format(format)
                + "\nالنهاية: " + appointment.getEndAt().format(format)
                + "\nجميع الأوقات بتوقيت السعودية.";
    }

    private void afterCommit(Runnable notification) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notification.run();
                }
            });
        } else {
            notification.run(); // Repository save already committed when there is no outer transaction.
        }
    }

    private void sendSafely(String recipient, String subject, String message, String html) {
        try {
            gmailService.sendEmail(recipient, subject, message, html);
        } catch (RuntimeException exception) {
            log.warn("Appointment email failed ({}); appointment remains saved",
                    exception.getClass().getSimpleName());
        }
    }
}
