const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "mohammadtanvir01suhrab@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write user profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set({
    uid: ALICE_UID,
    displayName: "Alice",
    email: "alice@test.com",
    coins: 100,
    currentLevel: 1,
    highestUnlockedLevel: 1,
    totalScore: 0,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user: can create own profile with 100 coins", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    uid: ALICE_UID,
    displayName: "Alice",
    email: "alice@test.com",
    coins: 100,
    currentLevel: 1,
    highestUnlockedLevel: 1,
    totalScore: 0,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user: cannot create profile with >100 coins", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set({
    uid: ALICE_UID,
    displayName: "Alice",
    email: "alice@test.com",
    coins: 9999,
    currentLevel: 1,
    highestUnlockedLevel: 1,
    totalScore: 0,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Player cannot grant themselves coins on update", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      email: "alice@test.com",
      coins: 100,
      currentLevel: 1,
      highestUnlockedLevel: 1,
      totalScore: 0,
      createdAt: now,
      updatedAt: now
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Player attempting to increase coins
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    coins: 500,
    updatedAt: new Date()
  }));

  // Player updating level and score keeping coins intact succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).update({
    currentLevel: 2,
    highestUnlockedLevel: 2,
    totalScore: 1200,
    updatedAt: new Date()
  }));
});

test("Player cannot edit another player's profile", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob",
      email: "bob@test.com",
      coins: 100,
      currentLevel: 1,
      highestUnlockedLevel: 1,
      totalScore: 0,
      createdAt: now,
      updatedAt: now
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({
    displayName: "Hacked",
    updatedAt: new Date()
  }));
});

test("Admin: can grant coins to any player and record transaction", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob",
      email: "bob@test.com",
      coins: 100,
      currentLevel: 1,
      highestUnlockedLevel: 1,
      totalScore: 0,
      createdAt: now,
      updatedAt: now
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  
  // Admin updating Bob's coins
  await assertSucceeds(adminDb.collection("users").doc(BOB_UID).update({
    coins: 350,
    updatedAt: new Date()
  }));

  // Admin recording transaction
  await assertSucceeds(adminDb.collection("coin_transactions").doc("tx_1").set({
    id: "tx_1",
    adminUid: ADMIN_UID,
    adminEmail: ADMIN_EMAIL,
    targetUid: BOB_UID,
    amount: 250,
    note: "Bonus reward",
    timestamp: new Date()
  }));
});

test("Regular player cannot create coin transaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("coin_transactions").doc("tx_fraud").set({
    id: "tx_fraud",
    adminUid: ALICE_UID,
    adminEmail: "alice@test.com",
    targetUid: ALICE_UID,
    amount: 1000,
    timestamp: new Date()
  }));
});
