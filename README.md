<p align="center">
  <img src="src/main/resources/static/logo.png" alt="Jur’a logo" width="140">
</p>

# جرعة | Jur’a

A Saudi-focused personal health companion built as a course project. Manage medications, supplements, dose schedules, and virtual consultations through an Arabic/English web interface.

## Integrations

| Integration | Purpose |
|---|---|
| **Saudi Food and Drug Authority (SFDA)** | Retrieves Saudi drug catalogue data, including trade names and scientific ingredients. |
| **Zoom** | Creates consultation meetings when appointments are approved. |
| **Gemini** | Assesses potential interactions between the patient's active medications and supplements, using ingredients and medical conditions. |
| **Gmail** | Sends appointment request notifications to patient and doctor, and approval notifications to the patient through SMTP. |

## Features

- Search Saudi medications using a locally synced SFDA catalogue.
- Manage medications and supplements with multiple ingredients.
- Request AI interaction assessments through Gemini.
- Track taken, skipped, and missed doses, with in-app reminders and adherence summaries.
- Request doctor appointments, create Zoom meetings after approval, and join during the appointment window.
- Send appointment request and approval notifications through Gmail SMTP.

## Tech Stack

Java 17+, Spring Boot, Spring Data JPA, Validation, Lombok, Spring Mail, MySQL, and HTML/CSS/JavaScript. SFDA, Gemini, and Zoom integrations use `RestClient`; email uses SMTP.

## Error Handling

Services throw `ApiException` for invalid requests, missing records, conflicts, and integration failures. The global `ControllerAdvice` returns HTTP `400` with a descriptive `message`. Controllers return success responses after service operations complete. Appointment email failures are logged without undoing saved appointments.

## API Endpoints

All paths below start with `/api/v1`.

### Users

| Method | Path | Purpose |
|---|---|---|
| POST | `/user/add` | Register a patient or doctor. |
| POST | `/user/login` | Check email and password and return user identity. |
| GET | `/user/getAll` | List all users. |
| GET | `/user/get-by-id/{id}` | Retrieve a user. |
| PUT | `/user/update/{id}` | Update user information. |
| DELETE | `/user/delete/{id}` | Delete a user and associated local records. |

### Doctor Profiles

| Method | Path | Purpose |
|---|---|---|
| POST | `/doctor/add` | Create a profile for an existing doctor user. |
| GET | `/doctor/getAll` | List doctor profiles. |
| GET | `/doctor/get-by-user-id/{userId}` | Retrieve a doctor's profile. |
| GET | `/doctor/search?specialty=…` | Search doctors by specialty. |
| PUT | `/doctor/update/{userId}` | Update specialty and biography. |
| DELETE | `/doctor/delete/{userId}` | Delete the profile and associated appointments; keep the user account. |

### Drug Catalogue

| Method | Path | Purpose |
|---|---|---|
| GET | `/drugs/search?q=…&page=1` | Search the local drug catalogue using Arabic or English names, with pagination. |

### Drug Cache CRUD

| Method | Path | Purpose |
|---|---|---|
| POST | `/drug-cache/add` | Manually create a cached drug record. |
| GET | `/drug-cache/getAll` | List cached drug records. |
| GET | `/drug-cache/get-by-id/{id}` | Retrieve a cached drug record. |
| PUT | `/drug-cache/update/{id}` | Update an unreferenced cache record. |
| DELETE | `/drug-cache/delete/{id}` | Delete an unreferenced cache record. |

### Patient Medications and Supplements

| Method | Path | Purpose |
|---|---|---|
| POST | `/user-item/add` | Add a medication from the drug cache to a patient's list. |
| POST | `/user-item/add-supplement` | Add a supplement with one or more ingredients. |
| GET | `/user-item/getAll` | List all patient items. |
| GET | `/user-item/get-by-id/{id}` | Retrieve an item. |
| GET | `/user-item/get-active-by-user/{userId}` | Retrieve a patient's active medications and supplements. |
| PUT | `/user-item/update/{id}` | Update item details or active status. |
| DELETE | `/user-item/delete/{id}` | Delete an item and its dependent records. |

### Item Ingredients

| Method | Path | Purpose |
|---|---|---|
| POST | `/item-ingredient/add` | Add an ingredient to a supplement. |
| GET | `/item-ingredient/getAll` | List ingredient records. |
| GET | `/item-ingredient/get-by-id/{id}` | Retrieve an ingredient record. |
| GET | `/item-ingredient/get-by-item/{itemId}` | Retrieve an item's ingredients. |
| PUT | `/item-ingredient/update/{id}` | Update a supplement ingredient. |
| DELETE | `/item-ingredient/delete/{id}` | Delete a supplement ingredient while retaining at least one. |
| POST | `/item-ingredient/sync-drug/{itemId}` | Rebuild a medication's ingredients from its linked drug cache. |

