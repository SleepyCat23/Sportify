const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { defineSecret, defineString } = require("firebase-functions/params");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

const STRIPE_SECRET = defineSecret("STRIPE_SECRET_KEY");
const STRIPE_WEBHOOK_SECRET = defineSecret("STRIPE_WEBHOOK_SECRET");
const STRIPE_PUBLISHABLE = defineString("STRIPE_PUBLISHABLE_KEY");

const stripeClient = () => require("stripe")(STRIPE_SECRET.value());
const slotIds = (b) =>
  Array.from({ length: b.durationHours }, (_, i) => `${b.courtId}_${b.date}_${b.startHour + i}`);

// ---------- helpers ----------
async function notify(uid, title, body, data = {}) {
  const user = await db.collection("users").doc(uid).get();
  const token = user.exists && user.data().fcmToken;
  if (!token) return;
  try {
    await admin.messaging().send({
      token,
      notification: { title, body },
      data,
      android: { priority: "high", notification: { channelId: "bookings" } },
    });
  } catch (e) {
    console.warn("FCM failed", e.code);
    if (e.code === "messaging/registration-token-not-registered")
      await user.ref.update({ fcmToken: "" });
  }
}

/** Idempotently confirms a booking after a verified successful payment. */
async function markPaid(bookingId, paymentIntentId) {
  const ref = db.collection("bookings").doc(bookingId);
  let booking = null;
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) return;
    const b = snap.data();
    if (b.status === "CONFIRMED") return; // already done
    if (b.status === "CANCELLED") {
      // Paid but slot was released: refund instead of confirming
      await stripeClient().refunds.create({ payment_intent: paymentIntentId });
      tx.update(ref, { paymentStatus: "REFUNDED" });
      return;
    }
    tx.update(ref, { status: "CONFIRMED", paymentStatus: "PAID", paymentIntentId });
    slotIds(b).forEach((id) =>
      tx.set(db.collection("slots").doc(id), { status: "CONFIRMED", expiresAt: admin.firestore.FieldValue.delete() }, { merge: true })
    );
    booking = b;
  });
  if (booking) {
    await notify(booking.userId, "Booking confirmed ✅",
      `${booking.venueName} · ${booking.courtName} on ${booking.date} at ${String(booking.startHour).padStart(2, "0")}:00`,
      { bookingId });
  }
}

// ---------- callable: create PaymentIntent ----------
exports.createPaymentIntent = onCall({ secrets: [STRIPE_SECRET] }, async (req) => {
  if (!req.auth) throw new HttpsError("unauthenticated", "Please sign in.");
  const bookingId = req.data && req.data.bookingId;
  if (!bookingId) throw new HttpsError("invalid-argument", "bookingId required");

  const snap = await db.collection("bookings").doc(bookingId).get();
  if (!snap.exists) throw new HttpsError("not-found", "Booking not found");
  const b = snap.data();
  if (b.userId !== req.auth.uid) throw new HttpsError("permission-denied", "Not your booking");
  if (b.status !== "PENDING_PAYMENT") throw new HttpsError("failed-precondition", "Booking is not awaiting payment");
  if (b.expiresAt.toMillis() < Date.now()) throw new HttpsError("failed-precondition", "Reservation expired. Please book again.");

  // the slot locks must still belong to this booking
  const slots = await Promise.all(slotIds(b).map((id) => db.collection("slots").doc(id).get()));
  if (slots.some((s) => !s.exists || s.data().bookingId !== bookingId))
    throw new HttpsError("failed-precondition", "Slot is no longer reserved");

  // price is recomputed server-side: never trust the client's totalAmount
  const court = (await db.collection("courts").doc(b.courtId).get()).data();
  const amount = Math.round(court.pricePerHour * b.durationHours * 100); // minor units (2-decimal currencies)

  const pi = await stripeClient().paymentIntents.create(
    {
      amount,
      currency: court.currency.toLowerCase(),
      automatic_payment_methods: { enabled: true },
      description: `Sportify – ${b.venueName} (${b.courtName}) ${b.date} ${b.startHour}:00`,
      metadata: { bookingId, userId: req.auth.uid },
    },
    { idempotencyKey: `booking_${bookingId}` }
  );
  await snap.ref.update({ paymentIntentId: pi.id, totalAmount: amount / 100 });
  return { clientSecret: pi.client_secret, publishableKey: STRIPE_PUBLISHABLE.value() };
});

