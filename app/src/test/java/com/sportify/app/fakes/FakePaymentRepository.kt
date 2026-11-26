package com.sportify.app.fakes

import com.sportify.app.domain.model.PaymentSession
import com.sportify.app.domain.repository.PaymentRepository

class FakePaymentRepository : PaymentRepository {
    var sessionResult: Result<PaymentSession> = Result.success(PaymentSession("secret_123", "pk_test_abc"))
    var confirmResult: Result<Unit> = Result.success(Unit)
    val sessionRequests = mutableListOf<String>()
    val confirmRequests = mutableListOf<String>()

    override suspend fun createPaymentSession(bookingId: String): Result<PaymentSession> {
        sessionRequests += bookingId
        return sessionResult
    }

    override suspend fun confirmPayment(bookingId: String): Result<Unit> {
        confirmRequests += bookingId
        return confirmResult
    }
}
