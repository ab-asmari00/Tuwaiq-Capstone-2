<p align="center">
  <img src="src/main/resources/static/logo.png" alt="Jur’a logo" width="140">
</p>

# جرعة | Jur’a

A Saudi-focused personal health companion built as a course project. Manage medications, supplements, dose schedules, and virtual consultations through an Arabic/English web interface.

## Features

- Search Saudi medications using a locally synced SFDA catalogue.
- Manage medications and supplements with multiple ingredients.
- Request AI interaction assessments through Gemini.
- Track taken, skipped, and missed doses, with in-app reminders and adherence summaries.
- Request doctor appointments, create Zoom meetings after approval, and join during the appointment window.
- Send appointment request and approval notifications through Gmail SMTP.

## Tech Stack

Java 17+, Spring Boot, Spring Data JPA, Validation, Lombok, Spring Mail, MySQL, and HTML/CSS/JavaScript. SFDA, Gemini, and Zoom integrations use `RestClient`; email uses SMTP.

## Run Locally

1. Create a MySQL database named `jura` and configure the connection in `src/main/resources/application.properties`.
2. Set `GEMINI_API_KEY` for interaction checks. For Zoom, set `ZOOM_ACCOUNT_ID`, `ZOOM_CLIENT_ID`, `ZOOM_CLIENT_SECRET`, and `ZOOM_HOST_USER_ID`. For Gmail SMTP, set `GMAIL_SENDER_EMAIL` and `GMAIL_APP_PASSWORD` (a Google App Password with 2-Step Verification enabled). Set `JURA_FRONTEND_URL` when the frontend address differs from localhost. Keep credentials outside version control.
3. Start the application:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Open [http://localhost:8080](http://localhost:8080). Create a patient or doctor account; doctors should complete their professional profile.
5. Before searching medications, sync the catalogue using `POST /api/v1/drugs/sync` in Insomnia or another API client.

## Prototype Limits

AI assessments are informational, may be incorrect, and do not establish medication safety. Login currently uses plain passwords without server-side authentication or authorization. Use demonstration data only; this prototype is not ready for production or clinical use.

Reminders work while the application is open. Deleting an appointment removes local records but does not cancel its remote Zoom meeting.
