package com.SIGMA.USCO.notifications.controller;

import com.SIGMA.USCO.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notificaciones", description = "Operaciones sobre notificaciones del usuario")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Obtener notificaciones", description = "Obtiene todas las notificaciones del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Lista de notificaciones")
    @GetMapping
    public ResponseEntity<?> getMyNotifications() {
        return notificationService.getMyNotifications();
    }

    @Operation(summary = "Obtener cantidad de no leídas", description = "Obtiene el número de notificaciones no leídas")
    @ApiResponse(responseCode = "200", description = "Cantidad de no leídas")
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        return notificationService.getUnreadCount();
    }

    @Operation(summary = "Detalle de notificación", description = "Obtiene el detalle de una notificación específica")
    @ApiResponse(responseCode = "200", description = "Detalle de la notificación")
    @GetMapping("/{notificationId}")
    public ResponseEntity<?> getNotificationDetail(@Parameter(description = "ID de la notificación") @PathVariable Long notificationId) {
        return notificationService.getNotificationDetail(notificationId);
    }

    @Operation(summary = "Marcar como leída", description = "Marca una notificación como leída")
    @ApiResponse(responseCode = "200", description = "Notificación marcada como leída")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@Parameter(description = "ID de la notificación") @PathVariable Long notificationId) {
        return notificationService.markAsRead(notificationId);
    }
}