### Interactions

| Method | Path | Purpose |
|---|---|---|
| POST | `/interaction/check/{itemId}` | Assess the selected item against the patient's other active items through Gemini and save results. |
| GET | `/interaction/getAll` | List saved interaction results. |
| GET | `/interaction/get-by-id/{id}` | Retrieve a result. |
| GET | `/interaction/get-by-item/{itemId}` | Retrieve interaction history involving an item. |
| POST | `/interaction/add` | Create a manual interaction record for course CRUD. |
| PUT | `/interaction/update/{id}` | Manually update a record without changing its item pair. |
| DELETE | `/interaction/delete/{id}` | Delete a result. |

### Dose Schedules and Reminders

| Method | Path | Purpose |
|---|---|---|
| POST | `/dose-schedule/add` | Create an intake schedule for an item. |
| GET | `/dose-schedule/getAll` | List schedules. |
| GET | `/dose-schedule/get-by-id/{id}` | Retrieve a schedule. |
| GET | `/dose-schedule/get-by-item/{itemId}` | Retrieve an item's schedules. |
| PUT | `/dose-schedule/update/{id}` | Update schedule time, days, and date range. |
| DELETE | `/dose-schedule/delete/{id}` | Delete a schedule and its dose logs. |
| GET | `/dose-schedule/today/{userId}` | Retrieve today's doses and their status; record sufficiently overdue doses as MISSED. |
| GET | `/dose-schedule/reminders/{userId}` | Retrieve pending doses currently within the reminder window. |

### Dose Logs and Adherence

| Method | Path | Purpose |
|---|---|---|
| POST | `/dose-log/add` | Record a scheduled dose as TAKEN, SKIPPED, or MISSED. |
| GET | `/dose-log/getAll` | List dose logs. |
| GET | `/dose-log/get-by-id/{id}` | Retrieve a dose log. |
| GET | `/dose-log/get-by-schedule/{scheduleId}` | Retrieve a schedule's dose history. |
| GET | `/dose-log/get-by-item/{itemId}` | Retrieve an item's dose history. |
| PUT | `/dose-log/update/{id}` | Correct a recorded dose's status or taken time. |
| DELETE | `/dose-log/delete/{id}` | Delete a dose log. |
| GET | `/dose-log/adherence-by-user/{userId}` | Summarize taken, skipped, and missed logs and the taken percentage. |

### Appointments

| Method | Path | Purpose |
|---|---|---|
| POST | `/appointment/add` | Submit a pending consultation request and email patient and doctor. |
| GET | `/appointment/getAll` | List appointments. |
| GET | `/appointment/get-by-id/{id}` | Retrieve an appointment. |
| GET | `/appointment/get-by-patient/{patientId}` | Retrieve a patient's appointments. |
| GET | `/appointment/get-by-doctor/{doctorId}` | Retrieve a doctor's appointments. |
| GET | `/appointment/get-pending-by-doctor/{doctorId}` | Retrieve requests awaiting the doctor's decision. |
| PUT | `/appointment/update/{id}` | Change a pending request's start and end times. |
| PUT | `/appointment/approve/{id}` | Create a Zoom meeting, approve the appointment, and email the patient. |
| PUT | `/appointment/reject/{id}` | Reject a pending request without sending an email. |
| DELETE | `/appointment/delete/{id}` | Delete the appointment and its local meeting record. |

### Meetings

| Method | Path | Purpose |
|---|---|---|
| GET | `/meeting/getAll` | List meeting summaries without join URLs. |
| GET | `/meeting/get-by-id/{id}` | Retrieve a meeting summary. |
| GET | `/meeting/get-by-appointment/{appointmentId}` | Retrieve an appointment's meeting summary. |
| GET | `/meeting/join/{appointmentId}` | Return the join URL during an approved appointment's scheduled window. |
| POST | `/meeting/add` | Manually create a local meeting record for an approved appointment without one. |
| PUT | `/meeting/update/{id}` | Update local meeting details. |
| DELETE | `/meeting/delete/{id}` | Delete a local meeting record without cancelling the remote Zoom room. |

## Prototype Limits

AI assessments are informational, may be incorrect, and do not establish medication safety. Login currently uses plain passwords without server-side authentication or authorization. Use demonstration data only; this prototype is not ready for production or clinical use.

Reminders work while the application is open. Deleting an appointment removes local records but does not cancel its remote Zoom meeting.
