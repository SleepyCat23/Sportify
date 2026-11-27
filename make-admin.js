const { initializeApp, cert } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore } = require('firebase-admin/firestore');

// Your service account details
const serviceAccount = {
  "type": "service_account",
  "project_id": "sportify-14bc0",
  "private_key_id": "9ee69a0462e78d3d39b27cfa91cc326747b97166",
  "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQChYHp2oqkUuRYA\nixUWpmOFTkA9XUp70KPsTJldbcwDjglF71rBFRg+oiWau0DhOVsPbl0GdrJx3b1B\no5QI69E82tLMQ/ggSh233o3WPcVFkn8gXoPAKBdsSJcRa27OCEjVm/ZWf9iFYx+t\nbccYD5yfE16BVhncL1efj7HzVgUfIgiXMf1D9O80RE4iI58oAUtDGoosNM1H/MnZ\na1sNBrJBV9G9M5Spr8BZtozEn3dtvBpaUmfm+PBawlVhHW0OMRRyYw4z5lgLySim\nnZDwj7bWPwOpY8YE7rP+jNZ1KDV+07eBWQDZzKmmiWbzhb78jruEP6mU9QWhGV51\nMSMv/e7DAgMBAAECggEADiAsa0Ju7tU9e5ixC0WCd2jtFWU07HeYq0ndMG3SKnF+\nME5bF4TQLxNXm7hOXcIwGiUWV+CywZk5d4ucNqR/StNgsxtnMCeTFSte9DGxMrDR\nfegObxbs54z6SqUO+VJSUphcGYahUYCAUliTBKRay+kQuanrifflTjgJ/JUIAVYZ\n6fggICe9e/Hw89s26b6SNsS/tvHlQSghBF/Dm8fH7FfBv1rt7synZ2fWpW1v3vca\nEUVI+DL/MvWfUzoAMjIfuMyotkANUFaSd4Egn3vqMauGDQ7lODplQykMUU10hdDp\nSIxy6ESL4sQRbmTlE5hCs+BmfC+ohKipONIqe3m3iQKBgQDQSmalHM2uVwIeY2li\nmv98wRZVCxuuYDl8QUuQAFKiVM1DFLMgsBSU4vRQh0YbYhS9rnHcfdhptlIk3Iqn\n8GParGQCwfuMSbCuyLh7/XlS0KJxxJ4p6zmjOx837cudQEfaIpIsX5U38kiO+xl7\n2OLGrk8EikuX6nHF5wfFyWDt9QKBgQDGVy5x6EXR+77m856PnnX8t09l783YlzbU\n1Re8Cj2RPCQcQnUGYJyiI5DHXKVVvohj6THsGhisAUSKsgO5w4cBnGNZdrhzZlCC\nBVyzLI8r1rtCEWQgNr/A9zKzbLrzQoHxx4Qsq6aL5PgDukV/SCtgWdA68vNC+8x4\nlO+x4mn+1wKBgQCPX1cuOhhd8+gycK1ewWAxSFuN6FqCehu3qt5PXdmDganUjdAL\nJQjVvy76q1KXkLjEI+qeIkWv5l6+nswWc0LYh6dDurDizOqANQu/1ScI2TGn6/YO\n1qpS8GQ5xEZOlKugvAEv6A83dl95g9/zvsPCjMM5VdRY2ni8B53zB/3fbQKBgGnE\nK9xKRJFJce+ukit8EOcGyaxOSLLrUC4GxHVd8J72NztqpQUVl/FOjfICEZi71feR\nbzG8aNOwiWB8tUfmYKQXRVN737iKIvxy55AUfvSduZXtuDsFdxPrvnm3zanTGYBZ\nKL65ekK3gxpZxp2tWpQy0LveuqJJrCvRIFD3sBZZAoGAaufGqPrqdh0DYBX3+Nnn\n0AYZLISv83nyK7jjmA6WbzW1fr1d9Yx6lbrir5ij0SRTvENGhjU4J8WRrN0CxP89\nMSKJvZlIcW0VqHwAtqjVu04nmmc2AMGRGUB4DeoB4VMzB/mC7HGUmLmPLSlj0qzo\nj6Lcy1h07ZoZ4cjHAQswjh8=\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-fbsvc@sportify-14bc0.iam.gserviceaccount.com",
};

// Initialize app using modular API to avoid the "undefined cert" error
initializeApp({
  credential: cert(serviceAccount)
});

const targetUid = 'SDVUYjfRJHbv0J9eN1n4mHyd2o03';

async function grantAdmin() {
  try {
    console.log(`Setting Admin privileges for UID: ${targetUid}...`);

    // 1. Set Custom User Claim (Backend permission)
    await getAuth().setCustomUserClaims(targetUid, { admin: true });
    console.log('✅ Custom Claims updated successfully.');

    // 2. Update Firestore document (App UI role)
    const db = getFirestore();
    await db.collection("users").doc(targetUid).set({
      role: "ADMIN"
    }, { merge: true });
    console.log('✅ Firestore "role" set to "ADMIN".');

    console.log('\nSuccess! Please restart the app on your Android device.');
    process.exit(0);
  } catch (error) {
    console.error('❌ Error updating admin status:', error);
    process.exit(1);
  }
}

grantAdmin();