// ---------- callable: verify payment with Stripe and confirm ----------
exports.confirmBookingPayment = onCall({ secrets: [STRIPE_SECRET] }, async (req) => {
  if (!req.auth) throw new HttpsError("unauthenticated", "Please sign in.");
  const snap = await db.collection("bookings").doc(req.data.bookingId).get();
  if (!snap.exists || snap.data().userId !== req.auth.uid) throw new HttpsError("permission-denied", "Not your booking");
  const piId = snap.data().paymentIntentId;
  if (!piId) throw new HttpsError("failed-precondition", "No payment started");
  const pi = await stripeClient().paymentIntents.retrieve(piId);
  if (pi.status !== "succeeded" || pi.metadata.bookingId !== snap.id)
    throw new HttpsError("failed-precondition", `Payment not completed (${pi.status})`);
  await markPaid(snap.id, pi.id);
  return { ok: true };
});

// ---------- Stripe webhook (authoritative; covers app being killed mid-payment) ----------
exports.stripeWebhook = onRequest({ secrets: [STRIPE_SECRET, STRIPE_WEBHOOK_SECRET] }, async (req, res) => {
  let event;
  try {
    event = stripeClient().webhooks.constructEvent(req.rawBody, req.headers["stripe-signature"], STRIPE_WEBHOOK_SECRET.value());
  } catch (e) {
    res.status(400).send(`Webhook error: ${e.message}`);
    return;
  }
  if (event.type === "payment_intent.succeeded") {
    const pi = event.data.object;
    if (pi.metadata.bookingId) await markPaid(pi.metadata.bookingId, pi.id);
  }
  res.json({ received: true });
});

// ---------- refund when a paid booking is cancelled (policy: >= 2h before start) ----------
exports.onBookingCancelled = onDocumentUpdated({ document: "bookings/{id}", secrets: [STRIPE_SECRET] }, async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  if (before.status === "CANCELLED" || after.status !== "CANCELLED" || after.paymentStatus !== "PAID") return;
  const hoursLeft = (after.startTime.toMillis() - Date.now()) / 3.6e6;
  if (hoursLeft < 2) {
    await notify(after.userId, "Booking cancelled", "Cancelled less than 2 hours before start – no refund applies.");
    return;
  }
  await stripeClient().refunds.create({ payment_intent: after.paymentIntentId });
  await event.data.after.ref.update({ paymentStatus: "REFUNDED" });
  await notify(after.userId, "Refund issued 💸", `Your booking at ${after.venueName} was cancelled and refunded.`);
});

// ---------- reminders: ~1 hour before start ----------
exports.sendBookingReminders = onSchedule("every 15 minutes", async () => {
  const from = admin.firestore.Timestamp.fromMillis(Date.now() + 45 * 60000);
  const to = admin.firestore.Timestamp.fromMillis(Date.now() + 75 * 60000);
  const snap = await db.collection("bookings").where("startTime", ">=", from).where("startTime", "<=", to).get();
  for (const doc of snap.docs) {
    const b = doc.data();
    if (b.status !== "CONFIRMED" || b.reminderSent) continue;
    await notify(b.userId, "Game on in 1 hour 🎾",
      `${b.venueName} · ${b.courtName} at ${String(b.startHour).padStart(2, "0")}:00`, { bookingId: doc.id });
    await doc.ref.update({ reminderSent: true });
  }
});
