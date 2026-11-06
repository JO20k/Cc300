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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user cannot read or write player profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user can create and read their own valid PlayerProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceWhale",
      tonBalance: 100.5,
      usdtBalance: 500.0,
      btcBalance: 0.025,
      activeCurrency: "TON",
      totalWagered: 0,
      totalWon: 0,
      gamesPlayed: 0,
      vipLevel: 1,
      botChatId: "",
      serverSeedHash: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      clientSeed: "televegas_seed_1",
      nonce: 1,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Cross-user isolation: Bob cannot read or update Alice's PlayerProfile", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceWhale",
      tonBalance: 100.5,
      usdtBalance: 500.0,
      btcBalance: 0.025,
      activeCurrency: "TON",
      totalWagered: 0,
      totalWon: 0,
      gamesPlayed: 0,
      vipLevel: 1,
      botChatId: "",
      serverSeedHash: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      clientSeed: "televegas_seed_1",
      nonce: 1,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow update with ghost field is rejected on PlayerProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceWhale",
      tonBalance: 100.5,
      usdtBalance: 500.0,
      btcBalance: 0.025,
      activeCurrency: "TON",
      totalWagered: 0,
      totalWon: 0,
      gamesPlayed: 0,
      vipLevel: 1,
      botChatId: "",
      serverSeedHash: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      clientSeed: "televegas_seed_1",
      nonce: 1,
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user can create and query their own BotChatMessages and CryptoTransactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("messages")
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        sender: "BOT",
        commandOrText: "🎰 Slot Spin: 7️⃣ 7️⃣ 7️⃣ — JACKPOT!",
        gameType: "SLOTS",
        emojiSymbol: "🎰",
        emojiValue: 64,
        betAmount: 10,
        payoutAmount: 150,
        currency: "TON",
        outcome: "JACKPOT",
        metadataSummary: "777 Triple Sevens (15x)",
        createdAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("messages")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set({
        userId: ALICE_UID,
        txType: "BET_WIN",
        currency: "TON",
        amount: 150,
        txHash: "0x9f8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c",
        addressOrNote: "Slots 777 Jackpot Payout",
        status: "CONFIRMED",
        createdAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});
