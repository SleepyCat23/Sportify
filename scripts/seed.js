// Usage: cd scripts && npm i firebase-admin && GOOGLE_APPLICATION_CREDENTIALS=../serviceAccountKey.json node seed.js
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();
const img = (s) => `https://picsum.photos/seed/${s}/900/500`; // replace with your Firebase Storage URLs

const venues = [
  { id: "v1", name: "Ace Tennis Club", city: "Bandung", address: "Jl. Dago 12", sports: ["TENNIS", "PADEL"], rating: 4.7, openHour: 7, closeHour: 22, imageUrl: img("tennis"), description: "Hard & clay courts with floodlights, showers and a pro shop.",
    courts: [["Court 1 (Hard)", "TENNIS", 20, "USD"], ["Court 2 (Clay)", "TENNIS", 25, "USD"], ["Padel A", "PADEL", 30, "USD"]] },
  { id: "v2", name: "Slam Dunk Arena", city: "Jakarta", address: "Jl. Sudirman 88", sports: ["BASKETBALL", "VOLLEYBALL"], rating: 4.5, openHour: 8, closeHour: 23, imageUrl: img("basket"), description: "Indoor hardwood courts, air-conditioned.",
    courts: [["Main Court", "BASKETBALL", 40, "USD"], ["Half Court", "BASKETBALL", 22, "USD"]] },
  { id: "v3", name: "Green Goal Football Park", city: "Bandung", address: "Jl. Setiabudi 5", sports: ["FOOTBALL"], rating: 4.6, openHour: 9, closeHour: 24, imageUrl: img("football"), description: "5-a-side synthetic pitches under lights.",
    courts: [["Pitch 1", "FOOTBALL", 35, "USD"], ["Pitch 2", "FOOTBALL", 35, "USD"]] },
  {
    id: "v4",
    name: "Vivo Arena",
    city: "Bandung",
    address: "Jl. Melong Asih No. 69C, Cijerah, Kecamatan Bandung Kulon, Kota Bandung, Jawa Barat 40213",
    sports: ["FUTSAL", "BADMINTON", "PICKLEBALL"],
    rating: 4.8,
    openHour: 8,
    closeHour: 20,
    imageUrl: img("arena"),
    description: "Multi-sport facility with 3 badminton courts, 2 pickleball courts, and 1 futsal court. Amenities include car and motorcycle parking, toilets, Wi-Fi, snacks, drinks, and a prayer room.",
    courts: [
      ["Badminton 1", "BADMINTON", 50000, "IDR"],
      ["Badminton 2", "BADMINTON", 50000, "IDR"],
      ["Badminton 3", "BADMINTON", 50000, "IDR"],
      ["Pickleball 1", "PICKLEBALL", 60000, "IDR"],
      ["Pickleball 2", "PICKLEBALL", 60000, "IDR"],
      ["Futsal Court", "FUTSAL", 150000, "IDR"]
    ]
  },
];

(async () => {
  for (const { courts, id, ...v } of venues) {
    await db.collection("venues").doc(id).set(v);
    for (const [i, [name, sport, price, currency]] of courts.entries())
      await db.collection("courts").doc(`${id}_c${i + 1}`).set({ venueId: id, name, sport, pricePerHour: price, currency: currency || "USD" });
  }
  // Make an existing account an admin (run with ADMIN_UID=<uid from Firebase Auth> node seed.js)
  if (process.env.ADMIN_UID) {
    await db.collection("users").doc(process.env.ADMIN_UID).set({ role: "ADMIN" }, { merge: true });
    console.log("Admin role granted to", process.env.ADMIN_UID);
  }
  console.log("Seeded.");
})();
