# Sportify – sports venue booking (Kotlin · Compose · Firebase · Stripe)


## Admin & social features

- **Admin role**: set `role: "ADMIN"` on `users/{uid}` from the Firebase console, or run
  `ADMIN_UID=<uid> node scripts/seed.js`. Admins land on a separate Admin graph (Venues, Payments, Reviews).
  Clients cannot change their own `role` (see `firestore.rules`).
- **Admin**: add / delete venues, edit court prices and opening hours, refund any paid booking
  (callable `adminRefund`), moderate reviews.
- **Reviews**: one per user per venue; `onReviewWritten` keeps `venues.rating` and `reviewCount` in sync.
- **Open Play**: create games, request to join, host approves/declines, full games close to the public.
- **Matchmaking**: public `players/{uid}` profile (level + sport), invites accepted/declined by the receiver.
- **Scores & stats**: `matches` collection; wins / win rate are derived on the device.
- **Explore filters**: city, minimum rating, maximum "from" price.
- Deploy: `firebase deploy --only firestore:rules,functions`.
