const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-pulsegoa-d8d79e0d-25c3-44d8-a911-42e41716ac76";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

let testEnv;

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

function validGoalPayload(uid) {
  const nowMs = Date.now();
  const safeTs = new Date(nowMs - 1000);
  return {
    userId: uid,
    title: "Daily Steps",
    description: "Walk 8,000 steps",
    category: "STEPS",
    targetValue: 8000,
    currentProgress: 0,
    unit: "steps",
    quickIncrement: 1000,
    startDate: nowMs,
    endDate: nowMs + 30 * 86400000,
    isArchived: false,
    createdAt: safeTs,
    updatedAt: safeTs,
  };
}

test("Unauthenticated user cannot read or write fitness_goals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/fitness_goals`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/fitness_goals`).doc("g1").set(validGoalPayload(ALICE_UID))
  );
});

test("Authenticated user can create, read, update, and delete own goal", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const goalRef = aliceDb.collection(`users/${ALICE_UID}/fitness_goals`).doc("goal_1");

  await assertSucceeds(goalRef.set(validGoalPayload(ALICE_UID)));
  await assertSucceeds(goalRef.get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/fitness_goals`).get());
  await assertSucceeds(
    goalRef.update({
      currentProgress: 2000,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(goalRef.delete());
});

test("Cross-user access to fitness_goals is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/fitness_goals`).doc("goal_1").set(validGoalPayload(ALICE_UID))
  );

  await assertFails(bobDb.collection(`users/${ALICE_UID}/fitness_goals`).doc("goal_1").get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/fitness_goals`).get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/fitness_goals`).doc("goal_1").update({
      currentProgress: 999,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/fitness_goals`).doc("goal_1").delete());
});

test("Shadow fields and spoofed userId are rejected on fitness_goals", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const payloadWithGhost = { ...validGoalPayload(ALICE_UID), isAdmin: true };
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/fitness_goals`).doc("g_ghost").set(payloadWithGhost)
  );

  const spoofedPayload = validGoalPayload(BOB_UID);
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/fitness_goals`).doc("g_spoof").set(spoofedPayload)
  );
});

test("GoalLog requires valid parent goal and rejects updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const goalRef = aliceDb.collection(`users/${ALICE_UID}/fitness_goals`).doc("goal_1");
  await assertSucceeds(goalRef.set(validGoalPayload(ALICE_UID)));

  // Orphan log without parent goal fails
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/goal_logs`).doc("log_orphan").set({
      userId: ALICE_UID,
      goalId: "missing_goal",
      amount: 1000,
      dateKey: "2026-11-16",
      note: "Morning walk",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Valid log referencing existing parent goal succeeds
  const logRef = aliceDb.collection(`users/${ALICE_UID}/goal_logs`).doc("log_1");
  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      goalId: "goal_1",
      amount: 1000,
      dateKey: "2026-11-16",
      note: "Morning walk",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable log update is rejected
  await assertFails(logRef.update({ amount: 5000 }));
  await assertSucceeds(logRef.delete());
});
