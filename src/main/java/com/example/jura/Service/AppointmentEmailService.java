package com.example.jura.Service;

import com.example.jura.Model.Appointment;
import com.example.jura.Model.User;
import com.example.jura.Repository.UserRepository;
import java.time.format.DateTimeFormatter;
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
        User patient = userRepository.findUserById(appointment.getPatientId());
        User doctor = userRepository.findUserById(appointment.getDoctorId());
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
        afterCommit(() -> {
            sendSafely(patientEmail, "جرعة — تم إرسال طلب موعدك", patientMessage);
            sendSafely(doctorEmail, "جرعة — طلب موعد جديد", doctorMessage);
        });
    }

    public void notifyApproved(Appointment appointment) {
        User patient = userRepository.findUserById(appointment.getPatientId());
        User doctor = userRepository.findUserById(appointment.getDoctorId());
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
        afterCommit(() -> sendSafely(recipient, "جرعة — تمت الموافقة على موعدك", message));
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

    private void sendSafely(String recipient, String subject, String message) {
        try {
            if (!gmailService.sendEmail(recipient, subject, message)) {
                log.warn("Appointment email was not sent; appointment remains saved");
            }
        } catch (RuntimeException exception) {
            log.warn("Appointment email failed ({}); appointment remains saved",
                    exception.getClass().getSimpleName());
        }
    }
}
