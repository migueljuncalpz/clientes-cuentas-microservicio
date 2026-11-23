package com.zarahome.cuentas.infrastructure.rest;

import com.zarahome.cuentas.domain.ErrorDominio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ErrorDominio.class)
    ResponseEntity<Object> manejarDominio(ErrorDominio error, WebRequest request) {
        HttpStatus estado = error.tipo() == ErrorDominio.Tipo.NO_ENCONTRADO
                ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return respuesta(estado, error.codigo(), error.getMessage(), request);
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<Object> manejarConflicto(RuntimeException error, WebRequest request) {
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO_CONCURRENCIA",
                "La operación entra en conflicto con otra escritura. Consulta el estado antes de repetirla.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> manejarInesperado(Exception error, WebRequest request) {
        LOG.error("Error inesperado al procesar la petición", error);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "No se ha podido completar la operación.", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException error,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "Hay campos inválidos en la solicitud.", request);
        Map<String, String> campos = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors()
                .forEach(field -> campos.putIfAbsent(field.getField(), field.getDefaultMessage()));
        problema.setProperty("errores", campos);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException error,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "JSON_INVALIDO",
                "El cuerpo JSON no es válido o contiene campos o valores no admitidos.", request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException error,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", "El parámetro no tiene el formato esperado.", request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception error, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = problema(status, "HTTP_" + status.value(),
                "La solicitud no se puede procesar con el recurso, método o formato indicado.", request);
        return new ResponseEntity<>(problema, headers, status);
    }

    private ResponseEntity<Object> respuesta(HttpStatus status, String codigo, String detalle, WebRequest request) {
        return ResponseEntity.status(status).body(problema(status, codigo, detalle, request));
    }

    private ProblemDetail problema(HttpStatusCode status, String codigo, String detalle, WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setProperty("codigo", codigo);
        problema.setProperty("correlationId", MDC.get("correlationId"));
        if (request instanceof ServletWebRequest servletRequest) {
            problema.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
        return problema;
    }
}
