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

test("Unauthenticated user cannot read or create tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user can create and read their own preferences and tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      blurIntensity: 68,
      themeMode: "DARK_GLASS",
      iconPack: "LIQUID_PRISM",
      parallaxEnabled: true,
      city: "San Francisco",
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Review Liquid Glass UI System",
      notes: "Tune 90Hz parallax spring damping and frosted border alpha",
      category: "FOCUS",
      priority: "HIGH",
      completed: false,
      sortOrder: 0,
      reminderLabel: "10:00 AM • Smart Focus",
      reminderEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's tasks or preferences", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      blurIntensity: 70,
      themeMode: "DARK_GLASS",
      iconPack: "LIQUID_PRISM",
      parallaxEnabled: true,
      city: "Tokyo",
      updatedAt: now,
    });
    await context.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Alice Private Task",
      notes: "Private",
      category: "WORK",
      priority: "HIGH",
      completed: false,
      sortOrder: 0,
      reminderLabel: "",
      reminderEnabled: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Shadow update and invalid schema values are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      blurIntensity: 50,
      themeMode: "DARK_GLASS",
      iconPack: "LIQUID_PRISM",
      parallaxEnabled: true,
      city: "London",
      updatedAt: now,
      isAdminGhost: true,
    })
  );

  // Reject out-of-range blurIntensity
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      blurIntensity: 250,
      themeMode: "DARK_GLASS",
      iconPack: "LIQUID_PRISM",
      parallaxEnabled: true,
      city: "London",
      updatedAt: now,
    })
  );
});
