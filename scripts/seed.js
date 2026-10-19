// Usage: cd scripts && npm i firebase-admin && GOOGLE_APPLICATION_CREDENTIALS=../serviceAccountKey.json node seed.js
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();
const img = (s) => `https://picsum.photos/seed/${s}/900/500`; // replace with your Firebase Storage URLs

const venues = [
  { id: "v1", name: "Ace Tennis Club", city: "Bandung", address: "Jl. Dago 12", sports: ["TENNIS", "PADEL"], rating: 4.7, openHour: 7, closeHour: 22, imageUrl: img("tennis"), description: "Hard & clay courts with floodlights, showers and a pro shop.",
    courts: [["Court 1 (Hard)", "TENNIS", 20], ["Court 2 (Clay)", "TENNIS", 25], ["Padel A", "PADEL", 30]] },
  { id: "v2", name: "Slam Dunk Arena", city: "Jakarta", address: "Jl. Sudirman 88", sports: ["BASKETBALL", "VOLLEYBALL"], rating: 4.5, openHour: 8, closeHour: 23, imageUrl: img("basket"), description: "Indoor hardwood courts, air-conditioned.",
    courts: [["Main Court", "BASKETBALL", 40], ["Half Court", "BASKETBALL", 22]] },
  { id: "v3", name: "Green Goal Football Park", city: "Bandung", address: "Jl. Setiabudi 5", sports: ["FOOTBALL"], rating: 4.6, openHour: 9, closeHour: 24, imageUrl: img("football"), description: "5-a-side synthetic pitches under lights.",
    courts: [["Pitch 1", "FOOTBALL", 35], ["Pitch 2", "FOOTBALL", 35]] },
];

(async () => {
  for (const { courts, id, ...v } of venues) {
    await db.collection("venues").doc(id).set(v);
    for (const [i, [name, sport, price]] of courts.entries())
      await db.collection("courts").doc(`${id}_c${i + 1}`).set({ venueId: id, name, sport, pricePerHour: price, currency: "USD" });
  }
  console.log("Seeded.");
})();
