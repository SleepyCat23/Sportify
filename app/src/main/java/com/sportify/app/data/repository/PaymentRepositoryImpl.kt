package com.sportify.app.data.repository

import com.google.firebase.functions.FirebaseFunctions
import com.sportify.app.domain.model.PaymentSession
import com.sportify.app.domain.repository.PaymentRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The amount is always computed server-side from the court price; the client never
 * sends a price. Secrets (Stripe secret key) live only in Cloud Functions.
 */
@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions
) : PaymentRepository {

    override suspend fun createPaymentSession(bookingId: String): Result<PaymentSession> = runCatching {
        val res = functions.getHttpsCallable("createPaymentIntent")
            .call(hashMapOf("bookingId" to bookingId)).await()
        val data = res.data as Map<*, *>
        PaymentSession(
            clientSecret = data["clientSecret"] as String,
            publishableKey = data["publishableKey"] as String
        )
    }

    override suspend fun confirmPayment(bookingId: String): Result<Unit> = runCatching {
        functions.getHttpsCallable("confirmBookingPayment")
            .call(hashMapOf("bookingId" to bookingId)).await()
        Unit
    }
}
