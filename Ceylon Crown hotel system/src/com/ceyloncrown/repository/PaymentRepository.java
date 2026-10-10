package com.ceyloncrown.repository;

import com.ceyloncrown.model.Payment;
import java.nio.file.Path;

public class PaymentRepository extends FileRepository<Payment> {
    public PaymentRepository(Path file) { super(file); }
    @Override protected Payment fromRecord(String[] f) { return Payment.fromRecord(f); }
}