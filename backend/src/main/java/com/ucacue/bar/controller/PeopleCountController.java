package com.ucacue.bar.controller;

import com.ucacue.bar.service.PeopleCountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/people-counts")
@RequiredArgsConstructor
@Tag(name = "People Counts", description = "Registro de afluencia YOLO")
public class PeopleCountController {

    private final PeopleCountService peopleCountService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar evento de afluencia")
    public ResponseEntity<Void> register(@RequestBody @jakarta.validation.Valid PeopleCountRequest request) {
        java.time.LocalDateTime timestamp = request.timestamp() != null ? request.timestamp() : java.time.LocalDateTime.now();
        peopleCountService.recordPeopleCount(timestamp, request.count());
        return ResponseEntity.accepted().build();
    }

    public record PeopleCountRequest(
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime timestamp,

        @Min(0)
        int count
    ) {}
}
