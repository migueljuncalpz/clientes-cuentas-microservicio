package com.zarahome.cuentas.infrastructure.persistence;

import com.zarahome.cuentas.application.ConflictoPersistenciaException;
import com.zarahome.cuentas.application.port.out.Transacciones;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class TransaccionesSpring implements Transacciones {
    private final TransactionTemplate lectura;
    private final TransactionTemplate escritura;

    public TransaccionesSpring(PlatformTransactionManager gestor) {
        lectura = new TransactionTemplate(gestor);
        lectura.setReadOnly(true);
        escritura = new TransactionTemplate(gestor);
    }

    @Override
    public <T> T consultar(Supplier<T> operacion) {
        return lectura.execute(status -> operacion.get());
    }

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        try {
            return escritura.execute(status -> operacion.get());
        } catch (DataIntegrityViolationException conflicto) {
            throw new ConflictoPersistenciaException(conflicto);
        }
    }
}
