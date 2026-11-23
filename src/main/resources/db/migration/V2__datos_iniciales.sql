INSERT INTO clientes (dni, nombre, apellido1, apellido2, fecha_nacimiento) VALUES
    ('11111111H', 'Juan', 'Pérez', 'López', DATE '1959-09-12'),
    ('22222222J', 'Raúl', 'Canales', 'Rodríguez', DATE '1985-03-01'),
    ('33333333P', 'Elena', 'Ruiz', 'Herrera', DATE '2010-05-10'),
    ('44444444A', 'Raquel', 'Ruiz', 'Herrera', DATE '2002-06-21'),
    ('55555555K', 'María', 'Sánchez', 'Torres', DATE '1999-08-08');

INSERT INTO cuentas_bancarias (id, dni_cliente, tipo_cuenta, total) VALUES
    (1, '11111111H', 'PREMIUM', 150000.00),
    (2, '11111111H', 'NORMAL', 20000.00),
    (3, '22222222J', 'NORMAL', 50000.00),
    (4, '22222222J', 'JUNIOR', 300.00),
    (5, '33333333P', 'JUNIOR', 300.00),
    (6, '44444444A', 'NORMAL', 75000.00),
    (7, '55555555K', 'PREMIUM', 120000.00);
