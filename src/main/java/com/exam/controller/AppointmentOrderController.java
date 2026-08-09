package com.exam.controller;

import com.exam.entity.Faculty;
import com.exam.service.AppointmentOrderService;
import com.exam.service.FacultyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentOrderController {

    private final AppointmentOrderService appointmentOrderService;
    private final FacultyService facultyService;

    public AppointmentOrderController(AppointmentOrderService appointmentOrderService, FacultyService facultyService) {
        this.appointmentOrderService = appointmentOrderService;
        this.facultyService = facultyService;
    }

    @GetMapping("/order/{facultyId}")
    public ResponseEntity<byte[]> generateOrder(
            @PathVariable UUID facultyId,
            @RequestParam String role,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @RequestParam(required = false) String board,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) Integer noOfCandidates,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String internalExaminerName,
            @RequestParam(required = false) String internalExaminerPhone) {
        try {
            Optional<Faculty> facultyOpt = facultyService.getById(facultyId);
            if (facultyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Faculty faculty = facultyOpt.get();
            byte[] wordDoc = appointmentOrderService.generateAppointmentOrderWord(
                    faculty, role, season, date,
                    board, time, venue,
                    subjectCode, subjectName, noOfCandidates,
                    semester, internalExaminerName, internalExaminerPhone);

            String safeRole = role.replaceAll("[^a-zA-Z0-9]", "_");
            String filename = "Appointment_" + safeRole + "_" + faculty.getName().replace(" ", "_") + ".docx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(wordDoc);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
