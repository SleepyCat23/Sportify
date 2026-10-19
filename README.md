# Sportify – sports venue booking (Kotlin · Compose · Firebase · Stripe)

## Architecture
```
presentation/  Compose screens + @HiltViewModel (StateFlow UI state)
domain/        models, repository interfaces, use cases (no Firebase-specific logic in UI)
data/          Firebase implementations (Auth, Firestore, Storage, Functions, FCM)
di/            Hilt modules
functions/     Cloud Functions (Stripe, FCM, refunds, reminders)
```
**Booking integrity:** every hour is a Firestore doc `slots/{courtId_date_hour}`. A booking is created in a
*transaction* that fails if any slot exists, so double-booking is impossible. Slots are held 10 min
(`PENDING_PAYMENT`); an expired hold can be taken by someone else. Payment fields can only be written by Cloud Functions
(see `firestore.rules`).

**Payment flow:** app → `createPaymentIntent` (server recomputes price) → Stripe PaymentSheet (cards, Google Pay/Link) →
`confirmBookingPayment` (server verifies with Stripe) + `stripeWebhook` as the safety net → booking `CONFIRMED` + FCM push.
Cancel ≥ 2 h before start ⇒ automatic refund.

## Setup
1. **Firebase console**: create project → add Android app `com.sportify.app` (add your debug **SHA-1**) → download
   `google-services.json` into `app/`.
2. Enable **Authentication** (Email/Password + Google), **Firestore**, **Storage**, **Cloud Messaging**. Cloud Functions needs the Blaze plan.
3. Deploy backend:
   ```bash
   npm i -g firebase-tools && firebase login && firebase use <project-id>
   cd functions && npm install && cd ..
   firebase functions:secrets:set STRIPE_SECRET_KEY       # sk_test_...
   firebase functions:secrets:set STRIPE_WEBHOOK_SECRET   # whsec_... (after step 4)
   echo "STRIPE_PUBLISHABLE_KEY=pk_test_..." > functions/.env
   firebase deploy --only firestore:rules,storage,functions
   ```
4. **Stripe** (test mode): Developers → Webhooks → add the deployed `stripeWebhook` URL, event `payment_intent.succeeded`.
5. **Seed demo data**: `cd scripts && npm i firebase-admin && GOOGLE_APPLICATION_CREDENTIALS=../serviceAccountKey.json node seed.js`
6. Open the folder in **Android Studio** (it generates the Gradle wrapper on first sync), run on a device/emulator with Play Services.
7. Pay with Stripe test card `4242 4242 4242 4242`, any future expiry/CVC.

## Notes / next steps
- Currencies are assumed to have 2 decimals (adjust `amount` in `createPaymentIntent` for JPY etc.).
- Venue/court management is via console or admin script; add an admin app/role via custom claims when needed.
- Slot times use the device time zone; store a venue time zone for multi-region use.
- Add `FLAG_SECURE`-style hardening, App Check, and ProGuard rules before release.
