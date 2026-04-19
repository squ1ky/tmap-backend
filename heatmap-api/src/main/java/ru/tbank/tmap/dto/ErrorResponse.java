package ru.tbank.tmap.dto;

import ru.tbank.tmap.exception.ErrorCode;

public record ErrorResponse(ErrorCode code, String message) {
}
