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
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        try {
            Optional<Faculty> facultyOpt = facultyService.getById(facultyId);
            if (facultyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Faculty faculty = facultyOpt.get();
            byte[] wordDoc = appointmentOrderService.generateAppointmentOrderWord(faculty, role, season, date);

            String filename = "Appointment_Order_" + faculty.getName().replace(" ", "_") + ".docx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(wordDoc);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
